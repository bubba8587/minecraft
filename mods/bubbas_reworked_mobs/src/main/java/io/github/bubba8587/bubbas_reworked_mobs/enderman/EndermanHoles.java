// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Ground missing round cairns, compared against natural generation (the target world), so endermen can arrive already
 * carrying it (owner, 2026-11-16: "more endermen to spawn naturally already having missing-from-natural-generation
 * blocks, predetermined by what's missing ... not just for cairns but for more of the surrounding area"; and "is the
 * system not just comparing against natural generation?": it is).
 *
 * <p>Each active site (a player within SITE_ACTIVE_RANGE) sweeps the chunks round it, nearest first, one chunk a
 * second, every column checked against the target world: air where natural ground was, with its floor in place, is a
 * hole. So a hole is found however it was made (digging, explosions, anything), and the list rebuilds itself after a
 * reload. A block a player breaks is also noted at once, as a head start. Cost: per active site, one chunk's 256
 * columns a second (one cached target-world lookup and a few block reads each; the target chunk is built once and
 * kept in TerrainOracle's cache).
 */
public final class EndermanHoles {
	private static final Map<ServerLevel, Set<BlockPos>> HOLES = new WeakHashMap<>();
	/** Each site's sweep position (index into CHUNK_ORDER), by site origin. */
	private static final Map<ServerLevel, Map<BlockPos, Integer>> SWEEPS = new WeakHashMap<>();
	/** Seconds a site's sweep has waited on its current chunk's target world. */
	private static final Map<ServerLevel, Map<BlockPos, Integer>> WAITS = new WeakHashMap<>();
	private static final int SWEEP_WAIT_LIMIT = 30;
	/** Chunk offsets round a site's chunk within HOLE_RANGE, nearest first. */
	private static final List<int[]> CHUNK_ORDER = chunkOrder();

	private EndermanHoles() {
	}

	private static List<int[]> chunkOrder() {
		int r = Math.floorDiv(EndermanTuning.HOLE_RANGE, 16) + 1;
		List<int[]> order = new ArrayList<>();
		int reach = EndermanTuning.HOLE_RANGE + 16;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx * 256 + dz * dz * 256 <= reach * reach) {
					order.add(new int[] {dx, dz}); // only chunks that reach into the circle (keeps within the target-world cache)
				}
			}
		}
		order.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
		return List.copyOf(order);
	}

	/** Every second: each site with a player near checks its next chunk of surroundings. */
	static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD || level.getServer() instanceof GameTestServer || level.getGameTime() % 20 != 7) {
			return;
		}
		Set<CairnSite> active = new LinkedHashSet<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				active.addAll(CairnSites.near(level, player.blockPosition(), EndermanTuning.SITE_ACTIVE_RANGE));
			}
		}
		Map<BlockPos, Integer> sweeps = SWEEPS.computeIfAbsent(level, l -> new HashMap<>());
		for (CairnSite site : active) {
			int index = sweeps.getOrDefault(site.origin(), 0);
			// The target world is worked out off the main thread: a chunk not ready yet is tried again next second, up to
			// SWEEP_WAIT_LIMIT times (a world type with no target world never gets one).
			int waited = WAITS.computeIfAbsent(level, l -> new HashMap<>()).getOrDefault(site.origin(), 0);
			if (sweepChunk(level, site, CHUNK_ORDER.get(index)) || waited >= SWEEP_WAIT_LIMIT) {
				sweeps.put(site.origin(), (index + 1) % CHUNK_ORDER.size());
				WAITS.get(level).put(site.origin(), 0);
			} else {
				WAITS.get(level).put(site.origin(), waited + 1);
			}
		}
	}

	/** Checks one chunk; false when its natural terrain isn't worked out yet (try again). */
	private static boolean sweepChunk(ServerLevel level, CairnSite site, int[] offset) {
		int cx = (site.origin().getX() >> 4) + offset[0];
		int cz = (site.origin().getZ() >> 4) + offset[1];
		if (!level.hasChunk(cx, cz)) {
			return true; // not loaded: skip it this round
		}
		if (TargetWorld.surface(level, (cx << 4) + 8, (cz << 4) + 8) == Integer.MIN_VALUE) {
			return false; // being worked out
		}
		double range = (double) EndermanTuning.HOLE_RANGE * EndermanTuning.HOLE_RANGE;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = cx << 4; x < (cx << 4) + 16; x++) {
			for (int z = cz << 4; z < (cz << 4) + 16; z++) {
				double dx = x - site.origin().getX();
				double dz = z - site.origin().getZ();
				if (dx * dx + dz * dz > range) {
					continue;
				}
				holesInColumn(level, x, z, pos);
			}
		}
		return true;
	}

	/**
	 * Every block missing from this column compared with natural generation (air where natural ground was, down to the
	 * first real block): all of them are noted, and the "floor in place" rule when claiming fills them bottom up.
	 */
	private static void holesInColumn(ServerLevel level, int x, int z, BlockPos.MutableBlockPos pos) {
		int surface = TargetWorld.surface(level, x, z);
		if (surface == Integer.MIN_VALUE) {
			return;
		}
		int bottom = Integer.MIN_VALUE;
		for (int y = surface; y > surface - EndermanTuning.FILL_DEPTH; y--) {
			pos.set(x, y, z);
			if (!level.getBlockState(pos).isAir()) {
				if (y == surface) {
					return; // untouched (one block read)
				}
				bottom = y + 1;
				break;
			}
		}
		if (bottom == Integer.MIN_VALUE) {
			return; // deeper than FILL_DEPTH (a mine, a deep pit): left alone
		}
		for (int y = bottom; y <= surface; y++) {
			pos.set(x, y, z);
			BlockState target = TargetWorld.target(level, pos);
			if (target != null && !target.isAir() && target.getFluidState().isEmpty() && TerrainKinds.family(target) != null
					&& CairnSites.covering(level, pos) == null) {
				add(level, pos.immutable());
			}
		}
	}

	/** A player broke the block at {@code pos}: note it at once if it was natural ground near a cairn (the sweep would find it later). */
	public static void broken(ServerLevel level, BlockPos pos) {
		if (level.dimension() != Level.OVERWORLD || CairnSites.near(level, pos, EndermanTuning.HOLE_RANGE).isEmpty()) {
			return;
		}
		BlockState target = TargetWorld.target(level, pos);
		if (target == null || target.isAir() || TerrainKinds.family(target) == null || CairnSites.covering(level, pos) != null) {
			return;
		}
		add(level, pos.immutable());
	}

	private static void add(ServerLevel level, BlockPos pos) {
		Set<BlockPos> holes = HOLES.computeIfAbsent(level, l -> new LinkedHashSet<>());
		if (holes.contains(pos)) {
			return;
		}
		if (holes.size() >= EndermanTuning.HOLE_LIST_CAP) {
			Iterator<BlockPos> oldest = holes.iterator();
			oldest.next();
			oldest.remove();
		}
		holes.add(pos);
	}

	/** Whether a hole is waiting near {@code site} (for its spawning). Drops ones that were filled meanwhile. */
	public static boolean waiting(ServerLevel level, CairnSite site) {
		return nearest(level, site, false) != null;
	}

	/** Takes the nearest waiting hole near {@code site} that can be filled now (its floor is there), or null. */
	public static @Nullable BlockPos claim(ServerLevel level, CairnSite site) {
		return nearest(level, site, true);
	}

	/** Takes the nearest known hole within {@code radius} of {@code around} that {@code carried} fills now, or null. */
	public static @Nullable BlockPos claimFor(ServerLevel level, BlockPos around, int radius, BlockState carried) {
		Set<BlockPos> holes = HOLES.get(level);
		if (holes == null || holes.isEmpty()) {
			return null;
		}
		BlockPos best = null;
		double bestDistance = (double) radius * radius;
		for (BlockPos pos : holes) {
			double distance = pos.distSqr(around);
			if (distance <= bestDistance && level.isLoaded(pos) && EndermanWork.needsFill(level, pos, carried)) {
				best = pos;
				bestDistance = distance;
			}
		}
		if (best != null) {
			holes.remove(best);
		}
		return best;
	}

	private static @Nullable BlockPos nearest(ServerLevel level, CairnSite site, boolean claim) {
		Set<BlockPos> holes = HOLES.get(level);
		if (holes == null || holes.isEmpty()) {
			return null;
		}
		double range = (double) EndermanTuning.HOLE_RANGE * EndermanTuning.HOLE_RANGE;
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Iterator<BlockPos> it = holes.iterator(); it.hasNext(); ) {
			BlockPos pos = it.next();
			if (!level.isLoaded(pos)) {
				continue;
			}
			if (!level.getBlockState(pos).isAir()) {
				it.remove(); // filled (by an enderman or a player)
				continue;
			}
			double distance = pos.distSqr(site.origin());
			BlockState target = TargetWorld.target(level, pos);
			// Fillable now: a hole two deep fills from the bottom up, so the upper one waits for its floor.
			if (distance <= range && distance < bestDistance && target != null && EndermanWork.needsFill(level, pos, target)) {
				best = pos;
				bestDistance = distance;
			}
		}
		if (claim && best != null) {
			holes.remove(best);
		}
		return best;
	}
}

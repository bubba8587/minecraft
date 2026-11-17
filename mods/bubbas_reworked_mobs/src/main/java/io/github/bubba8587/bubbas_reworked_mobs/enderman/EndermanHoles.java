// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Ground missing near cairns, so endermen can arrive already carrying it (owner, 2026-11-16: "more endermen to spawn
 * naturally already having missing-from-natural-generation blocks, predetermined by what's missing, not just for
 * cairns but for more of the surrounding area"). Every natural-ground block broken within HOLE_RANGE of a cairn site is
 * remembered (not saved: after a reload, endermen still find holes by their own random look round). A site's spawn
 * hands a newcomer the nearest waiting hole whose floor is in place and the block that belongs there.
 * Cost: one site lookup per broken block; the list is capped.
 */
public final class EndermanHoles {
	private static final Map<ServerLevel, Set<BlockPos>> HOLES = new WeakHashMap<>();

	private EndermanHoles() {
	}

	/** A block was removed at {@code pos} (a player broke it): remember it if it was natural ground near a cairn. */
	public static void broken(ServerLevel level, BlockPos pos) {
		if (level.dimension() != Level.OVERWORLD || CairnSites.near(level, pos, EndermanTuning.HOLE_RANGE).isEmpty()) {
			return;
		}
		BlockState target = TargetWorld.target(level, pos);
		if (target == null || target.isAir() || TerrainKinds.family(target) == null || CairnSites.covering(level, pos) != null) {
			return;
		}
		Set<BlockPos> holes = HOLES.computeIfAbsent(level, l -> new LinkedHashSet<>());
		if (holes.size() >= EndermanTuning.HOLE_LIST_CAP) {
			Iterator<BlockPos> oldest = holes.iterator();
			oldest.next();
			oldest.remove();
		}
		holes.add(pos.immutable());
	}

	/** Whether a hole is waiting near {@code site} (for its spawning). Drops ones that were filled meanwhile. */
	public static boolean waiting(ServerLevel level, CairnSite site) {
		return nearest(level, site, false) != null;
	}

	/** Takes the nearest waiting hole near {@code site} that can be filled now (its floor is there), or null. */
	public static @Nullable BlockPos claim(ServerLevel level, CairnSite site) {
		return nearest(level, site, true);
	}

	/** Takes the nearest remembered hole within {@code radius} of {@code around} that {@code carried} fills now, or null. */
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

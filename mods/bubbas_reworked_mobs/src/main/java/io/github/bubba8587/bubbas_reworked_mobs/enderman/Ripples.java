// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The ripple of a used Totem of the Beginning (Bubba's Totems & Artifacts, through api/Endermen): the ground round a
 * point goes back, ring by ring outward, to how it generated (the endermen's target world, {@link TargetWorld}).
 *
 * <p>It works on columns, nearest first, each in one go: blocks of a terrain family standing where the generated
 * ground had air (piles, pillars, a creeper's spoil) are taken away top down, then from the bottom up air where
 * generated ground was is filled with that ground (holes, craters, strip mines), and the top layer gets its own
 * material back (sand where grass was). The same rules as the endermen's (specs/endermen.md, E1), minus their small
 * chance of taking unnatural blocks, plus the depth of a whole cylinder.
 *
 * <p>It never harms a build: it only ever changes air and terrain-family blocks (dirt, stone, sand...), never a block
 * entity, an unnatural block (planks, glass, torches...) or anything else natural (logs, ores, flowers...); a whole
 * column is skipped when it, or any column within {@link EndermanTuning#RIPPLE_PROTECT_RADIUS}, holds a
 * {@link #protective protective} block anywhere in the cylinder's height, so a house in a dug-out pit (its roof,
 * floor, door) keeps the air in and round it; no block is put where an entity stands; water and lava are never put
 * back or filled over. Structures and wards are skipped (the target world has no opinion there).
 *
 * <p>Cost: one pass over the cylinder (each block read once or twice), spread over the ripple's time; a column whose
 * generated terrain is still being worked out waits (up to {@link EndermanTuning#RIPPLE_PENDING_TICKS}).
 */
public final class Ripples {
	/** Family blocks that terrain generation never makes on its own: built with, or trodden into paths. */
	private static final Set<Block> PLACED_LOOKING = Set.of(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLED_DEEPSLATE,
			Blocks.SMOOTH_STONE, Blocks.DIRT_PATH);
	/** Natural features that hollow out stone the target world can't reproduce: a geode's shell keeps its middle. */
	private static final Set<Block> HOLLOW_FEATURES = Set.of(Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.SMOOTH_BASALT,
			Blocks.AMETHYST_CLUSTER, Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD);

	private static final Map<ServerLevel, List<Ripple>> ACTIVE = new WeakHashMap<>();

	private Ripples() {
	}

	/** Whether a ripple can run here at all: the overworld, with a generator the target world can reproduce. */
	public static boolean supported(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && TerrainOracle.supports(level);
	}

	/**
	 * Starts a ripple at {@code centre}: a cylinder of {@code radius} (also up and down), one block further out every
	 * {@code ticksPerBlock} ticks. False when the level can't have one ({@link #supported}).
	 */
	public static boolean start(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock) {
		if (!supported(level)) {
			return false;
		}
		Ripple ripple = new Ripple(level, centre.immutable(), radius, Math.max(1, ticksPerBlock), null);
		ACTIVE.computeIfAbsent(level, l -> new ArrayList<>()).add(ripple);
		return true;
	}

	/**
	 * What a ripple at {@code centre} would change, changing nothing (tests: on an untouched world it should be close to
	 * nothing). Blocks until the generated terrain in reach is worked out. Returns "would change" positions with what
	 * each would become.
	 */
	public static Map<BlockPos, BlockState> dryRun(ServerLevel level, BlockPos centre, int radius) {
		for (int cx = (centre.getX() - radius) >> 4; cx <= (centre.getX() + radius) >> 4; cx++) {
			for (int cz = (centre.getZ() - radius) >> 4; cz <= (centre.getZ() + radius) >> 4; cz++) {
				if (level.hasChunk(cx, cz)) {
					TerrainOracle.getNow(level, cx, cz);
				}
			}
		}
		Map<BlockPos, BlockState> changes = new java.util.LinkedHashMap<>();
		Ripple ripple = new Ripple(level, centre.immutable(), radius, 1, changes);
		for (int[] o : ripple.columns) {
			ripple.column(level, o[0], o[1]);
		}
		return changes;
	}

	/** Whether any ripple is still running in this level (tests). */
	public static boolean running(ServerLevel level) {
		List<Ripple> list = ACTIVE.get(level);
		return list != null && !list.isEmpty();
	}

	static void tick(ServerLevel level) {
		List<Ripple> list = ACTIVE.get(level);
		if (list == null || list.isEmpty()) {
			return;
		}
		list.removeIf(ripple -> ripple.tick(level));
	}

	/** Blocks that keep a ripple off their column and the columns round it: anything built, placed or hollowing. */
	public static boolean protective(BlockState state) {
		if (state.isAir()) {
			return false;
		}
		if (state.hasBlockEntity() || PLACED_LOOKING.contains(state.getBlock()) || HOLLOW_FEATURES.contains(state.getBlock())
				|| state.getFluidState().is(FluidTags.LAVA)) {
			return true;
		}
		return !TerrainKinds.isNatural(state);
	}

	private static final class Ripple {
		private final BlockPos centre;
		private final int ticksPerBlock;
		private final int lo;
		private final int hi;
		private final long started;
		/** Column offsets inside the radius, nearest first. */
		private final List<int[]> columns = new ArrayList<>();
		private int next;
		/** Columns whose generated terrain wasn't ready: (dx, dz, first try). */
		private final ArrayDeque<long[]> pending = new ArrayDeque<>();
		/** Per column (packed x/z): holds a protective block somewhere between lo and hi. */
		private final Long2BooleanOpenHashMap protectiveColumn = new Long2BooleanOpenHashMap();
		private int particlesThisTick;
		/** A dry run: changes are recorded here instead of made. */
		private final @org.jspecify.annotations.Nullable Map<BlockPos, BlockState> dry;

		Ripple(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock, @org.jspecify.annotations.Nullable Map<BlockPos, BlockState> dry) {
			this.centre = centre;
			this.dry = dry;
			this.ticksPerBlock = ticksPerBlock;
			this.lo = Math.max(level.getMinY(), centre.getY() - radius);
			this.hi = Math.min(level.getMaxY(), centre.getY() + radius);
			this.started = level.getGameTime();
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dz * dz <= radius * radius) {
						columns.add(new int[] { dx, dz });
					}
				}
			}
			columns.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
			// Ask for the generated terrain of every chunk in reach now, so it's being worked out before the ring gets there.
			for (int cx = (centre.getX() - radius) >> 4; cx <= (centre.getX() + radius) >> 4; cx++) {
				for (int cz = (centre.getZ() - radius) >> 4; cz <= (centre.getZ() + radius) >> 4; cz++) {
					TargetWorld.surface(level, cx << 4, cz << 4);
				}
			}
		}

		/** One tick; true when done. */
		boolean tick(ServerLevel level) {
			long now = level.getGameTime();
			particlesThisTick = 0;
			double reach = (now - started + 1) / (double) ticksPerBlock;
			double reachSqr = reach * reach;
			while (next < columns.size()) {
				int[] o = columns.get(next);
				if (o[0] * o[0] + o[1] * o[1] > reachSqr) {
					break;
				}
				next++;
				if (!column(level, o[0], o[1])) {
					pending.add(new long[] { o[0], o[1], now });
				}
			}
			for (int i = pending.size(); i > 0; i--) {
				long[] p = pending.poll();
				if (!column(level, (int) p[0], (int) p[1]) && now - p[2] < EndermanTuning.RIPPLE_PENDING_TICKS) {
					pending.add(p);
				}
			}
			return next >= columns.size() && pending.isEmpty();
		}

		/** Restores one column; false when its generated terrain isn't worked out yet (try again later). */
		private boolean column(ServerLevel level, int dx, int dz) {
			int x = centre.getX() + dx;
			int z = centre.getZ() + dz;
			if (!level.hasChunk(x >> 4, z >> 4)) {
				return true; // not loaded: nothing to restore there now
			}
			int surface = TargetWorld.surface(level, x, z);
			if (surface == Integer.MIN_VALUE) {
				return false;
			}
			int r = EndermanTuning.RIPPLE_PROTECT_RADIUS;
			for (int ox = -r; ox <= r; ox++) {
				for (int oz = -r; oz <= r; oz++) {
					if (protectiveColumn(level, x + ox, z + oz)) {
						return true;
					}
				}
			}
			List<Entity> standing = level.getEntities((Entity) null, new AABB(x, lo, z, x + 1, hi + 1, z + 1),
					EntitySelector.NO_SPECTATORS.and(e -> !(e instanceof ItemEntity) && !(e instanceof ExperienceOrb)));
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			// Ground where there was none, top down (so nothing above falls into the gap first).
			for (int y = hi; y > Math.max(lo - 1, surface); y--) {
				pos.set(x, y, z);
				BlockState real = level.getBlockState(pos);
				if (real.isAir() || TerrainKinds.family(real) == null) {
					continue;
				}
				BlockState target = TargetWorld.target(level, pos);
				if (target != null && target.isAir()) {
					set(level, pos, target);
				}
			}
			// Missing ground and wrong top layers, bottom up (each filled block is the floor of the next).
			for (int y = lo; y <= Math.min(hi, surface); y++) {
				pos.set(x, y, z);
				BlockState target = TargetWorld.target(level, pos);
				if (target == null || target.isAir() || !target.getFluidState().isEmpty() || TerrainKinds.family(target) == null) {
					continue;
				}
				BlockState real = level.getBlockState(pos);
				if (real.isAir()) {
					if (blocked(standing, pos) || target.getBlock() instanceof FallingBlock && !holdsUp(level, pos.below())) {
						continue;
					}
					set(level, pos, target);
				} else if (TerrainKinds.family(real) != null && TerrainKinds.family(real) != TerrainKinds.family(target)
						&& y >= surface - 1 && !nearWater(level, pos)) {
					set(level, pos, target);
				}
			}
			return true;
		}

		private boolean protectiveColumn(ServerLevel level, int x, int z) {
			long key = BlockPos.asLong(x, 0, z);
			if (protectiveColumn.containsKey(key)) {
				return protectiveColumn.get(key);
			}
			boolean found = false;
			if (level.hasChunk(x >> 4, z >> 4)) {
				BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
				for (int y = lo; y <= hi && !found; y++) {
					found = protective(level.getBlockState(pos.set(x, y, z)));
				}
			}
			protectiveColumn.put(key, found);
			return found;
		}

		private void set(ServerLevel level, BlockPos pos, BlockState state) {
			if (dry != null) {
				dry.put(pos.immutable(), state);
				return;
			}
			BlockState old = level.getBlockState(pos);
			if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
				return;
			}
			if (particlesThisTick < EndermanTuning.RIPPLE_PARTICLES_PER_TICK && level.getBlockState(pos.above()).isAir()) {
				particlesThisTick++;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.02);
				if (particlesThisTick % 12 == 1) {
					BlockState sounding = state.isAir() ? old : state;
					level.playSound(null, pos, sounding.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.6F, 0.8F);
				}
			}
		}
	}

	private static boolean blocked(List<Entity> standing, BlockPos pos) {
		if (standing.isEmpty()) {
			return false;
		}
		AABB cell = new AABB(pos);
		for (Entity entity : standing) {
			if (entity.isAlive() && entity.getBoundingBox().intersects(cell)) {
				return true;
			}
		}
		return false;
	}

	private static boolean holdsUp(ServerLevel level, BlockPos below) {
		BlockState state = level.getBlockState(below);
		return !state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.REPLACEABLE);
	}

	private static boolean nearWater(ServerLevel level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (!level.getFluidState(pos.relative(d)).isEmpty()) {
				return true;
			}
		}
		return false;
	}
}

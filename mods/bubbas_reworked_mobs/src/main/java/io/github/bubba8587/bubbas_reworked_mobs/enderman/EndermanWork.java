// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The comparisons behind the jobs (specs/endermen.md, E1): which real block doesn't belong, and where a carried block
 * should go. Each search samples a few random columns and compares one real block with one target block per step,
 * so its cost is bounded: every column within NEAR_RADIUS, nearest first, then a few random columns farther out;
 * each column is a handful of block reads (about one for untouched ground).
 */
public final class EndermanWork {
	/** What to do at a spot. */
	public enum Kind {
		/** Take the block there. */
		TAKE,
		/** Jump on the farmland there until it turns back to dirt. */
		TRAMPLE
	}

	public record Found(BlockPos pos, Kind kind) {
	}

	/** Column offsets checked first, nearest first (all of them, so nearby work is always found). */
	private static final List<int[]> NEAR = new java.util.ArrayList<>();

	static {
		int r = EndermanTuning.NEAR_RADIUS;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				NEAR.add(new int[] { dx, dz });
			}
		}
		NEAR.sort(java.util.Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
	}

	private EndermanWork() {
	}

	/** Missing blocks of the unfinished cairns within reach: worth fetching from anywhere (offerings, End blocks). */
	public static Set<Block> wanted(ServerLevel level, BlockPos around) {
		Set<Block> wanted = new HashSet<>();
		for (CairnSite site : CairnSites.near(level, around, EndermanTuning.SITE_WORK_RADIUS)) {
			for (BlockPos pos : site.order()) {
				BlockState want = site.at(pos);
				if (level.isLoaded(pos) && !TerrainKinds.matches(level.getBlockState(pos), want)) {
					wanted.add(want.getBlock());
				}
			}
		}
		return wanted;
	}

	/** A block near {@code around} that doesn't belong (to take) or farmland that should be dirt, or null. */
	public static @Nullable Found findWork(ServerLevel level, BlockPos around, RandomSource random, Set<Block> wanted) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int near = NEAR.size();
		for (int i = 0; i < near + EndermanTuning.SCAN_COLUMNS; i++) {
			int x = around.getX() + (i < near ? NEAR.get(i)[0] : random.nextInt(2 * EndermanTuning.SCAN_RADIUS + 1) - EndermanTuning.SCAN_RADIUS);
			int z = around.getZ() + (i < near ? NEAR.get(i)[1] : random.nextInt(2 * EndermanTuning.SCAN_RADIUS + 1) - EndermanTuning.SCAN_RADIUS);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			// From the real top (under leaves, at most SCAN_HEIGHT above the original ground) down to just below the
			// original ground: piles, pillars and walls on it, and its top layer.
			int surface = TargetWorld.surface(level, x, z);
			if (surface == Integer.MIN_VALUE) {
				continue;
			}
			int top = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, surface + EndermanTuning.SCAN_HEIGHT);
			for (int y = top; y >= surface - 1; y--) {
				pos.set(x, y, z);
				Kind kind = judge(level, pos, random, wanted);
				if (kind != null) {
					return new Found(pos.immutable(), kind);
				}
			}
		}
		return null;
	}

	/** What an enderman would do about the block at {@code pos}, or null to leave it. */
	public static @Nullable Kind judge(ServerLevel level, BlockPos pos, RandomSource random, Set<Block> wanted) {
		BlockState real = level.getBlockState(pos);
		if (real.isAir()) {
			return null;
		}
		BlockState target = TargetWorld.target(level, pos);
		if (target == null || TerrainKinds.matches(real, target)) {
			return null;
		}
		CairnSite site = CairnSites.covering(level, pos);
		if (site != null && site.at(pos) != null) {
			// In the way of the cairn's design (grass where its footing goes, a block a player put in it): clear it.
			return TerrainKinds.liftable(level, pos, real) ? Kind.TAKE : null;
		}
		if (real.is(Blocks.FARMLAND)) {
			return TerrainKinds.family(target) == TerrainKinds.Family.SOIL ? Kind.TRAMPLE : null;
		}
		if (!TerrainKinds.liftable(level, pos, real)) {
			return null;
		}
		if (wanted.contains(real.getBlock()) && TerrainKinds.family(real) == null) {
			// A loose natural thing a cairn wants (a flower, a pumpkin): fetch it. Never dig ground out for an offering.
			return Kind.TAKE;
		}
		TerrainKinds.Family family = TerrainKinds.family(real);
		if (family != null) {
			int surface = TargetWorld.surface(level, pos.getX(), pos.getZ());
			if (target.isAir()) {
				// Ground where there was none: a pile, a pillar, a wall of dirt.
				return Kind.TAKE;
			}
			TerrainKinds.Family targetFamily = TerrainKinds.family(target);
			if (targetFamily != null && targetFamily != family && pos.getY() >= surface - 1 && !nearWater(level, pos)) {
				// The wrong material on the top layer (sand where grass was).
				return Kind.TAKE;
			}
			return null;
		}
		if (TerrainKinds.isNatural(real)) {
			// Trees, ores, plants, pumpkins: features the target can't reproduce. Only fetched when a cairn wants them.
			return null;
		}
		int surface = TargetWorld.surface(level, pos.getX(), pos.getZ());
		if (pos.getY() >= surface - 2 && exposed(level, pos) && random.nextFloat() < EndermanTuning.UNNATURAL_CHANCE) {
			return Kind.TAKE;
		}
		return null;
	}

	/**
	 * A spot near {@code around} that needs {@code carried}: a missing block of a nearby cairn first, else a hole in
	 * the terrain whose original block is in the same family. Null when there's none.
	 */
	public static @Nullable BlockPos destination(ServerLevel level, BlockPos around, BlockState carried, RandomSource random) {
		List<CairnSite> sites = CairnSites.near(level, around, EndermanTuning.SITE_WORK_RADIUS);
		sites.sort(java.util.Comparator.comparingDouble(s -> s.origin().distSqr(around)));
		for (CairnSite site : sites) {
			if (EndermanWards.isWarded(level, site.origin())) {
				continue;
			}
			BlockPos spot = site.spotFor(level, carried);
			if (spot != null) {
				return spot;
			}
		}
		return hole(level, around, carried, random);
	}

	/** A hole near {@code around} (air where the original terrain was solid, with a floor) that {@code carried} fills. */
	public static @Nullable BlockPos hole(ServerLevel level, BlockPos around, BlockState carried, RandomSource random) {
		if (TerrainKinds.family(carried) == null) {
			return null;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int near = NEAR.size();
		for (int i = 0; i < near + EndermanTuning.FILL_COLUMNS; i++) {
			int x = around.getX() + (i < near ? NEAR.get(i)[0] : random.nextInt(2 * EndermanTuning.FILL_RADIUS + 1) - EndermanTuning.FILL_RADIUS);
			int z = around.getZ() + (i < near ? NEAR.get(i)[1] : random.nextInt(2 * EndermanTuning.FILL_RADIUS + 1) - EndermanTuning.FILL_RADIUS);
			BlockPos hole = holeInColumn(level, x, z, carried, pos);
			if (hole != null) {
				return hole;
			}
		}
		return null;
	}

	/**
	 * The bottom of the hole in this column, if it has one {@code carried} fills: scanning down from the original
	 * surface to the first real block (one block read for untouched ground), the air just above it.
	 */
	public static @Nullable BlockPos holeInColumn(ServerLevel level, int x, int z, BlockState carried, BlockPos.MutableBlockPos pos) {
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return null;
		}
		int surface = TargetWorld.surface(level, x, z);
		if (surface == Integer.MIN_VALUE) {
			return null;
		}
		for (int y = surface; y > surface - EndermanTuning.FILL_DEPTH; y--) {
			pos.set(x, y, z);
			if (!level.getBlockState(pos).isAir()) {
				if (y == surface) {
					return null;
				}
				pos.set(x, y + 1, z);
				return needsFill(level, pos, carried) ? pos.immutable() : null;
			}
		}
		return null;
	}

	/** Whether {@code pos} is a hole {@code carried} should fill now: air, a solid floor, and the target is its family. */
	public static boolean needsFill(ServerLevel level, BlockPos pos, BlockState carried) {
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}
		BlockPos below = pos.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
			return false;
		}
		BlockState target = TargetWorld.target(level, pos);
		return target != null && !target.isAir() && target.getFluidState().isEmpty() && TerrainKinds.fills(carried, target)
				&& CairnSites.covering(level, pos) == null;
	}

	/** What to put at {@code pos} for {@code carried}: the cairn's design, or the original terrain block of its family. */
	public static @Nullable BlockState placement(ServerLevel level, BlockPos pos, BlockState carried) {
		CairnSite site = CairnSites.covering(level, pos);
		BlockState cairn = site == null ? null : site.at(pos);
		if (cairn != null) {
			return cairn.getBlock() == carried.getBlock() && CairnSite.canPlace(level, pos, cairn) ? cairn : null;
		}
		if (needsFill(level, pos, carried)) {
			return TargetWorld.target(level, pos);
		}
		return null;
	}

	private static boolean exposed(ServerLevel level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			if (level.getBlockState(pos.relative(d)).isAir()) {
				return true;
			}
		}
		return false;
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

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import java.util.List;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Sand heaped on the sea floor round the feet of a deep-sea fortress's support pillars. */
public final class SandPiles {
	/** How far a pile spreads from a pillar foot. */
	private static final int REACH = 2;
	/** How far up or down from the foot a pile follows the floor. */
	private static final int FLOOR_SEARCH = 2;
	/** Chance in percent of sand on the floor, by ring (next to the pillar, one further), and of a second layer next to it. */
	private static final int NEAR = 85;
	private static final int FAR = 35;
	private static final int SECOND_LAYER = 30;

	private SandPiles() {
	}

	public static void place(WorldGenLevel level, BoundingBox chunkBB, List<BlockPos> feet, List<StructurePiece> pieces) {
		if (feet.isEmpty()) {
			return;
		}
		List<BoundingBox> near = pieces.stream().map(StructurePiece::getBoundingBox).filter(b -> b.intersects(chunkBB.inflatedBy(REACH + 1))).toList();
		for (BlockPos foot : feet) {
			for (int dx = -REACH; dx <= REACH; dx++) {
				for (int dz = -REACH; dz <= REACH; dz++) {
					int ring = Math.max(Math.abs(dx), Math.abs(dz));
					if (ring == 0) {
						continue;
					}
					BlockPos floorTop = floorTop(level, chunkBB, foot.offset(dx, 0, dz));
					if (floorTop == null || near.stream().anyMatch(b -> b.isInside(floorTop))) {
						continue;
					}
					int roll = roll(floorTop);
					boolean side = dx == 0 || dz == 0;
					if (roll < (ring == 1 ? NEAR : FAR)) {
						level.setBlock(floorTop, Blocks.SAND.defaultBlockState(), 2);
						BlockPos up = floorTop.above();
						if (ring == 1 && side && roll(up) < SECOND_LAYER && isWater(level, up) && chunkBB.isInside(up)) {
							level.setBlock(up, Blocks.SAND.defaultBlockState(), 2);
						}
					}
				}
			}
		}
	}

	/** The lowest water block on solid floor near this column, or null. */
	private static BlockPos floorTop(WorldGenLevel level, BoundingBox chunkBB, BlockPos near) {
		for (int dy = FLOOR_SEARCH; dy >= -FLOOR_SEARCH; dy--) {
			BlockPos p = near.above(dy);
			if (!chunkBB.isInside(p) || !isWater(level, p)) {
				continue;
			}
			BlockState below = level.getBlockState(p.below());
			if (!below.isAir() && below.getFluidState().isEmpty()) {
				return p;
			}
		}
		return null;
	}

	private static boolean isWater(WorldGenLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(Blocks.WATER);
	}

	private static int roll(BlockPos pos) {
		return (int) Math.floorMod(Mth.getSeed(pos), 100L);
	}
}

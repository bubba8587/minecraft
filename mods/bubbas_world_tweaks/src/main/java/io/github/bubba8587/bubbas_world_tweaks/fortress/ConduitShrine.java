// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** The conduit shrine over the central crossing: a 16-block prismarine ring round an empty, flooded heart on a pedestal. */
public final class ConduitShrine {
	/** Heart height above the crossing's base. */
	private static final int HEART_Y = 7;

	private ConduitShrine() {
	}

	/** Where a conduit placed in the shrine over this crossing goes. */
	public static BlockPos heart(BoundingBox crossing) {
		return new BlockPos(crossing.minX() + crossing.getXSpan() / 2, crossing.minY() + HEART_Y, crossing.minZ() + crossing.getZSpan() / 2);
	}

	/** Builds the shrine's part inside chunkBB. */
	public static void place(WorldGenLevel level, BoundingBox chunkBB, BoundingBox crossing) {
		BlockPos heart = heart(crossing);
		BlockState water = Blocks.WATER.defaultBlockState();
		BlockState pedestal = Blocks.PRISMARINE_WALL.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				for (int dy = -2; dy <= 1; dy++) {
					BlockPos pos = heart.offset(dx, dy, dz);
					if (!chunkBB.isInside(pos)) {
						continue;
					}
					BlockState state;
					if (dx == 0 && dz == 0 && dy < 0) {
						state = pedestal;
					} else if (ring && dy == 0) {
						state = frame(dx, dz);
					} else if (!ring && dy >= -1) {
						state = water;
					} else {
						continue;
					}
					level.setBlock(pos, state, 2);
				}
			}
		}
	}

	/** Sea lanterns at the corners, prismarine bricks and dark prismarine alternating between. */
	private static BlockState frame(int dx, int dz) {
		if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
			return Blocks.SEA_LANTERN.defaultBlockState();
		}
		return ((dx + dz) & 1) == 0 ? Blocks.DARK_PRISMARINE.defaultBlockState() : Blocks.PRISMARINE_BRICKS.defaultBlockState();
	}
}

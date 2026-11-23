// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Places a code-built structure piece's blocks only inside the chunk being generated, and reads the ground the same way
 * in every chunk (crypt yards, monster camps). A piece decides everything from its own seed; each chunk it spans builds
 * its own part.
 */
public class ClippedBuild {
	protected final WorldGenLevel level;
	protected final BoundingBox chunk;

	public ClippedBuild(WorldGenLevel level, BoundingBox chunk) {
		this.level = level;
		this.chunk = chunk;
	}

	public WorldGenLevel level() {
		return level;
	}

	public boolean inside(int x, int y, int z) {
		return chunk.isInside(x, y, z);
	}

	/**
	 * The ground of this column: the top block that isn't a tree, a plant or air, or MIN_VALUE when the column isn't
	 * in this chunk or the ground is under water. Read the same way in every chunk (the yard is built after trees,
	 * which it clears).
	 */
	public int ground(int x, int z) {
		if (x < chunk.minX() || x > chunk.maxX() || z < chunk.minZ() || z > chunk.maxZ()) {
			return Integer.MIN_VALUE;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
		while (pos.getY() > level.getMinY() && cover(level.getBlockState(pos))) {
			pos.move(0, -1, 0);
		}
		return level.getFluidState(pos).isEmpty() ? pos.getY() : Integer.MIN_VALUE;
	}

	/** Trees and plants standing on the ground (cleared out of a yard), and air. */
	public static boolean cover(BlockState state) {
		return state.isAir() || state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.LEAVES)
				|| state.is(net.minecraft.tags.BlockTags.REPLACEABLE) || state.is(Blocks.VINE) || state.is(Blocks.BEE_NEST)
				|| state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
				|| state.is(net.minecraft.tags.BlockTags.FLOWERS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.PUMPKIN);
	}

	/** Clears trees and plants from the ground up to {@code height} above it, in this column. */
	public void clearCover(int x, int z, int height) {
		int g = ground(x, z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		for (int y = g + 1; y <= g + height; y++) {
			if (inside(x, y, z) && !level.getBlockState(new BlockPos(x, y, z)).isAir() && cover(level.getBlockState(new BlockPos(x, y, z)))) {
				put(x, y, z, Blocks.AIR.defaultBlockState());
			}
		}
	}

	public void put(int x, int y, int z, BlockState state) {
		if (inside(x, y, z)) {
			level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
		}
	}

	public void putIfAir(int x, int y, int z, BlockState state) {
		if (inside(x, y, z) && level.getBlockState(new BlockPos(x, y, z)).isAir()) {
			put(x, y, z, state);
		}
	}

	public void clearAbove(int x, int y, int z, int height) {
		for (int dy = 1; dy <= height; dy++) {
			if (inside(x, y + dy, z) && !level.getBlockState(new BlockPos(x, y + dy, z)).isAir()) {
				put(x, y + dy, z, Blocks.AIR.defaultBlockState());
			}
		}
	}
}

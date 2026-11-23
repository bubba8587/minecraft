// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A clipped build under a ceiling (Nether outposts, specs/monster-camps.md "Nether outposts"): the ground of a column is
 * the surface nearest the outpost's own height (at or below it first), not the top of the world, and a column whose
 * surface there is lava has none.
 */
public class NetherBuild extends ClippedBuild {
	/** How far below and above the outpost's height a column's ground is looked for. */
	static final int BELOW = 10;
	static final int ABOVE = 8;
	private final int refY;

	public NetherBuild(WorldGenLevel level, BoundingBox chunk, int refY) {
		super(level, chunk);
		this.refY = refY;
	}

	/** Air, plants and the fungus forest's growth (stems, wart, shroomlights, vines). */
	public static boolean netherCover(BlockState state) {
		return cover(state) || state.is(Blocks.NETHER_WART_BLOCK) || state.is(Blocks.WARPED_WART_BLOCK) || state.is(Blocks.SHROOMLIGHT)
				|| state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT) || state.is(Blocks.TWISTING_VINES)
				|| state.is(Blocks.TWISTING_VINES_PLANT) || state.is(Blocks.CRIMSON_FUNGUS) || state.is(Blocks.WARPED_FUNGUS) || state.is(Blocks.FIRE);
	}

	private static boolean open(BlockState state) {
		return netherCover(state) && state.getFluidState().isEmpty();
	}

	@Override
	public int ground(int x, int z) {
		if (x < chunk.minX() || x > chunk.maxX() || z < chunk.minZ() || z > chunk.maxZ()) {
			return Integer.MIN_VALUE;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, refY + 1, z);
		boolean openAbove = open(level.getBlockState(pos));
		for (int y = refY; y >= refY - BELOW; y--) {
			BlockState state = level.getBlockState(pos.setY(y));
			if (!state.getFluidState().isEmpty()) {
				if (openAbove) {
					return Integer.MIN_VALUE; // a lava surface
				}
			} else if (!netherCover(state) && openAbove) {
				return y;
			}
			openAbove = open(state);
		}
		for (int y = refY + 1; y <= refY + ABOVE; y++) {
			BlockState state = level.getBlockState(pos.setY(y));
			BlockState above = level.getBlockState(pos.setY(y + 1));
			if (!netherCover(state) && state.getFluidState().isEmpty() && open(above)) {
				return y;
			}
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public void clearCover(int x, int z, int height) {
		int g = ground(x, z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		for (int y = g + 1; y <= g + height; y++) {
			BlockState state = level.getBlockState(new BlockPos(x, y, z));
			if (inside(x, y, z) && !state.isAir() && netherCover(state) && state.getFluidState().isEmpty()) {
				put(x, y, z, Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** Lava in this column from {@code from} to {@code to} (springs, pockets) turned to blackstone. */
	public void seal(int x, int z, int from, int to) {
		for (int y = from; y <= to; y++) {
			if (inside(x, y, z) && !level.getFluidState(new BlockPos(x, y, z)).isEmpty()) {
				put(x, y, z, Blocks.BLACKSTONE.defaultBlockState());
			}
		}
	}

	/** Headroom:everything solid from above this column's ground up to {@code top} is carved out (lava is walled, not opened). */
	public void carve(int x, int z, int top) {
		int g = ground(x, z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		for (int y = g + 1; y <= top; y++) {
			BlockState state = level.getBlockState(new BlockPos(x, y, z));
			if (inside(x, y, z) && !state.isAir()) {
				put(x, y, z, state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
			}
		}
	}
}

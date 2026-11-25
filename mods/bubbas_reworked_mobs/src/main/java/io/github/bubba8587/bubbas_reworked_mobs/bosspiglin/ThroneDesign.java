// dte:C1,B15,B17,B16
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Boss Piglin's throne, vanilla blocks: a 2 x 2 seat of polished blackstone brick slabs with purple cushions on them with a backrest row behind
 * it, and crimson shelf armrests either side of the seat (4 wide x 3 deep in all). Offsets are from the front seat cell
 * on the seated boss's left; the other cell of a row is {@code facing.getClockWise()} of it and the rows go back along
 * {@code facing.getOpposite()}; the left armrest is at across -1. Backrest, bottom to top: stripped crimson hyphae,
 * gilded blackstone, chiseled polished blackstone, gold blocks, framed by stripped crimson stem posts with crimson fence tops.
 */
public final class ThroneDesign {
	public static final int WIDTH = 2;
	public static final int DEPTH = 3;
	public static final int HEIGHT = 4;
	/** The whole footprint across, armrests included: from across {@link #MIN_ACROSS} for this many cells. */
	public static final int OUTER_WIDTH = WIDTH + 2;
	public static final int MIN_ACROSS = -1;
	/** Seat centre distance behind the throne's front face, in blocks (fits BossPiglinModel at its render scale). */
	public static final double SEAT_DEPTH = 0.98;
	/** Seat surface height above the throne's floor: the slab (0.5) plus a cushion (0.25). */
	public static final double SEAT_HEIGHT = 0.75;

	public record Placed(BlockPos offset, BlockState state) {
	}

	private ThroneDesign() {
	}

	public static List<Placed> blocks(Direction facing) {
		Direction side = facing.getClockWise();
		Direction back = facing.getOpposite();
		List<Placed> out = new ArrayList<>();
		BlockState seat = Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState();
		for (int depth = 0; depth < 2; depth++) {
			for (int across = 0; across < WIDTH; across++) {
				out.add(new Placed(at(side, back, across, 0, depth), seat));
			}
		}
		// armrests: crimson shelves beside the seat, their backs inward against the seat
		for (int depth = 0; depth < 2; depth++) {
			out.add(new Placed(at(side, back, -1, 0, depth), Blocks.CRIMSON_SHELF.defaultBlockState().setValue(ShelfBlock.FACING, side.getOpposite())));
			out.add(new Placed(at(side, back, WIDTH, 0, depth), Blocks.CRIMSON_SHELF.defaultBlockState().setValue(ShelfBlock.FACING, side)));
		}
		// crimson posts at the back corners, framing the backrest
		BlockState post = Blocks.STRIPPED_CRIMSON_STEM.defaultBlockState();
		for (int y = 0; y < HEIGHT - 1; y++) {
			out.add(new Placed(at(side, back, -1, y, 2), post));
			out.add(new Placed(at(side, back, WIDTH, y, 2), post));
		}
		out.add(new Placed(at(side, back, -1, HEIGHT - 1, 2), Blocks.CRIMSON_FENCE.defaultBlockState()));
		out.add(new Placed(at(side, back, WIDTH, HEIGHT - 1, 2), Blocks.CRIMSON_FENCE.defaultBlockState()));
		Block[] column = {Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.GILDED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.GOLD_BLOCK};
		for (int y = 0; y < column.length; y++) {
			for (int across = 0; across < WIDTH; across++) {
				out.add(new Placed(at(side, back, across, y, 2), column[y].defaultBlockState()));
			}
		}
		return out;
	}

	/** Where the four purple cushions go (vanilla cushion entities: their bottom centre), relative to the origin cell's minimum corner. */
	public static List<Vec3> cushions(Direction facing) {
		Direction side = facing.getClockWise();
		Direction back = facing.getOpposite();
		List<Vec3> out = new ArrayList<>();
		for (int depth = 0; depth < 2; depth++) {
			for (int across = 0; across < WIDTH; across++) {
				BlockPos cell = at(side, back, across, 0, depth);
				out.add(new Vec3(cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5));
			}
		}
		return out;
	}

	/** Where the seated boss's position goes (its feet/bottom), relative to the origin cell's minimum corner. */
	public static Vec3 seat(Direction facing) {
		Direction side = facing.getClockWise();
		double forward = 0.5 - SEAT_DEPTH;
		return new Vec3(
			0.5 + side.getStepX() * 0.5 + facing.getStepX() * forward,
			SEAT_HEIGHT,
			0.5 + side.getStepZ() * 0.5 + facing.getStepZ() * forward);
	}

	private static BlockPos at(Direction side, Direction back, int across, int up, int depth) {
		return BlockPos.ZERO.relative(side, across).relative(back, depth).above(up);
	}
}

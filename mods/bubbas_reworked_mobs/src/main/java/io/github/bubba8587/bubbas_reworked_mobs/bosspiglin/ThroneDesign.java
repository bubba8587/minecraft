// dte:C1,B15,B17,B16
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Boss Piglin's throne, vanilla blocks: a 2 x 2 seat of polished blackstone brick slabs with a backrest row behind
 * it (2 wide x 3 deep in all). Offsets are from the front seat cell on the seated boss's left; the other cell of a row
 * is {@code facing.getClockWise()} of it and the rows go back along {@code facing.getOpposite()}. Backrest, bottom to
 * top: polished blackstone bricks, gilded blackstone, chiseled polished blackstone, gold blocks.
 */
public final class ThroneDesign {
	public static final int WIDTH = 2;
	public static final int DEPTH = 3;
	public static final int HEIGHT = 4;
	/** Seat centre distance behind the throne's front face, in blocks (fits BossPiglinModel at its render scale). */
	public static final double SEAT_DEPTH = 0.98;
	/** Seat surface height above the throne's floor. */
	public static final double SEAT_HEIGHT = 0.5;

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
		Block[] column = {Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.GOLD_BLOCK};
		for (int y = 0; y < column.length; y++) {
			for (int across = 0; across < WIDTH; across++) {
				out.add(new Placed(at(side, back, across, y, 2), column[y].defaultBlockState()));
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

// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Placeholder throne (the art agent's version replaces it): a 2 x 2 slab seat, a gilded backrest row behind. */
public final class ThroneDesign {
	public static final int WIDTH = 2;
	public static final int DEPTH = 3;
	public static final int HEIGHT = 4;

	private ThroneDesign() {
	}

	/** One throne block: its offset from the throne's front-left floor cell (the seated boss's left), and its state. */
	public record Placed(BlockPos offset, BlockState state) {
	}

	/** The throne's blocks facing {@code facing} (the way the seated boss looks). */
	public static List<Placed> blocks(Direction facing) {
		Direction right = facing.getClockWise();
		Direction back = facing.getOpposite();
		List<Placed> out = new ArrayList<>();
		for (int u = 0; u < WIDTH; u++) {
			BlockPos front = BlockPos.ZERO.relative(right, u);
			out.add(new Placed(front, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState()));
			out.add(new Placed(front.relative(back), Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState()));
			BlockPos rest = front.relative(back, 2);
			out.add(new Placed(rest, Blocks.GILDED_BLACKSTONE.defaultBlockState()));
			out.add(new Placed(rest.above(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()));
			out.add(new Placed(rest.above(2), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()));
			out.add(new Placed(rest.above(3), Blocks.GOLD_BLOCK.defaultBlockState()));
		}
		return out;
	}

	/** Where the seated boss's feet go, from the front-left floor cell's corner: the middle of the 2 x 2 seat. */
	public static Vec3 seat(Direction facing) {
		Direction right = facing.getClockWise();
		return new Vec3(0.5 + (right.getStepX() - facing.getStepX()) * 0.5 + facing.getStepX() * 0.02, 0.5,
				0.5 + (right.getStepZ() - facing.getStepZ()) * 0.5 + facing.getStepZ() * 0.02);
	}
}

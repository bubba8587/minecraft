// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Placeholder throne (the art agent's version replaces it): slab seats in front, a gilded back behind. */
public final class ThroneDesign {
	private ThroneDesign() {
	}

	/** One throne block: its offset from the throne's front-left floor cell, and its state. */
	public record Placed(BlockPos offset, BlockState state) {
	}

	/** The throne's blocks facing {@code facing} (the way the seated boss looks). */
	public static List<Placed> blocks(Direction facing) {
		Direction right = facing.getClockWise();
		Direction back = facing.getOpposite();
		List<Placed> out = new ArrayList<>();
		for (int u = 0; u <= 1; u++) {
			BlockPos front = BlockPos.ZERO.relative(right, u);
			out.add(new Placed(front, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState()));
			BlockPos rear = front.relative(back);
			out.add(new Placed(rear, Blocks.GILDED_BLACKSTONE.defaultBlockState()));
			out.add(new Placed(rear.above(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()));
			out.add(new Placed(rear.above(2), Blocks.GOLD_BLOCK.defaultBlockState()));
		}
		return out;
	}

	/** Where the seated boss's feet go, from the front-left floor cell's corner. */
	public static Vec3 seat(Direction facing) {
		Direction right = facing.getClockWise();
		return new Vec3(0.5 + right.getStepX() * 0.5, 0.5, 0.5 + right.getStepZ() * 0.5);
	}
}

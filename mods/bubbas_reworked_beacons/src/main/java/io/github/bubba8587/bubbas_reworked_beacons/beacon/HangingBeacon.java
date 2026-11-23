// dte:C5,B15
package io.github.bubba8587.bubbas_reworked_beacons.beacon;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The upside-down beacon. */
public final class HangingBeacon {
	public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");
	/** The block id that gets the property, as set by setId. */
	public static final String BLOCK_ID = "minecraft:beacon";

	private HangingBeacon() {
	}

	public static boolean isUpsideDown(BlockState state) {
		return state.getValueOrElse(UPSIDE_DOWN, false);
	}

	public static boolean isHangingBeacon(BlockState state) {
		return state.is(Blocks.BEACON) && isUpsideDown(state);
	}
}

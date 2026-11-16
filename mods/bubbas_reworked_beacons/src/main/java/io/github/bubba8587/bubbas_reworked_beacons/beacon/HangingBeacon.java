// dte:C5,B15
package io.github.bubba8587.bubbas_reworked_beacons.beacon;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The upside-down beacon: a block state property added to the vanilla beacon, not a new block (B15 fewNewBlocks;
 * docs/knowledge/item-keyed-block-drops.md approach C). {@code BlockExtraPropertyMixin} adds it while the beacon is
 * constructed and keeps {@code false} the default, so worldgen, commands and old saves give ordinary beacons;
 * placing a beacon against the underside of a block sets it, like a hanging lantern. The model is vanilla's,
 * turned over (assets/minecraft/blockstates/beacon.json).
 *
 * <p>This class is loaded while {@code Blocks} is being initialised, so it must not touch any registry.
 */
public final class HangingBeacon {
	public static final BooleanProperty UPSIDE_DOWN = BooleanProperty.create("upside_down");
	/** The block id that gets the property, as set by {@code BlockBehaviour.Properties#setId}. */
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

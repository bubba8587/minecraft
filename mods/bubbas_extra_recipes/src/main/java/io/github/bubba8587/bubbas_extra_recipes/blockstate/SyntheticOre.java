// dte:C3,B15
package io.github.bubba8587.bubbas_extra_recipes.blockstate;

import java.util.Set;

import io.github.bubba8587.bubbas_extra_recipes.network.ExtraStatesHandshake;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Item-keyed drops on a block with no block entity, without a new block. */
public final class SyntheticOre {
	public static final BooleanProperty SYNTHETIC = BooleanProperty.create("synthetic");

	/** Block ids that get the property. */
	public static final Set<String> BLOCK_IDS = Set.of("minecraft:deepslate_diamond_ore");

	private SyntheticOre() {
	}

	public static boolean isSynthetic(BlockState state) {
		return state.getValueOrElse(SYNTHETIC, false);
	}

	public static void initialize() {
		ExtraStatesHandshake.initialize();
	}
}

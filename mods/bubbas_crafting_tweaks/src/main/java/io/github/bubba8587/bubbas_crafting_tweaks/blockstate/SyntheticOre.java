// dte:C3,B15
package io.github.bubba8587.bubbas_crafting_tweaks.blockstate;

import java.util.Set;

import io.github.bubba8587.bubbas_crafting_tweaks.network.ExtraStatesHandshake;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Item-keyed drops on a block with no block entity, without a new block (specs/crafting-tweaks-mod.md,
 * docs/knowledge/item-keyed-block-drops.md approach C, ported from refmod).
 *
 * <p>{@code BlockExtraPropertyMixin} adds the boolean {@link #SYNTHETIC} to the vanilla blocks in {@link #BLOCK_IDS}
 * while they are constructed, and keeps {@code synthetic=false} as their default state, so worldgen, commands and old
 * saves all give natural ore. Everything else is data: an item with {@code minecraft:block_state {synthetic:"true"}}
 * places the synthetic state, and the block's loot table branches on it with {@code match_block}.
 *
 * <p>This class is loaded while {@code Blocks} is being initialised, so it must not touch any registry.
 */
public final class SyntheticOre {
	public static final BooleanProperty SYNTHETIC = BooleanProperty.create("synthetic");

	/** Block ids (as in {@code BlockBehaviour.Properties#setId}) that get the property. */
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

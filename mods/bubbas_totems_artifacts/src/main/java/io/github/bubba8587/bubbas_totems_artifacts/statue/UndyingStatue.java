// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

/** Sneak-using vanilla's Totem of Undying on a block sets down its statue. */
public final class UndyingStatue {
	private UndyingStatue() {
	}

	public static void initialize() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator() || !player.isSecondaryUseActive() || !player.getAbilities().mayBuild
					|| !player.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) {
				return InteractionResult.PASS;
			}
			return TotemStatueBlock.placeFrom(new UseOnContext(player, hand, hit), TotemType.UNDYING);
		});
	}
}

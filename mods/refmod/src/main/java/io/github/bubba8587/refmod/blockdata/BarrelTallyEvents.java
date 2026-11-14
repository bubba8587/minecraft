// dte:B5,B6
package io.github.bubba8587.refmod.blockdata;

import io.github.bubba8587.refmod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

/**
 * Behaviour on an existing vanilla block through an event, not a new block (spec requirement 17b, B15 fewNewBlocks):
 * right-clicking a vanilla {@code minecraft:barrel} with the tally stick counts up a per-barrel tally stored in
 * {@link BarrelTallies}, instead of opening the barrel. Cleanup when the barrel is removed: {@code BarrelBlockMixin}.
 */
public final class BarrelTallyEvents {
	private BarrelTallyEvents() {
	}

	public static void initialize() {
		// Fires from ServerPlayerGameMode#useItemOn (and the client's prediction) before the block's or the item's own use logic.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator() || !player.getItemInHand(hand).is(ModItems.TALLY_STICK)
					|| !level.getBlockState(hit.getBlockPos()).is(Blocks.BARREL)) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				int count = BarrelTallies.increment(level, hit.getBlockPos());
				player.sendOverlayMessage(Component.translatable("message.refmod.barrel_tally", count));
			}
			// Non-PASS on both sides: the client swings and sends the packet, the server skips the barrel menu.
			return InteractionResult.SUCCESS;
		});
	}
}

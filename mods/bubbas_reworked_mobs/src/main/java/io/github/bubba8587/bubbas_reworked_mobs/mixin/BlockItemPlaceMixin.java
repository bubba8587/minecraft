// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnGifts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player's placed block that completes a cairn earns its completion gift (enderman/CairnGifts; the Totem of the End
 * with Bubba's Totems & Artifacts). Fabric API has no block-placed event, so this watches vanilla's one place where a
 * block item puts its block down. One covering-site lookup per placed block.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {
	@Inject(method = "placeBlock", at = @At("RETURN"))
	private void brm$cairnCompleted(BlockPlaceContext context, BlockState placementState, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player) {
			CairnGifts.placedBy(level, context.getClickedPos(), player);
		}
	}
}

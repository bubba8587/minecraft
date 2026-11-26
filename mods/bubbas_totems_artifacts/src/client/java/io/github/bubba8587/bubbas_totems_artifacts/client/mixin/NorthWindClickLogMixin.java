// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.client.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Diagnostic (temporary, North Wind "stops working" bug): the client's state at each right-click with the totem in hand. */
@Mixin(Minecraft.class)
abstract class NorthWindClickLogMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"))
	private void bta$logNorthWindClick(CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (mc.player == null || mc.gameMode == null) {
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			var stack = mc.player.getItemInHand(hand);
			if (stack.is(ModItems.TOTEM_OF_NORTH_WIND)) {
				BubbasTotemsArtifacts.LOGGER.info("[north-wind-debug] client click: hand={} destroying={} handsBusy={} cooldown={} fallFlying={} usingItem={} hit={}",
						hand, mc.gameMode.isDestroying(), mc.player.isHandsBusy(), mc.player.getCooldowns().isOnCooldown(stack),
						mc.player.isFallFlying(), mc.player.isUsingItem(), mc.hitResult == null ? "none" : mc.hitResult.getType());
			}
		}
	}
}

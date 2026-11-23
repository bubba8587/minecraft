// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.display.HandPlacing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Sneak + middle click with an item in hand: lay it on the ground as a display. */
@Mixin(Minecraft.class)
abstract class LayItemPickMixin {
	@Shadow
	public @Nullable LocalPlayer player;

	@Shadow
	public @Nullable HitResult hitResult;

	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void brm$layItem(CallbackInfo ci) {
		if (player != null && player.isShiftKeyDown() && !player.getMainHandItem().isEmpty() && hitResult instanceof BlockHitResult hit
				&& hit.getType() == HitResult.Type.BLOCK) {
			ClientPlayNetworking.send(new HandPlacing.LayRequest(hit.getBlockPos(), hit.getDirection()));
			ci.cancel();
		}
	}
}

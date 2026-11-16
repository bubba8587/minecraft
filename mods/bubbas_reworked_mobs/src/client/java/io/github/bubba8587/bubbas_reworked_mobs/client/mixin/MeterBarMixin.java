// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.client.meter.MeterBars;
import io.github.bubba8587.bubbas_reworked_mobs.client.meter.MeterState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mob with a visible meter (game rule show_meters) gets a small boss-bar-style bar floating over it: over its head,
 * or, for a drowned holding someone, over the victim's head (so it doesn't draw across their body).
 */
@Mixin(EntityRenderer.class)
abstract class MeterBarMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
	private void brm$meterBar(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		MeterBars.extract(entity, state, partialTicks);
	}

	@Inject(method = "submit", at = @At("TAIL"))
	private void brm$submitMeterBar(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
		if (((MeterState) state).brm$meter() != 0) {
			MeterBars.submit((MeterState) state, poseStack, submitNodeCollector, camera);
		}
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.client.meter.MeterBars;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A mob with a visible meter (game rule show_meters) gets its bar in the below-name line, where vanilla draws scoreboard text. */
@Mixin(EntityRenderer.class)
abstract class MeterBarMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
	private void brm$meterBar(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		Component bar = MeterBars.bar(entity);
		if (bar == null || state.distanceToCameraSq > MeterBars.SHOW_DISTANCE * MeterBars.SHOW_DISTANCE) {
			return;
		}
		state.scoreText = bar;
		if (state.nameTagAttachment == null || state.nameTag == null) {
			state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
		}
	}
}

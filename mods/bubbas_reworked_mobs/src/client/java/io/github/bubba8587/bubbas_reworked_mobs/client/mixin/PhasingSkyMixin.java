// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Phasing blindness: no sky, as with Blindness and Darkness. */
@Mixin(Camera.class)
abstract class PhasingSkyMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void brm$phasingBlocksSky(CameraRenderState cameraState, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (((Camera) (Object) this).entity() instanceof LivingEntity living && living.hasEffect(Phasing.PHASING)) {
			cameraState.entityRenderState.doesMobEffectBlockSky = true;
		}
	}
}

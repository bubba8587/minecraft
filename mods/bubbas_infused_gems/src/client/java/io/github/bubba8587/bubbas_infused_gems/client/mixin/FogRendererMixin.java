// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.effect.GemValues;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Darkness and Blindness gems worn. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@ModifyReturnValue(method = "setupFog", at = @At("RETURN"))
	private FogData bubbas$gemFog(FogData fog, Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount,
			ClientLevel level, boolean shouldCreateBossFog) {
		if (!(camera.entity() instanceof LivingEntity entity)) {
			return fog;
		}
		float render = renderDistanceInChunks * 16;
		double blind = entity.hasEffect(MobEffects.BLINDNESS) ? 0.0 : GemValues.rawWorn(entity, "blindness");
		double dark = entity.hasEffect(MobEffects.DARKNESS) ? 0.0 : GemValues.rawWorn(entity, "darkness");
		if (blind > 0.0) {
			float d = Mth.lerp((float) blind, render, 5.0F);
			apply(fog, d, 0.25F, 0.8F);
		}
		if (dark > 0.0) {
			float d = Mth.lerp((float) dark, render, 15.0F);
			apply(fog, d, 0.75F, 1.0F);
		}
		double shade = Math.max(blind, dark);
		if (shade > 0.0) {
			float k = Mth.square(1.0F - (float) shade);
			fog.color.set(fog.color.x() * k, fog.color.y() * k, fog.color.z() * k, fog.color.w());
		}
		return fog;
	}

	private static void apply(FogData fog, float distance, float startShare, float skyShare) {
		if (distance < fog.environmentalEnd) {
			fog.environmentalStart = Math.min(fog.environmentalStart, distance * startShare);
			fog.environmentalEnd = distance;
		}
		fog.skyEnd = Math.min(fog.skyEnd, distance * skyShare);
		fog.cloudEnd = Math.min(fog.cloudEnd, distance * skyShare);
	}
}

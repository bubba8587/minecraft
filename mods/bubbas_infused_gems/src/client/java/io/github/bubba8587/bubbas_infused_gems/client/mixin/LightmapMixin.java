// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemValues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Night Vision gem: the lightmap's night-vision intensity is at least the gem's. */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {
	@Inject(method = "extract", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.PUTFIELD,
			target = "Lnet/minecraft/client/renderer/state/LightmapRenderState;nightVisionColor:Lorg/joml/Vector3fc;"))
	private void bubbas$gemNightVision(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			double s = GemValues.value(player, "night_vision");
			if (s > 0.0) {
				float intensity = (float) (s * Balance.get().effect("night_vision").wornNumber("intensity", 1.0));
				state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, intensity);
			}
			double dark = GemValues.rawWorn(player, "darkness");
			if (dark > 0.0 && !player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) {
				float option = Minecraft.getInstance().options.darknessEffectScale().get().floatValue();
				state.darknessEffectScale = (float) (0.45 * dark * option);
			}
			double conduit = GemValues.value(player, "conduit_power");
			float waterVision = player.getWaterVision();
			if (conduit > 0.0 && waterVision > 0.0F) {
				float sight = (float) (conduit * waterVision * Balance.get().effect("conduit_power").wornNumber("underwater_sight", 1.0));
				state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, sight);
			}
		}
	}
}

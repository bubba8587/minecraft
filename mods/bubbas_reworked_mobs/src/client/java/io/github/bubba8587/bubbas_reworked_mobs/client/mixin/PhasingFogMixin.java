// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.client.PhasingFogEnvironment;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Phasing blindness (soulfire/client/PhasingFogEnvironment): first in vanilla's fog list. */
@Mixin(FogRenderer.class)
abstract class PhasingFogMixin {
	@Shadow
	@Final
	private static List<FogEnvironment> FOG_ENVIRONMENTS;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void brm$phasingFog(CallbackInfo ci) {
		FOG_ENVIRONMENTS.addFirst(new PhasingFogEnvironment());
	}
}

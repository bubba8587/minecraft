// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.client.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clear Skies. */
@Mixin(ClientLevel.class)
abstract class ClientLevelPrecipitationMixin {
	@Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
	private void bta$clearSkies(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		if (ClearSkies.clearAt((ClientLevel) (Object) this, pos.getX(), pos.getZ())) {
			cir.setReturnValue(Biome.Precipitation.NONE);
		}
	}
}

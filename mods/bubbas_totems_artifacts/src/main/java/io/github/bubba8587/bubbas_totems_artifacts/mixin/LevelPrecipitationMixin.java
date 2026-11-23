// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clear Skies. */
@Mixin(Level.class)
abstract class LevelPrecipitationMixin {
	@Inject(method = "precipitationAt", at = @At("HEAD"), cancellable = true)
	private void bta$clearSkies(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		Level level = (Level) (Object) this;
		if (level.isRaining() && ClearSkies.clearAt(level, pos.getX(), pos.getZ())) {
			cir.setReturnValue(Biome.Precipitation.NONE);
		}
	}
}

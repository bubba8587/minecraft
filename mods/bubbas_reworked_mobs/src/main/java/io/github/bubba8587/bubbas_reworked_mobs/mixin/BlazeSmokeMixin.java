// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_reworked_mobs.captain.CaptainTuning;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireBlaze;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Soulfire Blazes trail soul particles instead of a blaze's large smoke. */
@Mixin(Blaze.class)
abstract class BlazeSmokeMixin {
	@WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private void brm$soulSmoke(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz,
			Operation<Void> original) {
		Blaze self = (Blaze) (Object) this;
		if (!(self instanceof SoulfireBlaze) || particle != ParticleTypes.LARGE_SMOKE) {
			original.call(level, particle, x, y, z, dx, dy, dz);
		} else if (self.getRandom().nextInt(CaptainTuning.SOUL_SMOKE_ONE_IN) == 0) {
			original.call(level, ParticleTypes.SOUL, x, y, z, 0.0, 0.02, 0.0);
		}
	}
}

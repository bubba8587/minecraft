// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Breeze hit on fireballs and other powered projectiles. */
@Mixin(AbstractHurtingProjectile.class)
abstract class BreezeVeerFireballMixin {
	@Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/Level;)V",
			at = @At("RETURN"))
	private void bubbas$veer(net.minecraft.world.entity.EntityType<?> type, LivingEntity mob, Vec3 direction, Level level, CallbackInfo ci) {
		double m = GemActions.veer(mob);
		if (m <= 1.0) {
			return;
		}
		AbstractHurtingProjectile self = (AbstractHurtingProjectile) (Object) this;
		Vec3 v = self.getDeltaMovement();
		double speed = v.length();
		if (speed < 1.0E-7) {
			return;
		}
		var random = self.getRandom();
		Vec3 dir = v.normalize().add(random.triangle(0.0, 0.0172275 * m), random.triangle(0.0, 0.0172275 * m), random.triangle(0.0, 0.0172275 * m));
		self.setDeltaMovement(dir.normalize().scale(speed));
	}
}

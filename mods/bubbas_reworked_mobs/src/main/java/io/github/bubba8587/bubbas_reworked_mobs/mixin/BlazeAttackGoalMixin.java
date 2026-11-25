// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireBlaze;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireCharge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Soulfire Blazes shoot Soulfire Charges (specs/wither-captain.md). */
@Mixin(targets = "net.minecraft.world.entity.monster.Blaze$BlazeAttackGoal")
abstract class BlazeAttackGoalMixin {
	@WrapOperation(method = "tick", at = @At(value = "NEW",
			target = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/projectile/hurtingprojectile/SmallFireball;"))
	private SmallFireball brm$soulfireCharge(Level level, LivingEntity mob, Vec3 direction, Operation<SmallFireball> original) {
		return mob instanceof SoulfireBlaze ? SoulfireCharge.shoot(level, mob, direction) : original.call(level, mob, direction);
	}
}

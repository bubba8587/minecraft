// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_extra_recipes.glider.GliderHandling;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Weaker rocket boost on the Glider. */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketGliderMixin {
	@Shadow
	private @Nullable LivingEntity attachedToEntity;

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
	private Vec3 bubbas_extra_recipes$weakerGliderBoost(Vec3 movement, double x, double y, double z, Operation<Vec3> original) {
		LivingEntity rider = this.attachedToEntity;
		if (rider != null && GliderHandling.isWearingGlider(rider)) {
			Vec3 boost = GliderHandling.rocketBoost(rider.getLookAngle(), movement, GliderHandling.GLIDER_ROCKET_POWER);
			return original.call(movement, boost.x, boost.y, boost.z);
		}
		return original.call(movement, x, y, z);
	}
}

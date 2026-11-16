// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/**
 * A grabbed victim rides the drowned, and vanilla's crosshair pick skips your own vehicle. Someone a drowned holds
 * must be able to hit it (the grip-break meter), so a drowned is never "the same vehicle" for the pick. Used by the
 * client's crosshair (LocalPlayer) and the debug renderer only.
 */
@Mixin(net.minecraft.world.entity.projectile.ProjectileUtil.class)
abstract class ProjectileUtilGripMixin {
	@WrapOperation(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getRootVehicle()Lnet/minecraft/world/entity/Entity;", ordinal = 0))
	private static Entity brm$drownedIsNotYourRide(Entity entity, Operation<Entity> original, Entity except, Vec3 from, Vec3 to, AABB box, Predicate<Entity> matching, double maxValue) {
		if (entity instanceof Drowned && except.getVehicle() == entity) {
			// Compared with ==, only against except's root vehicle: null never matches it.
			return null;
		}
		return original.call(entity);
	}
}

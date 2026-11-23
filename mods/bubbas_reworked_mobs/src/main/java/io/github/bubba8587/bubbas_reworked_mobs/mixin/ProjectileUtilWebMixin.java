// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Spun webs in vanilla's entity picks: hit tests on the strands, not the web's box (specs/spiders.md). */
@Mixin(ProjectileUtil.class)
abstract class ProjectileUtilWebMixin {
	private static final String VIEW_PICK = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;";
	private static final String MOVE_PICK = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;";
	private static final String MANY_PICK = "getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;FLnet/minecraft/world/level/ClipContext$Block;ZZ)Ljava/util/Collection;";
	private static final String CLIP = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;";
	private static final String CONTAINS = "Lnet/minecraft/world/phys/AABB;contains(Lnet/minecraft/world/phys/Vec3;)Z";

	// strand clip
	@WrapOperation(method = {VIEW_PICK, MOVE_PICK}, at = @At(value = "INVOKE", target = CLIP))
	private static Optional<Vec3> brm$clipStrands(AABB box, Vec3 from, Vec3 to, Operation<Optional<Vec3>> original, @Local(name = "entity") Entity entity) {
		return entity instanceof SpunWeb web ? web.strandClip(from, to) : original.call(box, from, to);
	}

	@WrapOperation(method = MANY_PICK, at = @At(value = "INVOKE", target = CLIP, ordinal = 0))
	private static Optional<Vec3> brm$clipStrandsMany(AABB box, Vec3 from, Vec3 to, Operation<Optional<Vec3>> original, @Local(name = "entity") Entity entity) {
		return entity instanceof SpunWeb web ? web.strandClip(from, to) : original.call(box, from, to);
	}

	// no margin hits on the box
	@WrapOperation(method = MANY_PICK, at = @At(value = "INVOKE", target = CLIP, ordinal = 1))
	private static Optional<Vec3> brm$noMarginMany(AABB box, Vec3 from, Vec3 to, Operation<Optional<Vec3>> original, @Local(name = "entity") Entity entity) {
		return entity instanceof SpunWeb ? Optional.empty() : original.call(box, from, to);
	}

	// never inside the box
	@WrapOperation(method = {VIEW_PICK, MANY_PICK}, at = @At(value = "INVOKE", target = CONTAINS))
	private static boolean brm$notInside(AABB box, Vec3 point, Operation<Boolean> original, @Local(name = "entity") Entity entity) {
		return !(entity instanceof SpunWeb) && original.call(box, point);
	}
}

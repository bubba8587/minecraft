// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Weaving gems (cobwebs slow less), Breeze gems (deflect projectiles), and Oozing slimes allied to their owner. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@ModifyVariable(method = "makeStuckInBlock", at = @At("HEAD"), argsOnly = true)
	private Vec3 bubbas$gemWeaving(Vec3 multiplier, BlockState state) {
		return (Object) this instanceof LivingEntity entity && state.is(Blocks.COBWEB) ? GemActions.webSlowdown(entity, multiplier) : multiplier;
	}

	@ModifyReturnValue(method = "deflection", at = @At("RETURN"))
	private ProjectileDeflection bubbas$gemBreeze(ProjectileDeflection vanilla, Projectile projectile) {
		return (Object) this instanceof LivingEntity entity ? GemActions.deflection(entity, projectile, vanilla) : vanilla;
	}

	@ModifyReturnValue(method = "considersEntityAsAlly", at = @At("RETURN"))
	private boolean bubbas$slimeOwner(boolean ally, Entity other) {
		return ally || GemActions.ownedBy((Entity) (Object) this, other);
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import io.github.bubba8587.bubbas_infused_gems.test.ProbeState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts how often each projectile reaches the entity hit-or-deflect step against a watched entity (Breeze measurement). */
@Mixin(Projectile.class)
public abstract class ProjectileProbe {
	@Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"))
	private void bubbas$probe(HitResult hit, CallbackInfoReturnable<?> cir) {
		if (ProbeState.watched != null && hit instanceof EntityHitResult e && e.getEntity() == ProbeState.watched) {
			ProbeState.CALLS.merge(((Entity) (Object) this).getId(), 1, Integer::sum);
		}
	}
}

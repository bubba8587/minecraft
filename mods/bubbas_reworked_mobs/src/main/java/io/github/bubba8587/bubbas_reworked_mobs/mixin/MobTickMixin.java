// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.warding.Wards;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wards: after a mob has moved this tick, hold it at a ward's edge (warding/WardBarrier). Server side only. */
@Mixin(LivingEntity.class)
abstract class MobTickMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void brm$wardEdge(CallbackInfo ci) {
		if ((Object) this instanceof Mob mob && !mob.level().isClientSide()) {
			Wards.afterMobTick(mob);
		}
	}
}

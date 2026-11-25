// dte:C1,B17,B15
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soulfire burning (soulfire/SoulBurning): the burn damage and the per-tick rules. */
@Mixin(Entity.class)
abstract class EntitySoulfireMixin {
	@WrapOperation(method = "baseTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean brm$soulBurn(Entity entity, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
		return SoulBurning.isSoulBurning(entity) ? SoulBurning.hurtByBurn(level, entity) : original.call(entity, level, source, amount);
	}

	@Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSharedFlagOnFire(Z)V"))
	private void brm$soulTick(CallbackInfo ci) {
		Entity entity = (Entity) (Object) this;
		if (entity.level() instanceof ServerLevel level) {
			SoulBurning.tick(level, entity);
		}
	}
}

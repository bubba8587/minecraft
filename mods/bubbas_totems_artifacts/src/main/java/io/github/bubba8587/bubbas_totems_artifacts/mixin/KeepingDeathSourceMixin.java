// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.keeping.Keeping;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem of Keeping (keeping/Keeping): the killing damage, for the drops it decides on. */
@Mixin(LivingEntity.class)
abstract class KeepingDeathSourceMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
	private void bta$dyingFrom(ServerLevel level, DamageSource source, CallbackInfo ci) {
		Keeping.DYING_FROM.set(source);
	}

	@Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
	private void bta$doneDying(ServerLevel level, DamageSource source, CallbackInfo ci) {
		Keeping.DYING_FROM.remove();
	}
}

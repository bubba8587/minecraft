// dte:C1,B17,B15
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Soulfire death (#bubbas_reworked_mobs:bypasses_totems) isn't saved by a Totem of Undying, unless the victim has Soulguard. */
@Mixin(LivingEntity.class)
abstract class TotemBypassMixin {
	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void brm$soulfireBypassesTotems(DamageSource killingDamage, CallbackInfoReturnable<Boolean> cir) {
		if (killingDamage.is(Soulfire.BYPASSES_TOTEMS) && !SoulBurning.hasSoulguard((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}

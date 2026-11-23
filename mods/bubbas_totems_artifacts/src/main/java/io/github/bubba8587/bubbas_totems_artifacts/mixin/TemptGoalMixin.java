// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.herding.Herding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Totem of Herding, held. */
@Mixin(TemptGoal.class)
abstract class TemptGoalMixin {
	@Shadow
	@Final
	protected Mob mob;

	@Inject(method = "shouldFollow", at = @At("HEAD"), cancellable = true)
	private void bta$herdingTotem(LivingEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (Herding.isLivestock(this.mob) && Herding.holds(player)) {
			cir.setReturnValue(true);
		}
	}
}

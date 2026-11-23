// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.herding.Herding;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Totem of Herding, held. */
@Mixin(TemptingSensor.class)
abstract class TemptingSensorMixin {
	@Inject(method = "playerHoldingTemptation", at = @At("HEAD"), cancellable = true)
	private void bta$herdingTotem(PathfinderMob mob, Player player, CallbackInfoReturnable<Boolean> cir) {
		if (Herding.isLivestock(mob) && Herding.holds(player)) {
			cir.setReturnValue(true);
		}
	}
}

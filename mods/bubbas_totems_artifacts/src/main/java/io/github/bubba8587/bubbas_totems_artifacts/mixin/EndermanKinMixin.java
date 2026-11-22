// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.end.TotemOfTheEndItem;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player holding a Totem of the End is one of the endermen's own: looking one in the eye doesn't count as staring
 * (vanilla's carved-pumpkin disguise check, for the hand instead of the head). Every enderman, vanilla or Reworked
 * Mobs' caretakers.
 */
@Mixin(Enderman.class)
public abstract class EndermanKinMixin {
	@Inject(method = "isBeingStaredBy", at = @At("HEAD"), cancellable = true)
	private void bta$kinIsNotStaring(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (TotemOfTheEndItem.held(player)) {
			cir.setReturnValue(false);
		}
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A grabbed player rides the drowned holding them (specs/drowned.md "The grab"); sneaking doesn't get them off. Only
 * the grip-break meter, Riptide, the drowned's death, or leaving some other way (ender pearl, commands) does. Nothing
 * else rides a drowned, so any drowned vehicle is a grip.
 */
@Mixin(Player.class)
abstract class PlayerGripMixin {
	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void brm$heldByDrowned(CallbackInfoReturnable<Boolean> cir) {
		if (((Player) (Object) this).getVehicle() instanceof Drowned) {
			cir.setReturnValue(false);
		}
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Saturation gem: food drains slower (exhaustion x (1 - s)), so eating stays part of the game. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float bubbas$gemSaturation(float amount) {
		return WornEffects.exhaustion((Player) (Object) this, amount);
	}
}

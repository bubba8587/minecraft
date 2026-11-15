// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_infused_gems.client.GemLooks;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gem Poison and Wither show vanilla's poisoned and withered hearts (owner). */
@Mixin(targets = "net.minecraft.client.gui.Hud$HeartType")
abstract class GemLooksHeartMixin {
	@WrapOperation(method = "forPlayer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"))
	private static boolean bubbas$gemCounts(Player player, Holder<MobEffect> effect, Operation<Boolean> original) {
		return original.call(player, effect) || GemLooks.has(player, effect);
	}
}

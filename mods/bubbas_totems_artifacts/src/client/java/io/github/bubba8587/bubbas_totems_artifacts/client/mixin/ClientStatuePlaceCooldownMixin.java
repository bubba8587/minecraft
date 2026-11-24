// dte:C6,B19
package io.github.bubba8587.bubbas_totems_artifacts.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Statue totems can be set down on a block while their item cooldown runs (client side). */
@Mixin(MultiPlayerGameMode.class)
abstract class ClientStatuePlaceCooldownMixin {
	@WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;isOnCooldown(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean bta$placeStatueOnCooldown(ItemCooldowns cooldowns, ItemStack stack, Operation<Boolean> original) {
		return !(stack.getItem() instanceof StatueTotemItem) && original.call(cooldowns, stack);
	}
}
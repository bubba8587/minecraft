// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;

/** Perfect Invisibility hidden armor. */
@Mixin(ResultContainer.class)
abstract class HiddenArmorResultMixin {
	@ModifyVariable(method = "setItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack bubbas$clean(ItemStack stack) {
		return HiddenArmor.isHidden(stack) ? HiddenArmor.restore(stack.copy()) : stack;
	}
}

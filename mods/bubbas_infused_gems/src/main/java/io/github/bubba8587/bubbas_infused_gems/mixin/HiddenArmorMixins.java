// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Perfect Invisibility hidden armor: a marked stack restores itself on its tick unless it is still worn and hidden. */
@Mixin(ItemStack.class)
abstract class HiddenArmorMixins {
	@Inject(method = "inventoryTick", at = @At("HEAD"))
	private void bubbas$selfHeal(Level level, Entity owner, @Nullable EquipmentSlot slot, CallbackInfo ci) {
		HiddenArmor.tick((ItemStack) (Object) this, owner, slot);
	}
}

// dte:C4,B19
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.AbsorptionCooldown;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Armor with an Absorption gem shares one cooldown group, so the shield's refill wait shows as the vanilla sweep. */
@Mixin(ItemCooldowns.class)
abstract class ItemCooldownsMixin {
	@Inject(method = "getCooldownGroup", at = @At("HEAD"), cancellable = true)
	private void bubbas$absorptionGroup(ItemStack item, CallbackInfoReturnable<Identifier> cir) {
		if (AbsorptionCooldown.carriesAbsorption(item)) {
			cir.setReturnValue(AbsorptionCooldown.GROUP);
		}
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gem mob effects name their strength as a percent of the strongest vanilla level ("Poison 46%"), not a level. */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@ModifyReturnValue(method = "getEffectName", at = @At("RETURN"))
	private Component bubbas$percent(Component name, MobEffectInstance effect) {
		if (effect.getEffect().value() instanceof GemMobEffect) {
			return effect.getEffect().value().getDisplayName().copy()
					.append(" " + Math.round(GemMobEffect.potency(effect.getAmplifier()) * 100) + "%");
		}
		return name;
	}
}

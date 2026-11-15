// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gem mob effects show vanilla's icon for their effect (no copied art). */
@Mixin(Hud.class)
public abstract class HudMixin {
	@ModifyReturnValue(method = "getMobEffectSprite", at = @At("RETURN"))
	private static Identifier bubbas$vanillaIcon(Identifier sprite, Holder<MobEffect> effect) {
		// Turtle Master has no vanilla effect icon: it shows Resistance's, its main part.
		return effect.value() instanceof GemMobEffect gem
				? Identifier.withDefaultNamespace("mob_effect/" + (gem.key().equals("turtle_master") ? "resistance" : gem.key())) : sprite;
	}
}

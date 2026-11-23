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

/** Gem mob effects show vanilla's icon for their effect. */
@Mixin(Hud.class)
public abstract class HudMixin {
	@ModifyReturnValue(method = "getMobEffectSprite", at = @At("RETURN"))
	private static Identifier bubbas$vanillaIcon(Identifier sprite, Holder<MobEffect> effect) {
		return effect.value() instanceof GemMobEffect gem
				? Identifier.withDefaultNamespace("mob_effect/" + switch (gem.key()) {
					case "turtle_master" -> "resistance";
					case "silence" -> "darkness";
					case "ender" -> "slow_falling";
					case "breeze" -> "wind_charged";
					default -> gem.key();
				}) : sprite;
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hero of the Village gem: the hero price cut beside vanilla's (the stronger applies); gifts stay vanilla-only. */
@Mixin(Villager.class)
public abstract class VillagerMixin {
	@ModifyVariable(method = "updateSpecialPrices", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
	private double bubbas$gemHero(double heroModifier, Player player) {
		return WornEffects.heroModifier(player, heroModifier);
	}
}

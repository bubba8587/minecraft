// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hero of the Village gem: the price cut beside vanilla's, the stronger applies; gifts stay vanilla-only. When the gem's
 * cut is the stronger, vanilla's is switched off and the gem's applied without vanilla's "at least 1 emerald off" floor,
 * so a weak gem can round to nothing off a cheap trade.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {
	@ModifyVariable(method = "updateSpecialPrices", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
	private double bubbas$gemHero(double heroModifier, Player player) {
		return WornEffects.heroModifier(player, heroModifier) > heroModifier ? 0.0 : heroModifier;
	}

	@Inject(method = "updateSpecialPrices", at = @At("TAIL"))
	private void bubbas$gemHeroCut(Player player, CallbackInfo ci) {
		MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
		double vanilla = hero == null ? 0.0 : 0.3F + 0.0625F * hero.getAmplifier();
		double gem = WornEffects.heroModifier(player, 0.0);
		if (gem > vanilla) {
			for (MerchantOffer offer : ((Villager) (Object) this).getOffers()) {
				int cut = (int) Math.floor(gem * offer.getBaseCostA().getCount());
				if (cut > 0) {
					offer.addToSpecialPriceDiff(-cut);
				}
			}
		}
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import io.github.bubba8587.bubbas_infused_gems.station.GemInversion;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Milestone: a finished gem's name reads back its Infusion and Enchant for every effect, potion, Base Power and enchant, and after inverting. */
public class GemNameGameTest {
	@GameTest
	public void gemNameReadsBackItsMaking(GameTestHelper helper) {
		Balance b = Balance.get();
		int checked = 0;
		for (String key : b.gemEffects) {
			Balance.EffectData e = b.effect(key);
			for (var variant : e.variants().keySet()) {
				int infusionWant = !"potion".equals(e.source()) ? 3 : switch (variant) {
					case "long" -> 2;
					case "normal" -> 1;
					default -> 3;
				};
				for (int power = 1; power <= 6; power++) {
					for (int tier = 1; tier <= 3; tier++) {
						for (int seed = 0; seed < 4; seed++) {
							Gem gem = GemEnchanting.roll(seed, tier, new InfusedGem(GemText.holder(key), variant, "potion".equals(e.source()) ? GemForm.LINGERING : GemForm.ITEM, power));
							if (!gem.hasMain()) {
								continue;
							}
							helper.assertTrue(gem.enchantLevel() == tier || closeVariants(e),
									key + " " + variant + " power " + power + " tier " + tier + ": enchant reads " + gem.enchantLevel());
							helper.assertTrue(gem.infusionLevel() == infusionWant || closeVariants(e),
									key + " " + variant + " power " + power + " tier " + tier + ": infusion reads " + gem.infusionLevel());
							checked++;
							if (GemInversion.canInvert(gem)) {
								Gem inverted = GemInversion.invert(gem);
								if (inverted.hasMain()) {
									helper.assertTrue(inverted.enchantLevel() <= Math.max(1, tier - 1) || closeVariants(b.effect(inverted.key())),
											key + " inverted from tier " + tier + ": enchant reads " + inverted.enchantLevel());
								}
							}
						}
					}
				}
			}
		}
		helper.assertTrue(checked > 100, "too few gems checked: " + checked);
		helper.succeed();
	}

	/** Two variants of one effect so close that its strength can't tell them apart. */
	private static boolean closeVariants(Balance.EffectData e) {
		var factors = e.variants().values().stream().map(Balance.Variant::factor).sorted().toList();
		for (int i = 1; i < factors.size(); i++) {
			if (factors.get(i) - factors.get(i - 1) < 0.03) {
				return true;
			}
		}
		return false;
	}
}

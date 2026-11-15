// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;
import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.close;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import io.github.bubba8587.bubbas_infused_gems.station.GemInversion;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Gem inversion (owner, 2026-11-15): a gem with a drawback + a fermented spider eye at a real brewing stand swaps main and
 * drawback, one grade and one Infusion level lower; the bonus is kept; it inverts back; gems that can't invert are refused.
 * Also the rarity-weighted rolls.
 */
public class GemInversionGameTest {
	/** A Speed gem rolled strongest at Infusion VI from a strong potion (1.0), with a Strength bonus and a Hunger drawback. */
	private static Gem speedWithHunger(int infusion, String grade) {
		Balance b = Balance.get();
		double main = b.infusion(infusion).factor() * b.grades.get(grade);
		return new Gem(GemText.holder("speed"), infusion, 3, (float) main,
				Optional.of(new Gem.Roll(GemText.holder("strength"), 0.5F)), Optional.of(new Gem.Roll(GemText.holder("hunger"), (float) b.drawbackOne)));
	}

	@GameTest(maxTicks = 900)
	public void brewingStandInvertsAndRevertsGems(GameTestHelper helper) {
		Balance b = Balance.get();
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.BREWING_STAND);
		BrewingStandBlockEntity stand = helper.getBlockEntity(pos, BrewingStandBlockEntity.class);
		var access = helper.getLevel().recipeAccess();
		Gem gem = speedWithHunger(6, "strongest");
		check(helper, PotionIngredient.isPotionInput(GemKit.item(gem), access), "the bottle slot refuses an invertible gem");
		// Refused: no drawback, a weak grade, Infusion I.
		check(helper, !PotionIngredient.isPotionInput(GemKit.item(GemKit.gem("speed", 1.0)), access), "a gem without a drawback goes in");
		check(helper, !PotionIngredient.isPotionInput(GemKit.item(speedWithHunger(6, "weak")), access), "a weak-grade gem goes in");
		check(helper, !PotionIngredient.isPotionInput(GemKit.item(speedWithHunger(1, "strongest")), access), "an Infusion I gem goes in");
		stand.setItem(0, GemKit.item(gem));
		stand.setItem(3, new ItemStack(Items.FERMENTED_SPIDER_EYE, 2));
		stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
		Gem expected = GemInversion.invert(gem);
		helper.succeedWhen(() -> {
			Gem out = stand.getItem(0).get(ModComponents.GEM);
			check(helper, out != null && !out.equals(gem), "not inverted yet");
			check(helper, GemText.key(out.effect()).equals("hunger"), "new main " + GemText.key(out.effect()));
			check(helper, out.drawback().map(r -> r.key().equals("speed") && Math.abs(r.strength() - b.drawbackOne) < 1e-6).orElse(false),
					"old main is not the drawback at the old drawback's size: " + out.drawback());
			close(out.main(), b.infusion(5).factor() * b.grades.get("medium"), 1e-5, helper, "one grade and one Infusion lower");
			check(helper, out.infusion() == 5 && out.tier() == 3, "Infusion " + out.infusion());
			check(helper, out.other().equals(gem.other()), "the bonus changed");
			check(helper, out.equals(expected), "stand result differs from the model");
			// Inverting back: Speed again, medium -> weak, Infusion IV.
			Gem back = GemInversion.invert(out);
			check(helper, GemText.key(back.effect()).equals("speed") && back.infusion() == 4, "reverted " + back);
			close(back.main(), b.infusion(4).factor() * b.grades.get("weak"), 1e-5, helper, "reverted strength");
			check(helper, back.other().equals(gem.other()), "the bonus changed on reverting");
			check(helper, !GemInversion.canInvert(back), "a weak-grade gem could invert again");
			check(helper, out.main() < 1.0F, "an inverted gem is perfect");
		});
	}

	/** Owner: rolls weighted by rarity, common 4, uncommon 2, rare (found-only) 1, both bonus and drawback pools. */
	@GameTest
	public void rollsFavourCommonEffects(GameTestHelper helper) {
		Balance b = Balance.get();
		InfusedGem infused = GemKit.infused("speed", "strong", GemForm.LINGERING, 6).get(ModComponents.INFUSED_GEM);
		Map<String, Integer> others = new HashMap<>();
		Map<String, Integer> drawbacks = new HashMap<>();
		for (int seed = 0; seed < 20000; seed++) {
			Gem g = GemEnchanting.roll(seed, 3, infused);
			g.other().ifPresent(r -> others.merge(r.key(), 1, Integer::sum));
			g.drawback().ifPresent(r -> drawbacks.merge(r.key(), 1, Integer::sum));
		}
		Balance.EffectData e = b.effect("speed");
		// Common (Strength) vs rare (Haste) in the bonus pool: about 4 to 1.
		double ratio = others.getOrDefault("strength", 0) / (double) Math.max(1, others.getOrDefault("haste", 0));
		check(helper, ratio > 2.8 && ratio < 5.6, "bonus Strength/Haste " + ratio + " (" + others + ")");
		check(helper, e.otherWeights().size() == e.otherPool().size() && e.drawbackWeights().size() == e.drawbackPool().size(), "weights missing");
		// Common (Weakness) vs uncommon (Poison) in the drawback pool: about 2 to 1.
		double d = drawbacks.getOrDefault("weakness", 0) / (double) Math.max(1, drawbacks.getOrDefault("poison", 0));
		check(helper, d > 1.4 && d < 2.8, "drawback Weakness/Poison " + d + " (" + drawbacks + ")");
		helper.succeed();
	}
}

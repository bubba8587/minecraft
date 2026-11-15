// dte:C4
package io.github.bubba8587.bubbas_infused_gems.station;

import java.util.List;
import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Enchanting an Infused Gem (owner): the three rows are the three tiers, each locking in one outcome by chance with the
 * model's odds. Costs follow vanilla's bookshelf formula (EnchantmentHelper.getEnchantmentCost), the outcome comes
 * from the player's enchantment seed (so the preview and vanilla's reroll trick behave as for gear), and rows above
 * the gem's form are off (normal: tier 1, splash: up to 2, lingering: all three).
 */
public final class GemEnchanting {
	private GemEnchanting() {
	}

	public static boolean isInfusedGem(ItemStack stack) {
		return stack.is(ModItems.INFUSED_GEM) && stack.has(ModComponents.INFUSED_GEM);
	}

	/** Vanilla's three level costs for this many bookshelves (random seeded from the enchantment seed), capped by the form. */
	public static int[] costs(int seed, int bookshelves, InfusedGem gem) {
		RandomSource random = RandomSource.create(seed);
		int shelves = Math.min(bookshelves, 15);
		int[] costs = new int[3];
		for (int slot = 0; slot < 3; slot++) {
			int selected = random.nextInt(8) + 1 + (shelves >> 1) + random.nextInt(shelves + 1);
			int cost = slot == 0 ? Math.max(selected / 3, 1) : slot == 1 ? selected * 2 / 3 + 1 : Math.max(selected, shelves * 2);
			// Off: below vanilla's minimum, above the gem's form, or without the tier's bookshelves (owner: tier 2 needs 8, tier 3 all 15).
			boolean gated = bookshelves < Balance.get().tierMinShelves.getOrDefault(slot + 1, 0);
			costs[slot] = cost < slot + 1 || slot + 1 > gem.maxTier() || gated ? 0 : cost;
		}
		return costs;
	}

	/** The outcome of enchanting this gem at this tier (1-3) with this seed: always the same for the same inputs. */
	public static Gem roll(int seed, int tier, InfusedGem infused) {
		Balance b = Balance.get();
		RandomSource random = random(seed, tier, infused);
		Balance.Outcome outcome = pick(random, tier);
		Balance.EffectData e = b.effect(infused.key());
		double infusion = b.infusion(infused.infusion()).factor();
		float main = outcome.main() == null ? 0.0F : (float) (infused.baseQuality() * b.grades.get(outcome.main()));
		Optional<Gem.Roll> other = Optional.empty();
		if (outcome.other() != null && e != null && !e.otherPool().isEmpty()) {
			String key = weighted(random, e.otherPool(), e.otherWeights());
			other = Optional.of(new Gem.Roll(GemText.holder(key), (float) (infusion * b.grades.get(outcome.other()))));
		}
		Optional<Gem.Roll> drawback = Optional.empty();
		if (outcome.drawback() != null && e != null && !e.drawbackPool().isEmpty()) {
			String key = weighted(random, e.drawbackPool(), e.drawbackWeights());
			drawback = Optional.of(new Gem.Roll(GemText.holder(key), (float) b.drawbackSize(outcome.drawback())));
		}
		return new Gem(infused.effect(), infused.infusion(), tier, main, other, drawback);
	}

	private static RandomSource random(int seed, int tier, InfusedGem infused) {
		return RandomSource.create(seed * 31L + tier * 7919L + (infused.key() + "|" + infused.variant() + "|" + infused.infusion()).hashCode());
	}

	private static Balance.Outcome pick(RandomSource random, int tier) {
		List<Balance.Outcome> outcomes = Balance.get().tiers.get(tier);
		double r = random.nextDouble();
		for (Balance.Outcome o : outcomes) {
			if (r < o.odds()) {
				return o;
			}
			r -= o.odds();
		}
		return outcomes.getLast();
	}

	/** Which outcome this seed gives this tier: the same draw {@link #roll} makes (the preview's kind and size). */
	public static Balance.Outcome outcome(int seed, int tier, InfusedGem infused) {
		return pick(random(seed, tier, infused), tier);
	}

	/** One of the pool, by weight (owner: Poison and Wither drawbacks roll half as often; weights from the model). */
	public static String weighted(RandomSource random, List<String> pool, List<Double> weights) {
		double total = 0.0;
		for (int i = 0; i < pool.size(); i++) {
			total += i < weights.size() ? weights.get(i) : 1.0;
		}
		double r = random.nextDouble() * total;
		for (int i = 0; i < pool.size(); i++) {
			r -= i < weights.size() ? weights.get(i) : 1.0;
			if (r < 0.0) {
				return pool.get(i);
			}
		}
		return pool.getLast();
	}

	public static ItemStack finished(Gem gem) {
		ItemStack stack = new ItemStack(ModItems.GEM);
		stack.set(ModComponents.GEM, gem);
		return stack;
	}

	/** The preview, like vanilla's clue: the outcome's main line, then "...?" (the rest stays a surprise). */
	/**
	 * The hover preview, "slightly anonymized" (owner): the main effect and its strength, then for an extra roll only its
	 * kind and size ("Speed 100% + minor bonus...?", "minor drawback only...?"). Exact, so the vanilla reroll trick works.
	 */
	public static MutableComponent clue(Gem gem, Balance.Outcome outcome) {
		MutableComponent extra = null;
		if (outcome.other() != null) {
			extra = Component.translatable(GemText.KEY + "clue.bonus", size(outcome.other()));
		} else if (outcome.drawback() != null) {
			extra = Component.translatable(GemText.KEY + "clue.drawback", size(outcome.drawback()));
		}
		if (!gem.hasMain()) {
			return Component.translatable(GemText.KEY + "clue.only", extra == null ? Component.empty() : extra).withStyle(ChatFormatting.WHITE);
		}
		MutableComponent main = GemText.effectName(gem.effect()).append(" " + GemText.percent(gem.main()));
		return (extra == null ? main : Component.translatable(GemText.KEY + "clue.extra", main, extra)).withStyle(ChatFormatting.WHITE);
	}

	/** Size words: weak = "minor", medium = none, strongest = "major". */
	private static Component size(String grade) {
		return Component.translatable(GemText.KEY + "clue.size." + grade);
	}
}

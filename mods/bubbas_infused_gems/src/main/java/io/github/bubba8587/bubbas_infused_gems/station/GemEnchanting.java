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
			costs[slot] = cost < slot + 1 || slot + 1 > gem.maxTier() ? 0 : cost;
		}
		return costs;
	}

	/** The outcome of enchanting this gem at this tier (1-3) with this seed: always the same for the same inputs. */
	public static Gem roll(int seed, int tier, InfusedGem infused) {
		Balance b = Balance.get();
		RandomSource random = RandomSource.create(seed * 31L + tier * 7919L + (infused.key() + "|" + infused.variant() + "|" + infused.infusion()).hashCode());
		List<Balance.Outcome> outcomes = b.tiers.get(tier);
		double r = random.nextDouble();
		Balance.Outcome outcome = outcomes.getLast();
		for (Balance.Outcome o : outcomes) {
			if (r < o.odds()) {
				outcome = o;
				break;
			}
			r -= o.odds();
		}
		Balance.EffectData e = b.effect(infused.key());
		double infusion = b.infusion(infused.infusion()).factor();
		float main = outcome.main() == null ? 0.0F : (float) (infused.baseQuality() * b.grades.get(outcome.main()));
		Optional<Gem.Roll> other = Optional.empty();
		if (outcome.other() != null && e != null && !e.v0OtherPool().isEmpty()) {
			String key = e.v0OtherPool().get(random.nextInt(e.v0OtherPool().size()));
			other = Optional.of(new Gem.Roll(GemText.holder(key), (float) (infusion * b.grades.get(outcome.other()))));
		}
		Optional<Gem.Roll> drawback = Optional.empty();
		if (outcome.drawback() && e != null && !e.v0DrawbackPool().isEmpty()) {
			String key = e.v0DrawbackPool().get(random.nextInt(e.v0DrawbackPool().size()));
			drawback = Optional.of(new Gem.Roll(GemText.holder(key), (float) b.drawbackOne));
		}
		return new Gem(infused.effect(), infused.infusion(), tier, main, other, drawback);
	}

	public static ItemStack finished(Gem gem) {
		ItemStack stack = new ItemStack(ModItems.GEM);
		stack.set(ModComponents.GEM, gem);
		return stack;
	}

	/** The preview, like vanilla's clue: the outcome's main line, then "...?" (the rest stays a surprise). */
	public static MutableComponent clue(Gem gem) {
		MutableComponent text = gem.hasMain()
				? GemText.effectName(gem.effect()).append(" " + GemText.percent(gem.main()))
				: Component.translatable(GemText.KEY + "tooltip.drawback", Component.literal("?"));
		return Component.translatable("container.enchant.clue", text).withStyle(ChatFormatting.WHITE);
	}
}

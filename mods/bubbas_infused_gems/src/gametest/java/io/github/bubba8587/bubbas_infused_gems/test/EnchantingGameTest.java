// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;
import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.close;

import java.util.HashMap;
import java.util.Map;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Enchanting an Infused Gem: rows capped by form (normal 1, splash 2, lingering 3) with vanilla's bookshelf costs; a
 * fixed seed always gives the same gem; outcome odds follow the model; "other" rolls are the same kind and never the
 * main effect; drawbacks are the opposite kind and never the main effect's direct opposite; and the real enchanting
 * table turns the gem into a finished one.
 */
public class EnchantingGameTest {
	private static InfusedGem infused(String key, GemForm form) {
		return GemKit.infused(key, "strong", form, 6).get(ModComponents.INFUSED_GEM);
	}

	@GameTest
	public void formCapsTheTiers(GameTestHelper helper) {
		for (int seed = 0; seed < 50; seed++) {
			int[] normal = GemEnchanting.costs(seed, 15, infused("speed", GemForm.NORMAL));
			int[] splash = GemEnchanting.costs(seed, 15, infused("speed", GemForm.SPLASH));
			int[] lingering = GemEnchanting.costs(seed, 15, infused("speed", GemForm.LINGERING));
			check(helper, normal[0] > 0 && normal[1] == 0 && normal[2] == 0, "normal form rows " + java.util.Arrays.toString(normal));
			check(helper, splash[0] > 0 && splash[1] > 0 && splash[2] == 0, "splash form rows " + java.util.Arrays.toString(splash));
			check(helper, lingering[0] > 0 && lingering[1] > 0 && lingering[2] > 0, "lingering form rows " + java.util.Arrays.toString(lingering));
			// Vanilla's bookshelf rule: with 15 shelves the bottom row costs 30.
			check(helper, lingering[2] == 30, "15 bookshelves: bottom row " + lingering[2]);
			check(helper, GemEnchanting.costs(seed, 0, infused("speed", GemForm.LINGERING))[2] <= 8, "no bookshelves: cheap bottom row");
		}
		helper.succeed();
	}

	@GameTest
	public void fixedSeedRollsFollowTheModel(GameTestHelper helper) {
		Balance b = Balance.get();
		InfusedGem gem = infused("speed", GemForm.LINGERING);
		check(helper, GemEnchanting.roll(1234, 3, gem).equals(GemEnchanting.roll(1234, 3, gem)), "same seed, different gem");
		for (int tier = 1; tier <= 3; tier++) {
			Map<String, Integer> seen = new HashMap<>();
			int n = 8000;
			for (int seed = 0; seed < n; seed++) {
				Balance.Outcome o = GemEnchanting.outcome(seed, tier, gem);
				Gem g = GemEnchanting.roll(seed, tier, gem);
				check(helper, g.tier() == tier, "tier");
				// The roll is exactly what the preview's outcome says.
				check(helper, g.hasMain() == (o.main() != null) && g.other().isPresent() == (o.other() != null)
						&& g.drawback().isPresent() == (o.drawback() != null), "roll and preview disagree at seed " + seed + ": " + o.label());
				if (o.main() != null) {
					close(g.main(), gem.baseQuality() * b.grades.get(o.main()), 1e-5, helper, "main grade " + o.label());
				}
				g.drawback().ifPresent(r -> close(r.strength(), b.drawbackSize(o.drawback()), 1e-6, helper, "drawback size " + o.label()));
				seen.merge(o.label(), 1, Integer::sum);
			}
			for (Balance.Outcome o : b.tiers.get(tier)) {
				close(seen.getOrDefault(o.label(), 0) / (double) n, o.odds(), 0.02, helper, "tier " + tier + " " + o.label());
			}
			// Owner (Q34): the plain result is the most common at every tier.
			Balance.Outcome first = b.tiers.get(tier).getFirst();
			check(helper, first.other() == null && first.drawback() == null && first.main() != null, "tier " + tier + " does not start plain");
		}
		// The preview (owner): main effect and strength, then only the extra roll's kind and size.
		for (int seed = 0; seed < 400; seed++) {
			for (int tier = 1; tier <= 3; tier++) {
				Balance.Outcome o = GemEnchanting.outcome(seed, tier, gem);
				String clue = GemEnchanting.clue(GemEnchanting.roll(seed, tier, gem), o).getString();
				String size = o.other() != null ? o.other() : o.drawback();
				String word = size == null ? "" : switch (size) {
					case "weak" -> "minor ";
					case "strongest" -> "major ";
					default -> "";
				};
				String expected = o.main() == null ? word + "drawback only...?"
						: "Speed " + Math.round(gem.baseQuality() * b.grades.get(o.main()) * 100) + "%"
								+ (o.other() != null ? " + " + word + "bonus...?" : o.drawback() != null ? " + " + word + "drawback...?" : "");
				check(helper, clue.equals(expected), "preview '" + clue + "', expected '" + expected + "'");
			}
		}
		// Strength: lingering strong swiftness + nether star + tier 3 main = a perfect gem.
		Gem best = GemEnchanting.roll(7, 3, gem);
		close(best.main(), 1.0, 1e-6, helper, "perfect gem main");
		helper.succeed();
	}

	@GameTest
	public void rollsKeepTheirKindAndNeverTheDirectOpposite(GameTestHelper helper) {
		Balance b = Balance.get();
		for (String key : b.gemEffects) {
			Balance.EffectData e = b.effect(key);
			InfusedGem gem = GemKit.infused(key, e.variants().keySet().iterator().next(), GemForm.LINGERING, 4).get(ModComponents.INFUSED_GEM);
			for (int seed = 0; seed < 300; seed++) {
				for (int tier = 1; tier <= 3; tier++) {
					Gem g = GemEnchanting.roll(seed, tier, gem);
					g.other().ifPresent(r -> {
						check(helper, !r.key().equals(key), key + " rolled itself as other");
						check(helper, b.effect(r.key()).good() == e.good(), key + " other " + r.key() + " is the wrong kind");
					});
					g.drawback().ifPresent(r -> {
						check(helper, b.effect(r.key()).good() != e.good(), key + " drawback " + r.key() + " is the same kind");
						check(helper, !e.opposites().contains(r.key()), key + " drawback " + r.key() + " is its direct opposite");
					});
				}
			}
		}
		helper.succeed();
	}

	@GameTest
	public void enchantingTableMakesAFinishedGem(GameTestHelper helper) {
		BlockPos table = new BlockPos(1, 1, 1);
		helper.setBlock(table, Blocks.ENCHANTING_TABLE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false; // pays lapis and levels like survival
		player.setExperienceLevels(30);
		EnchantmentMenu menu = new EnchantmentMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(table)));
		menu.getSlot(0).set(GemKit.infused("strength", "strong", GemForm.SPLASH, 3));
		menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 10));
		check(helper, menu.costs[0] > 0 && menu.costs[1] > 0 && menu.costs[2] == 0, "splash gem rows " + java.util.Arrays.toString(menu.costs));
		check(helper, menu.enchantClue[0] == -1, "vanilla clue shown for a gem");
		check(helper, !menu.clickMenuButton(player, 2), "tier 3 allowed on a splash gem");
		int seed = menu.getEnchantmentSeed();
		Gem expected = GemEnchanting.roll(seed, 2, GemKit.infused("strength", "strong", GemForm.SPLASH, 3).get(ModComponents.INFUSED_GEM));
		check(helper, menu.clickMenuButton(player, 1), "tier 2 refused");
		ItemStack out = menu.getSlot(0).getItem();
		check(helper, out.is(ModItems.GEM), "no finished gem: " + out);
		check(helper, expected.equals(out.get(ModComponents.GEM)), "rolled " + out.get(ModComponents.GEM) + ", preview said " + expected);
		check(helper, menu.getSlot(1).getItem().getCount() == 8, "lapis not used (2)");
		check(helper, menu.costs[0] == 0, "a finished gem can be enchanted again");
		check(helper, player.experienceLevel == 28, "levels paid: " + player.experienceLevel);
		menu.removed(player);
		GemKit.remove(helper, player);
		helper.succeed();
	}
}

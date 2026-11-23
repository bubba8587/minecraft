// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.check;
import static io.github.bubba8587.bubbas_infused_gems.test.GemKit.close;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemTotals;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** The model's numbers reach the mod. */
public class BalanceGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};

	// Core test: registered by InfusedGemsCoreGameTest
	public void balanceJsonLoadsWithKnownValues(GameTestHelper helper) {
		Balance b = Balance.get();
		check(helper, b.cap > 0 && !b.gemEffects.isEmpty() && b.infusions.size() == 6, "balance.json did not load whole");
		check(helper, !b.armorMaterials.containsKey("turtle"), "the turtle helmet takes no gems (owner, question 3)");
		JsonObject checks = b.raw.getAsJsonObject("checks");
		List<JsonElement> totals = checks.getAsJsonArray("drawback_totals").asList();
		for (int n = 0; n < totals.size(); n++) {
			close(b.drawbackTotal(n), totals.get(n).getAsDouble(), 1e-6, helper, n + " drawbacks");
		}
		checks.getAsJsonObject("heal_fraction").entrySet().forEach(e ->
				close(b.healFraction(Double.parseDouble(e.getKey())), e.getValue().getAsDouble(), 1e-6, helper, "heal at " + e.getKey()));
		helper.succeed();
	}

	@GameTest
	public void modelBuildsMatchFromRealItems(GameTestHelper helper) {
		Balance b = Balance.get();
		for (JsonElement el : b.raw.getAsJsonObject("checks").getAsJsonArray("armor_builds")) {
			JsonObject build = el.getAsJsonObject();
			Gem gem = GemKit.modelGem(build.get("effect").getAsString(), build.get("variant").getAsString(),
					build.get("base").getAsString(), build.get("grade").getAsString());
			ItemStack[] pieces = new ItemStack[4];
			for (int i = 0; i < 4; i++) {
				JsonElement m = build.getAsJsonArray("layout").get(i);
				int n = build.getAsJsonArray("gems").get(i).getAsInt();
				if (m.isJsonNull()) {
					pieces[i] = ItemStack.EMPTY;
					continue;
				}
				Gem[] gems = new Gem[n];
				java.util.Arrays.fill(gems, gem);
				pieces[i] = GemKit.socketed(armor(m.getAsString(), PIECES[i]), gems);
			}
			double total = GemTotals.ofArmor(pieces).totals().getOrDefault(build.get("effect").getAsString(), 0.0);
			close(total, build.get("total").getAsDouble(), 1e-5, helper, "armor build " + build.get("name").getAsString());
		}
		for (JsonElement el : b.raw.getAsJsonObject("checks").getAsJsonArray("weapon_builds")) {
			JsonObject build = el.getAsJsonObject();
			Gem gem = GemKit.modelGem(build.get("effect").getAsString(), build.get("variant").getAsString(),
					build.get("base").getAsString(), build.get("grade").getAsString());
			List<Gem> gems = new ArrayList<>();
			for (int i = 0; i < build.get("gems").getAsInt(); i++) {
				gems.add(gem);
			}
			ItemStack weapon = GemKit.socketed(weapon(build.get("weapon").getAsString()), gems.toArray(Gem[]::new));
			double total = GemTotals.ofWeapon(weapon).totals().getOrDefault(build.get("effect").getAsString(), 0.0);
			close(total, build.get("total").getAsDouble(), 1e-5, helper, "weapon build " + build.get("name").getAsString());
		}
		helper.succeed();
	}

	static Item armor(String material, String piece) {
		String prefix = material.equals("gold") ? "golden" : material;
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(prefix + "_" + piece));
	}

	static Item weapon(String material) {
		return switch (material) {
			case "bow", "crossbow", "trident", "mace" -> BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(material));
			case "wood" -> BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("wooden_sword"));
			case "gold" -> BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("golden_sword"));
			default -> BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(material + "_sword"));
		};
	}
}

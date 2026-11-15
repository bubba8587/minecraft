// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Builders for the gem tests. */
public final class GemKit {
	private GemKit() {
	}

	public static Gem gem(String key, double main) {
		return new Gem(GemText.holder(key), 6, 3, (float) main, Optional.empty(), Optional.empty());
	}

	public static Gem gem(String key, double main, String other, double otherStrength, String drawback) {
		return new Gem(GemText.holder(key), 6, 3, (float) main,
				other == null ? Optional.empty() : Optional.of(new Gem.Roll(GemText.holder(other), (float) otherStrength)),
				drawback == null ? Optional.empty() : Optional.of(new Gem.Roll(GemText.holder(drawback), (float) Balance.get().drawbackOne)));
	}

	/** A gem of the model's quality: variant x Infusion x grade. */
	public static Gem modelGem(String key, String variant, String base, String grade) {
		Balance b = Balance.get();
		double q = b.effect(key).variantFactor(variant) * b.infusions.stream().filter(i -> i.base().equals(base)).findFirst().orElseThrow().factor()
				* b.grades.get(grade);
		return gem(key, q);
	}

	public static ItemStack item(Gem gem) {
		return GemEnchanting.finished(gem);
	}

	public static ItemStack socketed(Item item, Gem... gems) {
		ItemStack stack = new ItemStack(item);
		SocketedGems s = SocketedGems.EMPTY;
		for (Gem g : gems) {
			s = s.with(g);
		}
		stack.set(ModComponents.SOCKETED_GEMS, s);
		return stack;
	}

	public static ItemStack infused(String key, String variant, GemForm form, int infusion) {
		ItemStack stack = new ItemStack(ModItems.INFUSED_GEM);
		stack.set(ModComponents.INFUSED_GEM, new InfusedGem(GemText.holder(key), variant, form, infusion));
		return stack;
	}

	public static void check(GameTestHelper helper, boolean ok, String message) {
		if (!ok) {
			helper.fail(message);
		}
	}

	public static void close(double actual, double expected, double eps, GameTestHelper helper, String what) {
		check(helper, Math.abs(actual - expected) <= eps, what + ": " + actual + ", expected " + expected);
	}

	public static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}
}

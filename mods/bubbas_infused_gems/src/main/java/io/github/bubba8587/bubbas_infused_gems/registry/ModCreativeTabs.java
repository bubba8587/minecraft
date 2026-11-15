// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.Optional;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

/** Creative tab: a lingering Infusion VI Infused Gem and a perfect finished gem of every effect v0 implements. */
public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, BubbasInfusedGems.id("main"));
	public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY,
			FabricCreativeModeTab.builder()
					.icon(() -> perfect("speed"))
					.title(Component.translatable("itemGroup." + BubbasInfusedGems.MOD_ID))
					.displayItems((params, output) -> {
						Balance b = Balance.get();
						for (String key : b.v0Brewable.stream().sorted().toList()) {
							output.accept(infused(key, top(key), GemForm.LINGERING, 6));
						}
						for (String key : b.v0Brewable.stream().sorted().toList()) {
							output.accept(perfect(key));
						}
						output.accept(perfect("haste"));
						output.accept(perfect("luck"));
					})
					.build());

	private ModCreativeTabs() {
	}

	/** The effect's strongest potion variant (the one that reaches a perfect gem). */
	public static String top(String key) {
		return Balance.get().effect(key).variants().values().stream().max((a, c) -> Double.compare(a.factor(), c.factor()))
				.map(Balance.Variant::name).orElse("normal");
	}

	public static ItemStack infused(String key, String variant, GemForm form, int infusion) {
		ItemStack stack = new ItemStack(ModItems.INFUSED_GEM);
		stack.set(ModComponents.INFUSED_GEM, new InfusedGem(GemText.holder(key), variant, form, infusion));
		return stack;
	}

	/** A perfect gem: strongest potion, Infusion VI, tier 3, the strongest result alone. */
	public static ItemStack perfect(String key) {
		return GemEnchanting.finished(new Gem(GemText.holder(key), 6, 3, 1.0F, Optional.empty(), Optional.empty()));
	}

	public static void initialize() {
	}
}

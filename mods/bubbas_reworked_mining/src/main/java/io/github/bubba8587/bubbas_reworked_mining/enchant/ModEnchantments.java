// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.enchant;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mining.BubbasReworkedMining;
import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.RemoveBinomial;

import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;

/**
 * Enchantment changes (specs/reworked-mining.md). True Unbreaking itself is data
 * ({@code data/bubbas_reworked_mining/enchantment/true_unbreaking.json}: item damage set to 0, exclusive with Mending,
 * in no enchanting-table, trade or random-loot tag). Stronger Unbreaking is an extra item-damage roll appended to
 * vanilla's Unbreaking through Fabric's enchantment builder hook, so vanilla's file is untouched and the numbers stay
 * in {@link MiningTuning}. Stronger Efficiency is in the mining curve (code), since it has to scale with the tool.
 */
public final class ModEnchantments {
	public static final ResourceKey<Enchantment> TRUE_UNBREAKING = ResourceKey.create(Registries.ENCHANTMENT, BubbasReworkedMining.id("true_unbreaking"));

	private ModEnchantments() {
	}

	public static void initialize() {
		EnchantmentEvents.MODIFY_WITH_LOOKUP.register((key, builder, source, lookup) -> {
			if (key.equals(Enchantments.UNBREAKING)) {
				List<Float> chances = new ArrayList<>();
				for (float chance : MiningTuning.UNBREAKING_EXTRA_CANCEL) {
					chances.add(chance);
				}
				float last = MiningTuning.UNBREAKING_EXTRA_CANCEL[MiningTuning.UNBREAKING_EXTRA_CANCEL.length - 1];
				builder.withEffect(EnchantmentEffectComponents.ITEM_DAMAGE,
						new RemoveBinomial(LevelBasedValue.lookup(chances, LevelBasedValue.constant(last))));
			}
		});
	}
}

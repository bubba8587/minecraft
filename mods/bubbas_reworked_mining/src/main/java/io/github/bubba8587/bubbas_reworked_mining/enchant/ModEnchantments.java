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

/** Enchantment changes. */
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

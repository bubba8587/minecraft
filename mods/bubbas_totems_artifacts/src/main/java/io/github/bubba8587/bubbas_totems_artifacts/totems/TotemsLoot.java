// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.totems;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;

/**
 * Where the five new totems are found (specs/totems-artifacts.md): like Warding's {@code WardingLoot}, one extra pool per
 * table and totem, rolling one totem at the chance in {@link TotemsTuning}. Additive (vanilla's pools untouched); tables
 * a datapack replaced through Fabric are left alone. Chests only: archaeology tables give a single item, so a second
 * pool there would break brushing.
 */
public final class TotemsLoot {
	private TotemsLoot() {
	}

	/** Every (table, totem) chance, for tests and docs. */
	public static Map<Item, Map<ResourceKey<LootTable>, Float>> all() {
		Map<Item, Map<ResourceKey<LootTable>, Float>> all = new LinkedHashMap<>();
		all.put(ModItems.TOTEM_OF_KEEPING, TotemsTuning.KEEPING_LOOT);
		all.put(ModItems.TOTEM_OF_GATHERING, TotemsTuning.GATHERING_LOOT);
		all.put(ModItems.TOTEM_OF_FORTIFICATION, TotemsTuning.FORTIFICATION_LOOT);
		all.put(ModItems.TOTEM_OF_GROWTH, TotemsTuning.GROWTH_LOOT);
		all.put(ModItems.TOTEM_OF_SEEKING, TotemsTuning.SEEKING_LOOT);
		return all;
	}

	public static void initialize() {
		Map<Item, Map<ResourceKey<LootTable>, Float>> all = all();
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source == LootTableSource.REPLACED) {
				return;
			}
			for (Map.Entry<Item, Map<ResourceKey<LootTable>, Float>> entry : all.entrySet()) {
				Float chance = entry.getValue().get(key);
				if (chance != null) {
					LootPool.Builder pool = LootPool.lootPool().add(LootItem.lootTableItem(entry.getKey()));
					if (chance < 1.0F) {
						pool.when(LootItemRandomChanceCondition.randomChance(chance));
					}
					builder.withPool(pool);
				}
			}
		});
	}
}

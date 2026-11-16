// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.warding;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;

/**
 * Where the Totem of Warding is found (specs/undead.md "Warding"): one extra pool on each table in
 * {@link WardingTuning#LOOT}, rolling one totem at that table's chance. Additive only (vanilla's pools are untouched);
 * a table a datapack replaced through Fabric is left alone.
 */
public final class WardingLoot {
	private WardingLoot() {
	}

	public static void initialize() {
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source == LootTableSource.REPLACED) {
				return;
			}
			Float chance = WardingTuning.LOOT.get(key);
			if (chance != null) {
				LootPool.Builder pool = LootPool.lootPool().add(LootItem.lootTableItem(ModItems.TOTEM_OF_WARDING));
				if (chance < 1.0F) {
					pool.when(LootItemRandomChanceCondition.randomChance(chance));
				}
				builder.withPool(pool);
			}
		});
	}
}

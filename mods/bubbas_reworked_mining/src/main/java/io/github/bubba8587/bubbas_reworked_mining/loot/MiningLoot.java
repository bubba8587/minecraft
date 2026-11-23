// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.loot;

import java.util.HashMap;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import io.github.bubba8587.bubbas_reworked_mining.enchant.ModEnchantments;
import io.github.bubba8587.bubbas_reworked_mining.natural.NaturalStone;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootPredicates;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;

/**
 * Loot additions (specs/reworked-mining.md): rare finds on natural stone and deepslate, and True Unbreaking books in a
 * few rare chests. Each is one extra pool added to the vanilla table (vanilla's pools untouched); a table a datapack
 * replaced is left alone. Odds and weights are in {@link MiningTuning}.
 */
public final class MiningLoot {
	private MiningLoot() {
	}

	public static void initialize() {
		Map<ResourceKey<LootTable>, Boolean> finds = new HashMap<>();
		for (String id : NaturalStone.STONE_IDS) {
			finds.put(blockTable(id), false);
		}
		for (String id : NaturalStone.DEEPSLATE_IDS) {
			finds.put(blockTable(id), true);
		}

		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source == LootTableSource.REPLACED) {
				return;
			}
			Boolean deepslate = finds.get(key);
			if (deepslate != null) {
				builder.withPool(findPool(key, deepslate, registries));
				return;
			}
			Float chance = MiningTuning.TRUE_UNBREAKING_LOOT.get(key);
			if (chance != null) {
				Holder<Enchantment> trueUnbreaking = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.TRUE_UNBREAKING);
				builder.withPool(LootPool.lootPool()
						.add(LootItem.lootTableItem(Items.BOOK).apply(new EnchantRandomlyFunction.Builder().withEnchantment(trueUnbreaking)))
						.when(LootItemRandomChanceCondition.randomChance(chance)));
			}
		});
	}

	private static ResourceKey<LootTable> blockTable(String blockId) {
		Identifier id = Identifier.parse(blockId);
		return ResourceKey.create(Registries.LOOT_TABLE, id.withPrefix("blocks/"));
	}

	private static LootPool.Builder findPool(ResourceKey<LootTable> key, boolean deepslate, HolderLookup.Provider registries) {
		HolderGetter<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
		Holder<Enchantment> fortune = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(key.identifier().getPath().substring("blocks/".length())));

		LootPool.Builder pool = LootPool.lootPool()
				// Only natural blocks: placed ones carry bubba_placed=true.
				.when(MatchBlock.blockMatches(BlockPredicate.Builder.block().of(blocks, block)
						.setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(NaturalStone.PLACED, false))))
				.when(InvertedLootItemCondition.invert(registries.lookupOrThrow(Registries.PREDICATE).getOrThrow(LootPredicates.TOOL_CAN_SILK_TOUCH)))
				.when(ExplosionCondition.survivesExplosion())
				.when(BonusLevelTableCondition.bonusLevelFlatChance(fortune,
						deepslate ? MiningTuning.DEEPSLATE_FIND_CHANCE : MiningTuning.STONE_FIND_CHANCE));
		if (deepslate) {
			pool.add(nuggets(Items.IRON_NUGGET, MiningTuning.DEEPSLATE_FIND_IRON_NUGGET))
					.add(nuggets(Items.GOLD_NUGGET, MiningTuning.DEEPSLATE_FIND_GOLD_NUGGET))
					.add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(MiningTuning.DEEPSLATE_FIND_LAPIS))
					.add(LootItem.lootTableItem(Items.EMERALD).setWeight(MiningTuning.DEEPSLATE_FIND_EMERALD))
					.add(LootItem.lootTableItem(Items.DIAMOND).setWeight(MiningTuning.DEEPSLATE_FIND_DIAMOND));
		} else {
			pool.add(nuggets(Items.IRON_NUGGET, MiningTuning.FIND_IRON_NUGGET))
					.add(nuggets(Items.GOLD_NUGGET, MiningTuning.FIND_GOLD_NUGGET))
					.add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(MiningTuning.FIND_LAPIS))
					.add(LootItem.lootTableItem(Items.EMERALD).setWeight(MiningTuning.FIND_EMERALD));
		}
		return pool;
	}

	private static UniformContainerBase.Builder<?> nuggets(Item item, int weight) {
		return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemCountFunction.setCount(
				Holder.direct(new UniformGenerator(ContextIntProviders.exactly(1), ContextIntProviders.exactly(MiningTuning.NUGGET_MAX)))));
	}
}

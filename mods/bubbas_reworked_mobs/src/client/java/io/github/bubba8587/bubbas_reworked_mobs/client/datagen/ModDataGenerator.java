// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.datagen;

import java.util.concurrent.CompletableFuture;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModCreativeTabs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

/** Datagen: block loot, tags, English lang, blockstates and item models. Output: src/main/generated (committed). */
public class ModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(BlockLoot::new);
		pack.addProvider(ItemTagsGen::new);
		pack.addProvider(EntityTypeTagsGen::new);
		pack.addProvider(GameEventTagsGen::new);
		pack.addProvider(EnglishLang::new);
		pack.addProvider(Models::new);
	}

	/** Any harvest tool gives the sleeper item (pose kept, gear not: the gear drops from the block entity); a bare hand leaves remains. */
	static class BlockLoot extends FabricBlockLootSubProvider {
		BlockLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generate() {
			for (UndeadVariant variant : UndeadVariant.values()) {
				SleeperBlock block = ModBlocks.sleeper(variant);
				add(block, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
						.when(ExplosionCondition.survivesExplosion())
						.add(LootItem.lootTableItem(block)
								.when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(items, ModTags.SLEEPER_HARVEST_TOOLS)))
								.apply(CopyBlockState.copyState(block).copy(SleeperBlock.POSE))
								.otherwise(LootItem.lootTableItem(variant.remains())))));
			}
		}
	}

	static class ItemTagsGen extends FabricTagsProvider.ItemTagsProvider {
		ItemTagsGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			builder(ModTags.SLEEPER_HARVEST_TOOLS).addTag(ItemTags.PICKAXES).addTag(ItemTags.AXES).addTag(ItemTags.SHOVELS).addTag(ItemTags.HOES);
			builder(ModTags.SLEEPER_WEAPONS).addTag(ItemTags.SWORDS).addTag(ItemTags.AXES).addTag(ItemTags.SPEARS);
		}
	}

	static class EntityTypeTagsGen extends FabricTagsProvider.EntityTypeTagsProvider {
		EntityTypeTagsGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			ResourceKey<EntityType<?>> skeleton = key(UndeadVariant.SKELETON);
			ResourceKey<EntityType<?>> zombie = key(UndeadVariant.ZOMBIE);
			builder(ModTags.WOKEN_UNDEAD).add(skeleton, zombie);
			// #skeletons and #zombies feed #minecraft:undead (smite, potions, wither targeting).
			builder(EntityTypeTags.SKELETONS).add(skeleton);
			builder(EntityTypeTags.ZOMBIES).add(zombie);
			builder(EntityTypeTags.BURN_IN_DAYLIGHT).addTag(ModTags.WOKEN_UNDEAD);
			builder(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE).addTag(ModTags.WOKEN_UNDEAD);
			builder(ModTags.DOES_NOT_WAKE_SLEEPERS).addTag(EntityTypeTags.UNDEAD);
		}

		private static ResourceKey<EntityType<?>> key(UndeadVariant variant) {
			return ModEntities.woken(variant).builtInRegistryHolder().key();
		}
	}

	static class GameEventTagsGen extends FabricTagsProvider<GameEvent> {
		GameEventTagsGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, Registries.GAME_EVENT, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			builder(ModTags.SLEEPERS_HEAR).addTag(GameEventTags.VIBRATIONS).add(ModGameEvents.AWAKEN.key());
		}
	}

	static class EnglishLang extends FabricLanguageProvider {
		EnglishLang(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
			builder.add(ModItems.sleeper(UndeadVariant.SKELETON), "Sleeping Skeleton");
			builder.add(ModItems.sleeper(UndeadVariant.ZOMBIE), "Sleeping Zombie");
			builder.add(ModEntities.woken(UndeadVariant.SKELETON), "Skeleton");
			builder.add(ModEntities.woken(UndeadVariant.ZOMBIE), "Zombie");
			builder.add(ModCreativeTabs.MAIN_KEY, "Bubba's Reworked Mobs");
			builder.add(ModGameRules.CUT_MONSTER_SPAWNS.getDescriptionId(), "Cut natural monster spawns");
			builder.add(ModGameRules.MONSTER_CAP_PERCENT.getDescriptionId(), "Natural monster cap (% of vanilla)");
		}
	}

	static class Models extends FabricModelProvider {
		Models(FabricPackOutput output) {
			super(output);
		}

		@Override
		public void generateBlockStateModels(BlockModelGenerators gen) {
			// The renderer draws the sleeper; the block model only gives the break particles.
			particleOnly(gen, ModBlocks.sleeper(UndeadVariant.SKELETON), TextureMapping.getBlockTexture(Blocks.BONE_BLOCK, "_side"));
			particleOnly(gen, ModBlocks.sleeper(UndeadVariant.ZOMBIE), TextureMapping.getBlockTexture(Blocks.MUD));
		}

		private static void particleOnly(BlockModelGenerators gen, Block block, Material particle) {
			gen.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
					BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(particle), gen.modelOutput))));
		}

		@Override
		public void generateItemModels(ItemModelGenerators gen) {
			for (UndeadVariant variant : UndeadVariant.values()) {
				gen.generateFlatItem(ModItems.sleeper(variant), ModelTemplates.FLAT_ITEM);
			}
		}
	}
}

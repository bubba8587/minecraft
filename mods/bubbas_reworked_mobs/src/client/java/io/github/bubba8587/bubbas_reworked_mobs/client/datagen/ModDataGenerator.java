// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.datagen;

import java.util.concurrent.CompletableFuture;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
		pack.addProvider(BlockTagsGen::new);
		pack.addProvider(ItemTagsGen::new);
		pack.addProvider(EntityTypeTagsGen::new);
		pack.addProvider(GameEventTagsGen::new);
		pack.addProvider(EnglishLang::new);
		pack.addProvider(Models::new);
	}

	/** A Silk Touch pickaxe gives the sleeper item (pose kept, gear not: the gear drops from the block entity); anything else leaves remains. */
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
								.when(hasSilkTouch())
								.apply(CopyBlockState.copyState(block).copy(SleeperBlock.POSE).copy(SleeperBlock.RESTING))
								.otherwise(LootItem.lootTableItem(variant.remains())))));
			}
			// Gravestones drop themselves (mined with a pickaxe, like stone).
			dropSelf(ModBlocks.GRAVESTONE);
		}
	}

	static class BlockTagsGen extends FabricTagsProvider.BlockTagsProvider {
		BlockTagsGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			// Buried loot digs faster with a shovel (the other placements break at once anyway).
			builder(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.LOOT_DISPLAY.builtInRegistryHolder().key());
			builder(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.GRAVESTONE.builtInRegistryHolder().key());
		}
	}

	static class ItemTagsGen extends FabricTagsProvider.ItemTagsProvider {
		ItemTagsGen(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			builder(ModTags.SLEEPER_HARVEST_TOOLS).forceAddTag(ItemTags.PICKAXES);
			// What a waking mob reaches for first; anything else lying around comes after (loot/LootGrabbing).
			builder(ModTags.PREFERRED_ON_WAKE).forceAddTag(ItemTags.SWORDS).forceAddTag(ItemTags.AXES).forceAddTag(ItemTags.SPEARS)
					.add(key(Items.BOW), key(Items.CROSSBOW), key(Items.TRIDENT), key(Items.MACE));
		}

		private static ResourceKey<Item> key(Item item) {
			return item.builtInRegistryHolder().key();
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
			ResourceKey<EntityType<?>> husk = key(UndeadVariant.HUSK);
			ResourceKey<EntityType<?>> stray = key(UndeadVariant.STRAY);
			ResourceKey<EntityType<?>> drowned = key(UndeadVariant.DROWNED);
			builder(ModTags.WOKEN_UNDEAD).add(skeleton, zombie, husk, stray, drowned);
			builder(ModTags.GRABS_LOOT).add(skeleton, zombie, husk, stray, drowned);
			// #skeletons and #zombies feed #minecraft:undead (smite, potions, wither targeting).
			builder(EntityTypeTags.SKELETONS).add(skeleton, stray);
			builder(EntityTypeTags.ZOMBIES).add(zombie, husk, drowned);
			// Husks don't burn by day (vanilla), so the daylight tag lists the others, not the whole woken tag.
			builder(EntityTypeTags.BURN_IN_DAYLIGHT).add(skeleton, zombie, stray, drowned);
			builder(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE).addTag(ModTags.WOKEN_UNDEAD);
			builder(ModTags.DOES_NOT_WAKE_SLEEPERS).forceAddTag(EntityTypeTags.UNDEAD);
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
			builder(ModTags.SLEEPERS_HEAR).forceAddTag(GameEventTags.VIBRATIONS).add(ModGameEvents.AWAKEN.key());
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
			builder.add(ModItems.sleeper(UndeadVariant.HUSK), "Sleeping Husk");
			builder.add(ModItems.sleeper(UndeadVariant.STRAY), "Sleeping Stray");
			builder.add(ModEntities.woken(UndeadVariant.HUSK), "Husk");
			builder.add(ModEntities.woken(UndeadVariant.STRAY), "Stray");
			builder.add(ModItems.sleeper(UndeadVariant.DROWNED), "Sleeping Drowned");
			builder.add(ModEntities.woken(UndeadVariant.DROWNED), "Drowned");
			builder.add("item.bubbas_reworked_mobs.sleeper.form", "%s (%s)");
			builder.add("item.bubbas_reworked_mobs.sleeper.pose.sitting", "Sitting");
			builder.add("item.bubbas_reworked_mobs.sleeper.pose.lying", "Lying");
			builder.add("item.bubbas_reworked_mobs.sleeper.pose.prone", "Face Down");
			for (UndeadVariant variant : UndeadVariant.values()) {
				String mob = Character.toUpperCase(variant.name().charAt(0)) + variant.name().substring(1).toLowerCase(java.util.Locale.ROOT);
				builder.add(ModItems.sleeper(variant).getDescriptionId() + ".statue", mob + " Statue");
			}
			builder.add("item.bubbas_reworked_mobs.graveyard_map", "Graveyard Explorer Map");
			builder.add("item.bubbas_reworked_mobs.camp_map", "Monster Camp Explorer Map");
			builder.add("item.bubbas_reworked_mobs.fabricator_map", "Fabricator Explorer Map");
			builder.add(ModCreativeTabs.MAIN_KEY, "Bubba's Reworked Mobs");
			builder.add(ModBlocks.LOOT_DISPLAY, "Loot on Display");
			builder.add(ModBlocks.GRAVESTONE, "Gravestone");
			builder.add(LootDisplays.BURIED_HINT, "It's buried. Dig it up with a shovel.");
			String display = "commands." + BubbasReworkedMobs.MOD_ID + ".display.";
			builder.add(display + "success", "Put %s on display (%s) at %s, %s, %s");
			builder.add(display + "not_looking", "Look at a block within 20 blocks");
			builder.add(display + "needs_wall", "Look at the side of a block to hang it on the wall");
			builder.add(display + "no_room", "There is a block in the way");
			builder.add(display + "no_support", "Nothing there to hold it up");
			builder.add(ModGameRules.CUT_MONSTER_SPAWNS.getDescriptionId(), "Cut natural monster spawns");
			builder.add(ModGameRules.MONSTER_CAP_PERCENT.getDescriptionId(), "Natural monster cap (% of vanilla)");
			builder.add(ModGameRules.WATCHER_CREEPERS.getDescriptionId(), "Creepers spawn as watchers");
			builder.add(ModGameRules.CAIRN_ENDERMEN.getDescriptionId(), "Overworld endermen spawn at cairns");
			builder.add(ModGameRules.SHOW_METERS.getDescriptionId(), "Show mob meters (drowned grip, creeper startle)");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.client.drowned.DrownedPoses.GRABBED_HINT, "A drowned has you! Hit it hard and fast to break free");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.NEST_SPIDER, "Spider");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.SPIDER_NEST, "Spider Nest");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.GIANT_SPIDER, "Giant Spider");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.SPUN_WEB, "Spider Web");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.SILK_COCOON, "Silk Cocoon");
			builder.add("bubbas_reworked_mobs.spider.cocooned", "Cocooned!");
			builder.add("bubbas_reworked_mobs.spider.grabbed", "A spider has you! Hit it hard and fast to break free");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPosts.SIGN_LINE_1, "Trader Post");
			builder.add(io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPosts.SIGN_LINE_2, "All welcome");
			// Structure and item tag names (Explorer's Compass, recipe viewers; specs/compat.md)
			builder.add("structure.bubbas_reworked_mobs.monster_camp", "Monster Camp");
			builder.add("structure.bubbas_reworked_mobs.crypt_yard", "Crypt Yard");
			builder.add("structure.bubbas_reworked_mobs.nether_outpost", "Nether Outpost");
			builder.add("structure.bubbas_reworked_mobs.trader_post", "Trader Post");
			builder.add("tag.item.bubbas_reworked_mobs.sleeper_harvest_tools", "Sleeper Harvest Tools");
			builder.add("tag.item.bubbas_reworked_mobs.preferred_on_wake", "Weapons Woken Undead Prefer");
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
			particleOnly(gen, ModBlocks.sleeper(UndeadVariant.HUSK), TextureMapping.getBlockTexture(Blocks.SAND));
			particleOnly(gen, ModBlocks.sleeper(UndeadVariant.STRAY), TextureMapping.getBlockTexture(Blocks.PACKED_ICE));
			particleOnly(gen, ModBlocks.sleeper(UndeadVariant.DROWNED), TextureMapping.getBlockTexture(Blocks.PRISMARINE));
			// Loot on display: the renderer draws the item; break particles are neutral iron.
			particleOnly(gen, ModBlocks.LOOT_DISPLAY, TextureMapping.getBlockTexture(Blocks.IRON_BLOCK));
			// Gravestones: 16 rotations like a standing sign, from four hand-made models (models/block/gravestone_rot_0..3,
			// turned 0, 22.5, 45 and 67.5 degrees) each turned by quarter turns, as vanilla's sign blockstates are.
			gen.blockStateOutput.accept(net.minecraft.client.data.models.blockstates.MultiVariantGenerator.dispatch(ModBlocks.GRAVESTONE)
					.with(net.minecraft.client.data.models.blockstates.PropertyDispatch.initial(net.minecraft.world.level.block.StandingSignBlock.ROTATION)
							.generate(rotation -> {
								var variant = BlockModelGenerators.plainVariant(io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs.id("block/gravestone_rot_" + rotation % 4));
								return switch (rotation / 4) {
									case 1 -> variant.with(BlockModelGenerators.Y_ROT_90);
									case 2 -> variant.with(BlockModelGenerators.Y_ROT_180);
									case 3 -> variant.with(BlockModelGenerators.Y_ROT_270);
									default -> variant;
								};
							})));
		}

		private static final com.mojang.math.Transformation SKULL_TRANSFORM = new com.mojang.math.Transformation(
				new org.joml.Vector3f(0.5F, 0.0F, 0.5F), new org.joml.Quaternionf().rotationX((float) Math.PI), null, null);

		private static net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked head(UndeadVariant variant) {
			var skeleton = net.minecraft.world.level.block.SkullBlock.Types.SKELETON;
			var zombie = net.minecraft.world.level.block.SkullBlock.Types.ZOMBIE;
			return switch (variant) {
				case SKELETON -> new net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked(skeleton);
				case ZOMBIE -> new net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked(zombie);
				case HUSK -> new net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked(zombie,
						java.util.Optional.of(net.minecraft.resources.Identifier.withDefaultNamespace("zombie/husk")), 0.0F);
				case STRAY -> new net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked(skeleton,
						java.util.Optional.of(net.minecraft.resources.Identifier.withDefaultNamespace("skeleton/stray")), 0.0F);
				case DROWNED -> new net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked(zombie,
						java.util.Optional.of(net.minecraft.resources.Identifier.withDefaultNamespace("zombie/drowned")), 0.0F);
			};
		}

		private static void particleOnly(BlockModelGenerators gen, Block block, Material particle) {
			gen.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block,
					BlockModelGenerators.plainVariant(ModelTemplates.PARTICLE_ONLY.create(block, TextureMapping.particle(particle), gen.modelOutput))));
		}

		@Override
		public void generateItemModels(ItemModelGenerators gen) {
			// Sleeper icons: vanilla mob heads
			for (UndeadVariant variant : UndeadVariant.values()) {
				gen.itemModelOutput.accept(ModItems.sleeper(variant), net.minecraft.client.data.models.model.ItemModelUtils.specialModel(
						net.minecraft.client.data.models.model.ModelLocationUtils.decorateItemModelLocation("template_skull"), SKULL_TRANSFORM, head(variant)));
			}
			// The gravestone item shows the stone itself (models/item/gravestone, hand-made).
			gen.itemModelOutput.accept(ModItems.GRAVESTONE,
					net.minecraft.client.data.models.model.ItemModelUtils.plainModel(io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs.id("item/gravestone")));
		}
	}
}

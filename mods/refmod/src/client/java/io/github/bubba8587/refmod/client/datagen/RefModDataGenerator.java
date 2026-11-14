// dte:B5,B6
package io.github.bubba8587.refmod.client.datagen;

import java.util.concurrent.CompletableFuture;

import io.github.bubba8587.refmod.block.SignalLampBlock;
import io.github.bubba8587.refmod.registry.ModBlocks;
import io.github.bubba8587.refmod.registry.ModCreativeTabs;
import io.github.bubba8587.refmod.registry.ModEntities;
import io.github.bubba8587.refmod.registry.ModItems;
import io.github.bubba8587.refmod.registry.ModTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

/** Datagen: recipes, block loot, an item tag, English lang, block/item models. Output: src/main/generated (committed). */
public class RefModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(Recipes::new);
		pack.addProvider(BlockLoot::new);
		pack.addProvider(ItemTags::new);
		pack.addProvider(EnglishLang::new);
		pack.addProvider(Models::new);
	}

	static class Recipes extends FabricRecipeProvider {
		Recipes(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
			return new RecipeProvider(recipes, advancements) {
				@Override
				public void buildRecipes() {
					shapeless(RecipeCategory.TOOLS, ModItems.TALLY_STICK).requires(Items.STICK).requires(Items.FLINT)
							.unlockedBy("has_flint", has(Items.FLINT)).save(this.output);
					shaped(RecipeCategory.TOOLS, ModItems.FLINT_HATCHET).define('F', Items.FLINT).define('S', Items.STICK)
							.pattern("FF").pattern("FS").pattern(" S")
							.unlockedBy("has_flint", has(Items.FLINT)).save(this.output);
					shapeless(RecipeCategory.COMBAT, ModItems.PEBBLE, 4).requires(Items.COBBLESTONE)
							.unlockedBy("has_cobblestone", has(Items.COBBLESTONE)).save(this.output);
					shaped(RecipeCategory.REDSTONE, ModBlocks.SIGNAL_LAMP).define('G', Items.GLASS).define('L', Items.GLOWSTONE_DUST)
							.pattern(" G ").pattern("GLG").pattern(" G ")
							.unlockedBy("has_glowstone_dust", has(Items.GLOWSTONE_DUST)).save(this.output);
					shaped(RecipeCategory.REDSTONE, ModBlocks.COUNTER_BLOCK).define('C', Items.COBBLESTONE).define('K', Items.CLOCK)
							.pattern("CCC").pattern("CKC").pattern("CCC")
							.unlockedBy("has_clock", has(Items.CLOCK)).save(this.output);
				}
			};
		}

		@Override
		public String getName() {
			return "refmod recipes";
		}
	}

	static class BlockLoot extends FabricBlockLootSubProvider {
		BlockLoot(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generate() {
			dropSelf(ModBlocks.SIGNAL_LAMP);
			dropSelf(ModBlocks.COUNTER_BLOCK);
		}
	}

	static class ItemTags extends FabricTagsProvider.ItemTagsProvider {
		ItemTags(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			builder(ModTags.THROWABLES).add(key(ModItems.PEBBLE)).add(key(Items.SNOWBALL));
		}

		private static ResourceKey<Item> key(Item item) {
			return item.builtInRegistryHolder().key();
		}
	}

	static class EnglishLang extends FabricLanguageProvider {
		EnglishLang(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
			builder.add(ModItems.TALLY_STICK, "Tally Stick");
			builder.add(ModItems.FLINT_HATCHET, "Flint Hatchet");
			builder.add(ModItems.PEBBLE, "Pebble");
			builder.add(ModBlocks.SIGNAL_LAMP, "Signal Lamp");
			builder.add(ModBlocks.COUNTER_BLOCK, "Counter Block");
			builder.add(ModEntities.PEBBLE, "Pebble");
			builder.add(ModCreativeTabs.REFMOD_KEY, "Reference Mod");
			builder.add("item.refmod.tally_stick.uses", "Uses: %s");
			builder.add("hud.refmod.tally", "Tally: %s");
			builder.add("message.refmod.lamp_on", "Signal lamp on (%s toggles)");
			builder.add("message.refmod.lamp_off", "Signal lamp off (%s toggles)");
			builder.add("message.refmod.stats_self", "You have toggled %s signal lamps");
			builder.add("commands.refmod.stats", "%s has toggled %s signal lamps");
			builder.add("key.refmod.show_stats", "Show lamp stats");
			builder.add("key.category.refmod.main", "Reference Mod");
			builder.add(ModTags.THROWABLES, "Throwables");
		}
	}

	static class Models extends FabricModelProvider {
		Models(FabricPackOutput output) {
			super(output);
		}

		@Override
		public void generateBlockStateModels(BlockModelGenerators gen) {
			// Like vanilla createRedstoneLamp: <id> (off) and <id>_on cube_all models, dispatched on LIT.
			MultiVariant off = BlockModelGenerators.plainVariant(TexturedModel.CUBE.create(ModBlocks.SIGNAL_LAMP, gen.modelOutput));
			MultiVariant on = BlockModelGenerators.plainVariant(gen.createSuffixedVariant(ModBlocks.SIGNAL_LAMP, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube));
			gen.blockStateOutput.accept(MultiVariantGenerator.dispatch(ModBlocks.SIGNAL_LAMP)
					.with(BlockModelGenerators.createBooleanModelDispatch(SignalLampBlock.LIT, on, off)));
			gen.createTrivialCube(ModBlocks.COUNTER_BLOCK);
		}

		@Override
		public void generateItemModels(ItemModelGenerators gen) {
			gen.generateFlatItem(ModItems.TALLY_STICK, ModelTemplates.FLAT_ITEM);
			gen.generateFlatItem(ModItems.PEBBLE, ModelTemplates.FLAT_ITEM);
			gen.generateFlatItem(ModItems.FLINT_HATCHET, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
	}
}

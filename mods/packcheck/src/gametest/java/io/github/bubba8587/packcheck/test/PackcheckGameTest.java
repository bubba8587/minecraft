// dte:A4
package io.github.bubba8587.packcheck.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github.bubba8587.packcheck.Packcheck;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Server game tests for the datapacks bundled by packcheck (specs/pack-testing.md).
 *
 * <p>bubbas_extra_recipes: one {@link RecipeCase} per recipe in {@link #EXTRA_RECIPES}. Each case is checked
 * the same way: the recipe is looked up by id, every listed input matches it, no other recipe of that type matches the
 * same input, and the assembled result has the expected item, count, name, lore (and cooking time/experience).
 * To change a recipe, edit its row; to add one, add a row and a one-line {@code @GameTest} method.
 * {@link #everyPackRecipeHasATestCase} fails when the pack and the table disagree.
 */
public class PackcheckGameTest {
	private static final String EXTRA_RECIPES_NS = "bubbas_extra_recipes";

	// --- The table: one row per recipe in datapacks/bubbas_extra_recipes/data/bubbas_extra_recipes/recipe/ ---

	private static final List<RecipeCase> EXTRA_RECIPES = List.of(
		crafting("bamboo_paper", Items.PAPER, 2, "Bamboo Paper", List.of(),
			grid(Map.of('L', Items.BAMBOO), "LLL", "   ", "   "),
			grid(Map.of('L', Items.BAMBOO), "   ", "   ", "LLL")),
		crafting("blackdye", Items.DYE.black(), 1, null, List.of(),
			grid(Map.of('c', Items.COAL), "c  ", "   ", "   "),
			grid(Map.of('c', Items.CHARCOAL), "   ", " c ", "   ")),
		crafting("charcoal_block", Items.COAL_BLOCK, 1, "Charcoal Block", List.of(),
			grid(Map.of('c', Items.CHARCOAL), "ccc", "ccc", "ccc")),
		crafting("trident", Items.TRIDENT, 1, null, List.of(),
			grid(Map.of('#', Items.PRISMARINE_SHARD, 'P', Items.PRISMARINE_CRYSTALS, '/', Items.STICK),
				"###", "P/P", "P/P")),
		crafting("mud2clay", Items.CLAY_BALL, 4, null, List.of(),
			grid(Map.of('m', Items.MUD, 's', Items.SAND), "ms ", "   ", "   "),
			grid(Map.of('m', Items.MUD, 's', Items.RED_SAND), "   ", " s ", "  m")),
		crafting("sand2redsand", Items.RED_SAND, 8, null, List.of(),
			grid(Map.of('s', Items.SAND, 'n', Items.IRON_NUGGET), "sss", "sns", "sss")),
		// block_state {synthetic:"true"}: read by the companion mod (specs/extra-recipes-mod.md); ignored without it.
		cooking(RecipeType.BLASTING, "blast2diamond", Items.DEEPSLATE_DIAMOND_ORE, "Synthetic Diamond Ore", List.of(), 4000, 5.0F,
			Items.COAL_BLOCK).withBlockState(Map.of("synthetic", "true")),
		cooking(RecipeType.BLASTING, "blast2sand", Items.SAND, null, List.of(), 200, 0.0F,
			Items.GRAVEL, Items.GLASS),
		cooking(RecipeType.BLASTING, "blast_quartz", Items.QUARTZ, null, List.of(), 200, 0.1F,
			Items.ANDESITE, Items.DIORITE, Items.GRANITE),
		cooking(RecipeType.BLASTING, "cobble2gravel", Items.GRAVEL, null, List.of(), 200, 0.0F,
			Items.COBBLESTONE),
		cooking(RecipeType.BLASTING, "bone2calcite", Items.CALCITE, null, List.of(), 200, 0.1F,
			Items.BONE_BLOCK),
		cooking(RecipeType.BLASTING, "deepslate2tuff", Items.TUFF, null, List.of(), 200, 0.1F,
			Items.COBBLED_DEEPSLATE),
		cooking(RecipeType.BLASTING, "basalt2smooth", Items.SMOOTH_BASALT, null, List.of(), 200, 0.1F,
			Items.BASALT),
		cooking(RecipeType.BLASTING, "ironnugget", Items.IRON_NUGGET, null, List.of(), 500, 0.1F,
			Items.STONE),
		cooking(RecipeType.SMOKING, "orgglo", Items.GLOWSTONE_DUST, "Organic Glowstone Dust", List.of(), 200, 0.1F,
			Items.GLOW_BERRIES, Items.GLOW_LICHEN, Items.GLOW_INK_SAC),
		cooking(RecipeType.SMOKING, "rotleather", Items.LEATHER, null, List.of("Cured zombie flesh. Gross..."), 300, 0.1F,
			Items.ROTTEN_FLESH)
	);

	@GameTest public void bambooPaper(GameTestHelper helper) { check(helper, "bamboo_paper"); }

	/** Bamboo Paper carries a green tint color that the companion resource pack applies to the paper model. */
	@GameTest public void bambooPaperCarriesTint(GameTestHelper helper) {
		ItemStack out = assembledResults(helper.getLevel()).stream()
				.filter(s -> s.is(Items.PAPER)).findFirst().orElseThrow();
		var cmd = out.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
		helper.assertTrue(cmd != null && !cmd.colors().isEmpty() && cmd.colors().getFirst() == 0xD8F0C0,
				net.minecraft.network.chat.Component.literal("bamboo paper tint: " + cmd));
		helper.succeed();
	}
	@GameTest public void blackDye(GameTestHelper helper) { check(helper, "blackdye"); }
	@GameTest public void charcoalBlock(GameTestHelper helper) { check(helper, "charcoal_block"); }
	@GameTest public void trident(GameTestHelper helper) { check(helper, "trident"); }
	@GameTest public void syntheticDiamondOre(GameTestHelper helper) { check(helper, "blast2diamond"); }
	@GameTest public void blastToSand(GameTestHelper helper) { check(helper, "blast2sand"); }
	@GameTest public void blastQuartz(GameTestHelper helper) { check(helper, "blast_quartz"); }
	@GameTest public void cobbleToGravel(GameTestHelper helper) { check(helper, "cobble2gravel"); }
	@GameTest public void mudToClay(GameTestHelper helper) { check(helper, "mud2clay"); }
	@GameTest public void sandToRedSand(GameTestHelper helper) { check(helper, "sand2redsand"); }
	@GameTest public void boneToCalcite(GameTestHelper helper) { check(helper, "bone2calcite"); }
	@GameTest public void deepslateToTuff(GameTestHelper helper) { check(helper, "deepslate2tuff"); }
	@GameTest public void basaltToSmooth(GameTestHelper helper) { check(helper, "basalt2smooth"); }
	@GameTest public void ironNugget(GameTestHelper helper) { check(helper, "ironnugget"); }
	@GameTest public void organicGlowstone(GameTestHelper helper) { check(helper, "orgglo"); }
	@GameTest public void rottenFleshLeather(GameTestHelper helper) { check(helper, "rotleather"); }

	// --- Pack-level checks ---

	@GameTest
	public void everyPackRecipeHasATestCase(GameTestHelper helper) {
		// The pack is bundled and enabled (otherwise every recipe row below fails too, with a less clear message).
		var selected = helper.getLevel().getServer().getPackRepository().getSelectedIds();
		helper.assertTrue(Packcheck.bundledPacks().contains(EXTRA_RECIPES_NS) && selected.contains(Packcheck.id(EXTRA_RECIPES_NS).toString()),
			"pack not bundled or not enabled: bundled " + Packcheck.bundledPacks() + ", selected " + selected);
		List<String> inPack = recipes(helper.getLevel()).getRecipes().stream()
			.map(h -> h.id().identifier())
			.filter(id -> id.getNamespace().equals(EXTRA_RECIPES_NS))
			.map(Identifier::getPath).sorted().toList();
		List<String> inTable = EXTRA_RECIPES.stream().map(RecipeCase::id).sorted().toList();
		helper.assertValueEqual(inPack, inTable, "recipes in the pack vs. rows in PackcheckGameTest.EXTRA_RECIPES");
		helper.succeed();
	}

	/** End to end: a real smoker cooks rotten flesh into leather that keeps its lore. */
	@GameTest(maxTicks = 400)
	public void smokerCooksRottenFleshLeather(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.SMOKER);
		AbstractFurnaceBlockEntity smoker = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
		smoker.setItem(0, new ItemStack(Items.ROTTEN_FLESH));
		smoker.setItem(1, new ItemStack(Items.COAL));
		helper.succeedWhen(() -> {
			ItemStack out = smoker.getItem(2);
			helper.assertTrue(out.is(Items.LEATHER), "smoker output: " + out);
			assertNameAndLore(helper, "rotleather (smoker)", out, null, List.of("Cured zombie flesh. Gross..."));
		});
	}

	// --- The shared check ---

	private static void check(GameTestHelper helper, String id) {
		RecipeCase c = EXTRA_RECIPES.stream().filter(r -> r.id.equals(id)).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("no table row for " + id));
		ServerLevel level = helper.getLevel();
		RecipeManager recipes = recipes(level);
		ResourceKey<Recipe<?>> key = key(EXTRA_RECIPES_NS, c.id);
		RecipeHolder<?> holder = recipes.byKey(key).orElse(null);
		helper.assertTrue(holder != null, "recipe " + key.identifier() + " is not loaded");
		helper.assertValueEqual(holder.value().getType(), c.type, c.id + " recipe type");

		for (RecipeInput input : c.inputs()) {
			String where = c.id + " with " + describe(input);
			helper.assertTrue(matches(holder, input, level), where + ": recipe does not match");
			helper.assertValueEqual(matchingIds(level, c.type, input, EXTRA_RECIPES_NS), List.of(key.identifier()), where + ": vanilla/pack recipes that match this input");
			ItemStack out = assemble(holder, input);
			helper.assertTrue(out.is(c.result), where + ": result is " + out + ", expected " + c.result);
			helper.assertValueEqual(out.getCount(), c.count, where + ": result count");
			assertNameAndLore(helper, where, out, c.name, c.lore);
			BlockItemStateProperties state = out.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
			helper.assertValueEqual(state.properties(), c.blockState, where + ": block_state component");
		}

		if (holder.value() instanceof AbstractCookingRecipe cooking) {
			helper.assertValueEqual(cooking.cookingTime(), c.cookingTime, c.id + " cooking time");
			helper.assertValueEqual(cooking.experience(), c.experience, c.id + " experience");
			ResourceKey<RecipePropertySet> slot = c.type == RecipeType.SMOKING ? RecipePropertySet.SMOKER_INPUT : RecipePropertySet.BLAST_FURNACE_INPUT;
			for (RecipeInput input : c.inputs()) {
				helper.assertTrue(recipes.propertySet(slot).test(input.getItem(0)), c.id + ": " + slot.identifier() + " does not accept " + input.getItem(0));
			}
		}
		helper.succeed();
	}

	private static void assertNameAndLore(GameTestHelper helper, String where, ItemStack out, String name, List<String> lore) {
		if (name != null) {
			Component itemName = out.get(DataComponents.ITEM_NAME);
			helper.assertTrue(itemName != null && !itemName.equals(out.getItem().getDefaultInstance().get(DataComponents.ITEM_NAME)),
				where + ": result has no item_name override: " + itemName);
			helper.assertValueEqual(itemName.getString(), name, where + ": item_name");
			helper.assertValueEqual(out.getHoverName().getString(), name, where + ": shown name");
		}
		ItemLore itemLore = out.get(DataComponents.LORE);
		List<String> lines = itemLore == null ? List.of() : itemLore.lines().stream().map(Component::getString).toList();
		helper.assertValueEqual(lines, lore, where + ": lore");
		if (name == null && lore.isEmpty()) {
			helper.assertTrue(out.getComponentsPatch().isEmpty(), where + ": unexpected components " + out.getComponentsPatch());
		}
	}

	// --- Helpers ---

	/**
	 * One recipe's expectations. Inputs are suppliers because ItemStacks can't be made before the world's
	 * components are bound (this table is built when the class loads).
	 */
	private record RecipeCase(String id, RecipeType<?> type, List<Supplier<RecipeInput>> inputSuppliers, Item result, int count,
			String name, List<String> lore, int cookingTime, float experience, Map<String, String> blockState) {
		RecipeCase(String id, RecipeType<?> type, List<Supplier<RecipeInput>> inputSuppliers, Item result, int count,
				String name, List<String> lore, int cookingTime, float experience) {
			this(id, type, inputSuppliers, result, count, name, lore, cookingTime, experience, Map.of());
		}

		/** The result's expected minecraft:block_state properties (none by default). */
		RecipeCase withBlockState(Map<String, String> state) {
			return new RecipeCase(id, type, inputSuppliers, result, count, name, lore, cookingTime, experience, state);
		}

		List<RecipeInput> inputs() {
			return inputSuppliers.stream().map(Supplier::get).toList();
		}
	}

	/** A crafting-table recipe; each grid is one arrangement that must craft it. */
	@SafeVarargs
	private static RecipeCase crafting(String id, Item result, int count, String name, List<String> lore, Supplier<RecipeInput>... grids) {
		return new RecipeCase(id, RecipeType.CRAFTING, List.of(grids), result, count, name, lore, 0, 0.0F);
	}

	/** A blasting/smoking recipe; each item is one accepted ingredient. */
	private static RecipeCase cooking(RecipeType<?> type, String id, Item result, String name, List<String> lore,
			int cookingTime, float experience, Item... inputs) {
		List<Supplier<RecipeInput>> in = Arrays.stream(inputs).<Supplier<RecipeInput>>map(i -> () -> new SingleRecipeInput(new ItemStack(i))).toList();
		return new RecipeCase(id, type, in, result, 1, name, lore, cookingTime, experience);
	}

	private static Supplier<RecipeInput> grid(Map<Character, Item> key, String... rows) {
		return () -> craftingGrid(key, rows);
	}

	/** A 3x3 crafting-table grid; spaces are empty slots. CraftingInput.of trims empty rows/columns like the table does. */
	private static CraftingInput craftingGrid(Map<Character, Item> key, String... rows) {
		List<ItemStack> items = new ArrayList<>(9);
		for (String row : rows) {
			for (char ch : row.toCharArray()) {
				items.add(ch == ' ' ? ItemStack.EMPTY : new ItemStack(key.get(ch)));
			}
		}
		return CraftingInput.of(3, rows.length, items);
	}

	private static RecipeManager recipes(ServerLevel level) {
		return level.recipeAccess();
	}

	private static ResourceKey<Recipe<?>> key(String namespace, String path) {
		return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(namespace, path));
	}

	@SuppressWarnings("unchecked")
	private static boolean matches(RecipeHolder<?> holder, RecipeInput input, ServerLevel level) {
		return ((Recipe<RecipeInput>) holder.value()).matches(input, level);
	}

	@SuppressWarnings("unchecked")
	static ItemStack assemble(RecipeHolder<?> holder, RecipeInput input) {
		return ((Recipe<RecipeInput>) holder.value()).assemble(input);
	}

	/**
	 * Every loaded recipe of this type from vanilla or the given pack namespace that matches the input. Other bundled
	 * packs (e.g. the internal refpack) are ignored: players never run them together with this pack.
	 */
	private static List<Identifier> matchingIds(ServerLevel level, RecipeType<?> type, RecipeInput input, String packNamespace) {
		return recipes(level).getRecipes().stream()
			.filter(h -> h.value().getType() == type)
			.filter(h -> h.id().identifier().getNamespace().equals("minecraft") || h.id().identifier().getNamespace().equals(packNamespace))
			.filter(h -> {
				try {
					return matches(h, input, level);
				} catch (ClassCastException e) {
					return false;
				}
			})
			.map(h -> h.id().identifier()).sorted().toList();
	}

	private static String describe(RecipeInput input) {
		List<String> items = new ArrayList<>();
		for (int i = 0; i < input.size(); i++) {
			ItemStack s = input.getItem(i);
			items.add(s.isEmpty() ? "-" : s.getItem().toString());
		}
		return items.toString();
	}

	static int recipeCaseCount() {
		return EXTRA_RECIPES.size();
	}

	/** Assembled result of every row's first input, for the client screenshot test. */
	static List<ItemStack> assembledResults(ServerLevel level) {
		List<ItemStack> out = new ArrayList<>();
		for (RecipeCase c : EXTRA_RECIPES) {
			recipes(level).byKey(key(EXTRA_RECIPES_NS, c.id)).ifPresent(h -> out.add(assemble(h, c.inputs().getFirst())));
		}
		return out;
	}
}

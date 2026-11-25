// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.test;

import java.util.Collections;

import io.github.bubba8587.bubbas_extra_recipes.BubbasExtraRecipes;
import io.github.bubba8587.bubbas_extra_recipes.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Quartz shards: andesite, diorite and granite blast into one shard; four shards craft one nether quartz; no direct quartz. */
public class QuartzShardGameTest {
	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.world.item.crafting.RecipeInput> Recipe<T> recipe(GameTestHelper helper, String path) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, BubbasExtraRecipes.id(path));
		return (Recipe<T>) helper.getLevel().recipeAccess().byKey(key).orElseThrow(() -> helper.assertionException("no recipe " + path)).value();
	}

	@GameTest
	public void stoneBlastsToShardsAndFourMakeQuartz(GameTestHelper helper) {
		Recipe<SingleRecipeInput> blast = recipe(helper, "blast_quartz_shard");
		for (var stone : java.util.List.of(Items.ANDESITE, Items.DIORITE, Items.GRANITE)) {
			SingleRecipeInput in = new SingleRecipeInput(new ItemStack(stone));
			helper.assertTrue(blast.matches(in, helper.getLevel()), "blasting doesn't take " + stone);
			ItemStack out = blast.assemble(in);
			helper.assertTrue(out.is(ModItems.QUARTZ_SHARD) && out.getCount() == 1, stone + " blasts into " + out);
		}
		Recipe<CraftingInput> craft = recipe(helper, "quartz_from_shards");
		CraftingInput four = CraftingInput.of(2, 2, Collections.nCopies(4, new ItemStack(ModItems.QUARTZ_SHARD)));
		helper.assertTrue(craft.matches(four, helper.getLevel()), "four shards in a square don't craft");
		ItemStack quartz = craft.assemble(four);
		helper.assertTrue(quartz.is(Items.QUARTZ) && quartz.getCount() == 1, "four shards make " + quartz);
		ResourceKey<Recipe<?>> old = ResourceKey.create(Registries.RECIPE, BubbasExtraRecipes.id("blast_quartz"));
		helper.assertTrue(helper.getLevel().recipeAccess().byKey(old).isEmpty(), "the old stone-to-quartz blasting recipe is still there");
		helper.succeed();
	}
}

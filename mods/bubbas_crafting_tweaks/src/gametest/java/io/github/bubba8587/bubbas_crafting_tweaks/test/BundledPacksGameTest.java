// dte:C3
package io.github.bubba8587.bubbas_crafting_tweaks.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.github.bubba8587.bubbas_crafting_tweaks.BubbasCraftingTweaks;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The jar carries the repo's Crafting Tweaks datapack as an always-enabled built-in data pack: with only this mod
 * installed (no datapack in the world), every recipe file of the pack is loaded.
 */
public class BundledPacksGameTest {
	@GameTest
	public void bundledDatapackIsEnabledAndItsRecipesExist(GameTestHelper helper) throws IOException {
		var selected = helper.getLevel().getServer().getPackRepository().getSelectedIds();
		helper.assertTrue(selected.contains(BubbasCraftingTweaks.DATAPACK_ID.toString()), "bundled datapack not enabled; selected: " + selected);

		Path recipes = FabricLoader.getInstance().getModContainer(BubbasCraftingTweaks.MOD_ID).orElseThrow()
				.findPath("bundled/datapack/data/bubbas_crafting_tweaks/recipe").orElseThrow();
		List<String> files;
		try (Stream<Path> s = Files.list(recipes)) {
			files = s.map(p -> p.getFileName().toString().replace("/", "")).filter(n -> n.endsWith(".json"))
					.map(n -> n.substring(0, n.length() - 5)).sorted().toList();
		}
		helper.assertTrue(files.size() >= 10, "bundled recipe files: " + files);
		for (String name : files) {
			Identifier id = BubbasCraftingTweaks.id(name);
			helper.assertTrue(helper.getLevel().recipeAccess().getRecipes().stream().anyMatch(h -> h.id().identifier().equals(id)),
					"recipe " + id + " from the bundled datapack is not loaded");
		}
		helper.succeed();
	}
}

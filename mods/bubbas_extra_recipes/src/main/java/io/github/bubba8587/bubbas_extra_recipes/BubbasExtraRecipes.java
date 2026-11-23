// dte:C3
package io.github.bubba8587.bubbas_extra_recipes;

import io.github.bubba8587.bubbas_extra_recipes.blockstate.SyntheticOre;
import io.github.bubba8587.bubbas_extra_recipes.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Companion mod for the Bubba's Extra Recipes datapack. */
public class BubbasExtraRecipes implements ModInitializer {
	public static final String MOD_ID = "bubbas_extra_recipes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Built-in data pack id. */
	public static final Identifier DATAPACK_ID = id("extra_recipes_datapack");
	/** Built-in resource pack id: the repo's resourcepacks/bubbas_extra_recipes. */
	public static final Identifier RESOURCEPACK_ID = id("extra_recipes_resources");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.initialize();
		SyntheticOre.initialize();
		registerBundledPacks();
	}

	private static void registerBundledPacks() {
		ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
		// The recipes: always on, so mod users install one file
		if (!ResourceLoader.registerBuiltinPack(DATAPACK_ID, "bundled/datapack", mod,
				Component.literal("Bubba's Extra Recipes recipes"), PackActivationType.ALWAYS_ENABLED)) {
			throw new IllegalStateException("bundled Extra Recipes datapack is missing from the jar");
		}
		ResourceLoader.registerBuiltinPack(RESOURCEPACK_ID, "bundled/resourcepack", mod,
				Component.literal("Bubba's Extra Recipes looks"), PackActivationType.DEFAULT_ENABLED);
	}
}

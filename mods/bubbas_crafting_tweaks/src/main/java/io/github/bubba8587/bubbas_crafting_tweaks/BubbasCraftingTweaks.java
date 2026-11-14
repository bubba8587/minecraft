// dte:C3
package io.github.bubba8587.bubbas_crafting_tweaks;

import io.github.bubba8587.bubbas_crafting_tweaks.blockstate.SyntheticOre;
import io.github.bubba8587.bubbas_crafting_tweaks.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Companion mod for the Bubba's Crafting Tweaks datapack (specs/crafting-tweaks-mod.md). */
public class BubbasCraftingTweaks implements ModInitializer {
	public static final String MOD_ID = "bubbas_crafting_tweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Built-in data pack id: the repo's datapacks/bubbas_crafting_tweaks, copied into the jar by the build. */
	public static final Identifier DATAPACK_ID = id("crafting_tweaks_datapack");
	/** Built-in resource pack id: the repo's resourcepacks/bubbas_crafting_tweaks. */
	public static final Identifier RESOURCEPACK_ID = id("crafting_tweaks_resources");

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
		// The recipes: always on, so mod users install one file.
		if (!ResourceLoader.registerBuiltinPack(DATAPACK_ID, "bundled/datapack", mod,
				Component.literal("Bubba's Crafting Tweaks recipes"), PackActivationType.ALWAYS_ENABLED)) {
			throw new IllegalStateException("bundled Crafting Tweaks datapack is missing from the jar");
		}
		// The looks (Bamboo Paper's tint): on by default, players can switch it off in Resource Packs.
		ResourceLoader.registerBuiltinPack(RESOURCEPACK_ID, "bundled/resourcepack", mod,
				Component.literal("Bubba's Crafting Tweaks looks"), PackActivationType.DEFAULT_ENABLED);
	}
}

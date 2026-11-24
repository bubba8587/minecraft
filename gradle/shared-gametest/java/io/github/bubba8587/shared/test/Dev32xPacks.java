// dte:B16
package io.github.bubba8587.shared.test;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Test runs only: each loaded mod's resourcepacks/<modid>_32x/ as built-in pack <modid>:<modid>_32x (specs/textures.md). */
public class Dev32xPacks implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			String id = mod.getMetadata().getId();
			if (mod.findPath("resourcepacks/" + id + "_32x/pack.mcmeta").isEmpty()) {
				continue;
			}
			ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath(id, id + "_32x"), mod,
				Component.literal(mod.getMetadata().getName() + " 32x (Classic Faithful style)"), PackActivationType.NORMAL);
		}
	}
}

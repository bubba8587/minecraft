// dte:C3,B16
package io.github.bubba8587.bubbas_crafting_tweaks.client;

import io.github.bubba8587.bubbas_crafting_tweaks.BubbasCraftingTweaks;
import io.github.bubba8587.bubbas_crafting_tweaks.network.ExtraStatesHandshake;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class BubbasCraftingTweaksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Synthetic ore adds a block state: tell the server this client has it, or it turns the client away.
		ClientConfigurationNetworking.registerGlobalReceiver(ExtraStatesHandshake.TYPE, (payload, context) -> { });

		// 32x textures (Faithful style) as an optional built-in resource pack: specs/textures.md.
		FabricLoader.getInstance().getModContainer(BubbasCraftingTweaks.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasCraftingTweaks.id(BubbasCraftingTweaks.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Crafting Tweaks 32x (Faithful style)"), PackActivationType.NORMAL));
	}
}

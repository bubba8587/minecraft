// dte:C3,B16
package io.github.bubba8587.bubbas_extra_recipes.client;

import io.github.bubba8587.bubbas_extra_recipes.BubbasExtraRecipes;
import io.github.bubba8587.bubbas_extra_recipes.network.ExtraStatesHandshake;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class BubbasExtraRecipesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Synthetic ore adds a block state
		ClientConfigurationNetworking.registerGlobalReceiver(ExtraStatesHandshake.TYPE, (payload, context) -> { });

		FabricLoader.getInstance().getModContainer(BubbasExtraRecipes.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasExtraRecipes.id(BubbasExtraRecipes.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Extra Recipes 32x (Faithful style)"), PackActivationType.NORMAL));
	}
}

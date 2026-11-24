// dte:C3,B16
package io.github.bubba8587.bubbas_extra_recipes.client;

import io.github.bubba8587.bubbas_extra_recipes.network.ExtraStatesHandshake;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;

public class BubbasExtraRecipesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Synthetic ore adds a block state
		ClientConfigurationNetworking.registerGlobalReceiver(ExtraStatesHandshake.TYPE, (payload, context) -> { });
	}
}

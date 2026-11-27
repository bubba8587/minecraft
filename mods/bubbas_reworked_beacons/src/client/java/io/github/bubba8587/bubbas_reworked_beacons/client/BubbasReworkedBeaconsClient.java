// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.client;

import net.minecraft.client.core.registries.BuiltInClientRegistries;
import net.minecraft.core.WritableRegistry;
import io.github.bubba8587.bubbas_reworked_beacons.menu.ModMenus;
import io.github.bubba8587.bubbas_reworked_beacons.network.ExtraStatesHandshake;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class BubbasReworkedBeaconsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The upside-down beacon adds a block state
		ClientConfigurationNetworking.registerGlobalReceiver(ExtraStatesHandshake.TYPE, (payload, context) -> { });
		MenuScreens.register((WritableRegistry<MenuScreens.ScreenConstructor<?, ?>>) BuiltInClientRegistries.MENU_SCREEN, ModMenus.BEACON, BeaconItemScreen::new);
	}
}

// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons;

import io.github.bubba8587.bubbas_reworked_beacons.menu.ModMenus;
import io.github.bubba8587.bubbas_reworked_beacons.network.ExtraStatesHandshake;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

/** Bubba's Reworked Beacons. */
public class BubbasReworkedBeacons implements ModInitializer {
	public static final String MOD_ID = "bubbas_reworked_beacons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModMenus.initialize();
		ExtraStatesHandshake.initialize();
	}
}

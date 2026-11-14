package io.github.bubba8587.bubbas_reworked_mobs;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BubbasReworkedMobs implements ModInitializer {
	public static final String MOD_ID = "bubbas_reworked_mobs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
	}
}

package io.github.bubba8587.refmod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefMod implements ModInitializer {
	public static final String MOD_ID = "refmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("refmod init");
	}
}

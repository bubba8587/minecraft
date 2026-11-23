// dte:C8
package io.github.bubba8587.bubbas_world_tweaks;

import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import io.github.bubba8587.bubbas_world_tweaks.lava.ModParticles;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bubba's World Tweaks (specs/world-tweaks.md): world and environment touches. First: lava bubbles and lava geysers. */
public class BubbasWorldTweaks implements ModInitializer {
	public static final String MOD_ID = "bubbas_world_tweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModParticles.initialize();
		LavaGeysers.initialize();
	}
}

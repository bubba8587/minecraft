// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModCommands;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModCreativeTabs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.Creepers;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.Drowneds;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.Endermen;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadCombat;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

/**
 * Bubba's Reworked Mobs. Shared infrastructure (registries, spawn-cap control, loot on display) lives at the top level;
 * each mob family has its own package ({@code undead} is part 1, specs/undead.md).
 */
public class BubbasReworkedMobs implements ModInitializer {
	/** The only place the mod id is written in Java. */
	public static final String MOD_ID = "bubbas_reworked_mobs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order: game events and entities before the blocks that use them, blocks before items and block entities.
		ModGameEvents.initialize();
		ModGameRules.initialize();
		ModEntities.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		ModBlockEntities.initialize();
		ModCreativeTabs.initialize();
		ModCommands.initialize();
		UndeadCombat.initialize();
		Creepers.initialize();
		Endermen.initialize();
		Drowneds.initialize();
		io.github.bubba8587.bubbas_reworked_mobs.meter.Meters.initialize();
	}
}

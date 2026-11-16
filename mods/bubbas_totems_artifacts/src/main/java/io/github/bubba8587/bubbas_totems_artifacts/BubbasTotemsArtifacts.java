// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts;

import io.github.bubba8587.bubbas_totems_artifacts.growth.Growth;
import io.github.bubba8587.bubbas_totems_artifacts.keeping.Keeping;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModBlocks;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModCreativeTabs;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.StructureSeeker;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsLoot;
import io.github.bubba8587.bubbas_totems_artifacts.warding.WardingLoot;
import io.github.bubba8587.bubbas_totems_artifacts.warding.Wards;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

/**
 * Bubba's Totems & Artifacts (specs/totems-artifacts.md): found items that do what potions and enchantments don't.
 * Each artifact has its own package. Optional links: Bubba's Reworked Mobs' sleepers (compat/SleeperHook) and Bubba's
 * Reworked Beacons' payload API (compat/WardingBeaconPayload); neither is required.
 */
public class BubbasTotemsArtifacts implements ModInitializer {
	public static final String MOD_ID = "bubbas_totems_artifacts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModComponents.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		ModCreativeTabs.initialize();
		Wards.initialize();
		WardingLoot.initialize();
		// The five totems of 2026-11-16 (Gathering and Fortification need no setup of their own).
		Keeping.initialize();
		Growth.initialize();
		StructureSeeker.initialize();
		TotemsLoot.initialize();
	}
}

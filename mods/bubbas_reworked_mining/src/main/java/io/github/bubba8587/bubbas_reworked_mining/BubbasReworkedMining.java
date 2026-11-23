// dte:C7
package io.github.bubba8587.bubbas_reworked_mining;

import io.github.bubba8587.bubbas_reworked_mining.enchant.ModEnchantments;
import io.github.bubba8587.bubbas_reworked_mining.loot.MiningLoot;
import io.github.bubba8587.bubbas_reworked_mining.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bubba's Reworked Mining: a mining curve from "tough" to "machine". */
public class BubbasReworkedMining implements ModInitializer {
	public static final String MOD_ID = "bubbas_reworked_mining";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModEnchantments.initialize();
		MiningLoot.initialize();
	}
}

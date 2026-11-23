// dte:C7
package io.github.bubba8587.bubbas_reworked_mining;

import io.github.bubba8587.bubbas_reworked_mining.enchant.ModEnchantments;
import io.github.bubba8587.bubbas_reworked_mining.loot.MiningLoot;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bubba's Reworked Mining (specs/reworked-mining.md): a mining curve from "tough" to "machine". The curve's speed and
 * wear are mixins ({@code PlayerMixin}, {@code ItemMixin}) reading {@link MiningTuning}; natural vs placed stone is a
 * block-state mark ({@code natural/NaturalStone}).
 */
public class BubbasReworkedMining implements ModInitializer {
	public static final String MOD_ID = "bubbas_reworked_mining";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEnchantments.initialize();
		MiningLoot.initialize();
	}
}

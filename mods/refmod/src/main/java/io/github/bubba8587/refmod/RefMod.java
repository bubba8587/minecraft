// dte:B5,B6
package io.github.bubba8587.refmod;

import io.github.bubba8587.refmod.command.RefModCommands;
import io.github.bubba8587.refmod.event.LampBreakEvents;
import io.github.bubba8587.refmod.network.RefModNetworking;
import io.github.bubba8587.refmod.registry.ModAttachments;
import io.github.bubba8587.refmod.registry.ModBlockEntities;
import io.github.bubba8587.refmod.registry.ModBlocks;
import io.github.bubba8587.refmod.registry.ModComponents;
import io.github.bubba8587.refmod.registry.ModCreativeTabs;
import io.github.bubba8587.refmod.registry.ModEntities;
import io.github.bubba8587.refmod.registry.ModItemComponents;
import io.github.bubba8587.refmod.registry.ModItems;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class RefMod implements ModInitializer {
	public static final String MOD_ID = "refmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order matters: components before items that use them, blocks before block entities, entities before the items that throw them.
		ModComponents.initialize();
		ModEntities.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		ModBlockEntities.initialize();
		ModCreativeTabs.initialize();
		ModItemComponents.initialize();
		ModAttachments.initialize();
		RefModNetworking.initialize();
		RefModCommands.initialize();
		LampBreakEvents.initialize();
		LOGGER.info("refmod init");
	}
}

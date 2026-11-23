// dte:C4
package io.github.bubba8587.bubbas_infused_gems;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.loot.GemLoot;
import io.github.bubba8587.bubbas_infused_gems.effect.WornEffects;
import io.github.bubba8587.bubbas_infused_gems.registry.ModAttachments;
import io.github.bubba8587.bubbas_infused_gems.registry.ModCommands;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.registry.ModCreativeTabs;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import io.github.bubba8587.bubbas_infused_gems.registry.ModRecipes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bubba's Infused Gems. */
public class BubbasInfusedGems implements ModInitializer {
	public static final String MOD_ID = "bubbas_infused_gems";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Balance balance = Balance.get();
		LOGGER.info("Infused Gems balance: cap {}, {} effects", balance.cap, balance.effects.size());
		ModComponents.initialize();
		ModAttachments.initialize();
		ModItems.initialize();
		ModEffects.initialize();
		ModRecipes.initialize();
		ModCreativeTabs.initialize();
		ModCommands.initialize();
		GemLoot.initialize();
		ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, previous, current) -> {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				WornEffects.refresh(entity);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> WornEffects.afterDamage(entity, source, taken));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> WornEffects.onDeath(entity));
	}
}

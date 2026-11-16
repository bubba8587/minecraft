// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;

/**
 * Wiring for the lurking drowned (specs/drowned.md): hits on a drowned feed its grip-break meter, a dying drowned lets
 * go, and tridents move into buried treasure (D6).
 */
public final class Drowneds {
	private Drowneds() {
	}

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseTaken, taken, blocked) -> {
			if (entity instanceof Drowned drowned && source.getEntity() != null && entity.level() instanceof ServerLevel level) {
				ItemStack weapon = source.getWeaponItem();
				if (weapon == null && source.getEntity() == source.getDirectEntity() && source.getEntity() instanceof LivingEntity living) {
					weapon = living.getMainHandItem();
				}
				DrownedAccess.mind(drowned).hurt(drowned, level, source.getEntity(), source.getDirectEntity(), weapon, baseTaken);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Drowned drowned) {
				DrownedAccess.mind(drowned).release(drowned, false);
			}
		});
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source != LootTableSource.REPLACED && key.equals(BuiltInLootTables.BURIED_TREASURE)) {
				builder.withPool(LootPool.lootPool().add(LootItem.lootTableItem(Items.TRIDENT))
						.when(LootItemRandomChanceCondition.randomChance(DrownedTuning.TREASURE_TRIDENT_CHANCE)));
			}
		});
	}
}

// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.EnumMap;
import java.util.Map;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.fabricmc.loader.api.FabricLoader;

/** Where relic map shreds are found, and the vaults' Infused Gems loot (only with that mod). */
public final class RelicLoot {
	public static final String GEMS_MOD = "bubbas_infused_gems";
	/** Infused Gems' rare gem table (gems and gemmed gear). */
	public static final ResourceKey<LootTable> GEMS_RARE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(GEMS_MOD, "gems/rare"));
	/** The vault guard's gemmed gear (loads only with Infused Gems). */
	public static final ResourceKey<LootTable> GUARD_GEAR = ResourceKey.create(Registries.LOOT_TABLE, BubbasTotemsArtifacts.id("relic_vault/guard_gear"));
	/** Rolls of Infused Gems' rare table added to each vault chest (min, max). */
	public static int[] gemRolls(RelicVaultPiece.Kind kind) {
		return switch (kind) {
			case GRAND -> new int[] { 1, 2 };
			case SMALL -> new int[] { 0, 2 };
			case CRYPT -> new int[] { 0, 1 };
		};
	}

	private RelicLoot() {
	}

	public static void initialize() {
		Map<ResourceKey<LootTable>, Float> shreds = RelicTuning.SHRED_LOOT;
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source == LootTableSource.REPLACED) {
				return;
			}
			Float chance = shreds.get(key);
			if (chance != null) {
				builder.withPool(LootPool.lootPool().add(LootItem.lootTableItem(Relics.RELIC_MAP_SHRED).apply(() -> new RelicShredFunction(java.util.Optional.empty())))
						.when(LootItemRandomChanceCondition.randomChance(chance)));
			}
		});
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!FabricLoader.getInstance().isModLoaded(GEMS_MOD)) {
				return;
			}
			for (RelicVaultPiece.Kind kind : RelicVaultPiece.Kind.values()) {
				if (table.is(kind.loot)) {
					int[] rolls = gemRolls(kind);
					LootTable gems = context.getLevel().getServer().reloadableRegistries().getLootTable(GEMS_RARE);
					int n = rolls[0] + context.getRandom().nextInt(rolls[1] - rolls[0] + 1);
					for (int i = 0; i < n; i++) {
						gems.getRandomItemsRaw(context, drops::add);
					}
				}
			}
		});
	}

	/** The vault guard's gemmed pieces by slot (empty without Infused Gems). */
	public static Map<EquipmentSlot, ItemStack> gemmedGear(WorldGenLevel level, RandomSource random) {
		Map<EquipmentSlot, ItemStack> gear = new EnumMap<>(EquipmentSlot.class);
		MinecraftServer server = level.getServer();
		if (server == null || !FabricLoader.getInstance().isModLoaded(GEMS_MOD)) {
			return gear;
		}
		LootTable table = server.reloadableRegistries().getLootTable(GUARD_GEAR);
		LootParams params = new LootParams.Builder(level.getLevel()).create(LootContextParamSets.EMPTY);
		for (ItemStack stack : table.getRandomItems(params, random)) {
			Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
			gear.put(equippable != null ? equippable.slot() : EquipmentSlot.MAINHAND, stack);
		}
		return gear;
	}
}

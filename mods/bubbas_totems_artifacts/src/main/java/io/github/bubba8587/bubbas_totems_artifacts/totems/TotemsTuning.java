// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.totems;

import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/** Totem numbers. */
public final class TotemsTuning {
	/** Held: dropped items within this many blocks fly to the holder. */
	public static final double GATHER_RADIUS = 8.0;
	/** Held: how fast pulled items fly. */
	public static final double GATHER_SPEED = 0.3;
	/** Held: at most this many items are pulled per tick. */
	public static final int GATHER_MAX_ITEMS_PER_TICK = 64;
	/** Beacon ring: radius and height above the beacon's base. */
	public static final double RING_RADIUS = 1.25;
	public static final double RING_HEIGHT = 1.05;
	/** Ticks per turn of the ring. */
	public static final int RING_PERIOD_TICKS = 100;
	/** Ring join speed and size limit. */
	public static final double RING_FLY_SPEED = 0.4;
	public static final int RING_MAX_MEMBERS = 48;
	/** Ticks between ring collection checks. */
	public static final int RING_COLLECT_TICKS = 10;
	/** XP release distance to a player. */
	public static final double RING_XP_RELEASE_DISTANCE = 2.5;
	/** Gathering statue radius. */
	public static final double GATHER_STATUE_RADIUS = 3.0;
	/** Held: blocks within this many blocks of the holder are not broken by explosions. */
	public static final double FORTIFY_RADIUS = 8.0;
	/** Held: the box whose crops and saplings grow faster. */
	public static final int GROWTH_RADIUS = 8;
	/** Held: extra random ticks, as a multiple of vanilla's rate. */
	public static final int GROWTH_HELD_EXTRA = 2;
	/** Beacon: extra random ticks per pyramid layer. */
	public static final int GROWTH_BEACON_EXTRA_PER_LAYER = 1;
	/** Upper bound on extra random-tick samples per area per tick. */
	public static final int GROWTH_MAX_SAMPLES_PER_TICK = 2048;
	/** Growth planting: check interval, chance, reach. */
	public static final int PLANT_CHECK_TICKS = 40;
	public static final float PLANT_CHANCE = 0.35F;
	public static final int PLANT_REACH = 3;
	/** How often a held Totem of Seeking asks for its target. */
	public static final int SEEK_UPDATE_TICKS = 20;
	/** Search radius in structure-spacing steps, like /locate and explorer maps. */
	public static final int SEEK_MAX_RINGS = 100;
	/** Search time per server tick, all searches together. */
	public static final long SEEK_BUDGET_NANOS = 2_000_000L;
	/** Results are cached per area of this size: moving into a new one searches again. */
	public static final int SEEK_CELL_BLOCKS = 256;
	/** Cached results kept. */
	public static final int SEEK_CACHE_SIZE = 512;
	/** Used: the ground within this many blocks goes back to how it generated. */
	public static final int BEGINNING_RADIUS = 24;
	/** Used: the ripple spreads one block every this many ticks. */
	public static final int BEGINNING_TICKS_PER_BLOCK = 2;
	/** Used: teleports to the block aimed at within this many blocks of the eyes. */
	public static final double END_RANGE = 32.0;
	/** Used: the cooldown after a teleport, shown with the item cooldown sweep. */
	public static final int END_COOLDOWN_TICKS = 80;
	/** Pasture: range of a herder who can lead livestock out. */
	public static final double HERD_LEAD_OUT_RANGE = 10.0;
	/** Statue area radius. */
	public static final int STATUE_RADIUS = 3;
	/** Warding statue ward radius (keeps sleepers asleep). */
	public static final int WARD_STATUE_RADIUS = 5;
	/** Statue pulse interval. */
	public static final int STATUE_PULSE_TICKS = 20;
	/** Growth statue extra random ticks. */
	public static final int GROWTH_STATUE_EXTRA = 1;
	/** A beacon's Fortification or Growth area lasts this long after its last pulse. */
	public static final int AREA_LIFETIME_TICKS = 100;
	/** A compass pointed by a Seeking beacon keeps pointing this long after the last pulse that reached it. */
	public static final int SEEK_COMPASS_LIFETIME_TICKS = 100;
	/** Totem of Herding loot. */
	public static final Map<ResourceKey<LootTable>, Float> HERDING_LOOT = Map.of(
			BuiltInLootTables.VILLAGE_SHEPHERD, 0.12F,
			BuiltInLootTables.VILLAGE_BUTCHER, 0.08F,
			BuiltInLootTables.VILLAGE_TANNERY, 0.08F);
	/** Totem of Clear Skies loot. */
	public static final Map<ResourceKey<LootTable>, Float> CLEAR_SKIES_LOOT = Map.of(
			BuiltInLootTables.IGLOO_CHEST, 0.12F,
			BuiltInLootTables.VILLAGE_DESERT_HOUSE, 0.06F,
			BuiltInLootTables.SHIPWRECK_SUPPLY, 0.05F);
	/** Totem of Keeping: rare and deep. */
	public static final Map<ResourceKey<LootTable>, Float> KEEPING_LOOT = Map.of(
			BuiltInLootTables.ANCIENT_CITY, 0.06F,
			BuiltInLootTables.END_CITY_TREASURE, 0.08F,
			BuiltInLootTables.STRONGHOLD_LIBRARY, 0.10F,
			BuiltInLootTables.WOODLAND_MANSION, 0.08F);
	/** Totem of Gathering: the collectors' haunts. */
	public static final Map<ResourceKey<LootTable>, Float> GATHERING_LOOT = Map.of(
			BuiltInLootTables.ABANDONED_MINESHAFT, 0.06F,
			BuiltInLootTables.SIMPLE_DUNGEON, 0.08F,
			BuiltInLootTables.PILLAGER_OUTPOST, 0.12F);
	/** Totem of Fortification: places built against blasts. */
	public static final Map<ResourceKey<LootTable>, Float> FORTIFICATION_LOOT = Map.of(
			BuiltInLootTables.DESERT_PYRAMID, 0.15F,
			BuiltInLootTables.BASTION_OTHER, 0.08F,
			BuiltInLootTables.STRONGHOLD_CROSSING, 0.08F);
	/** Totem of Growth: overgrown places and ships' stores of seed. */
	public static final Map<ResourceKey<LootTable>, Float> GROWTH_LOOT = Map.of(
			BuiltInLootTables.JUNGLE_TEMPLE, 0.20F,
			BuiltInLootTables.SHIPWRECK_SUPPLY, 0.06F,
			BuiltInLootTables.IGLOO_CHEST, 0.15F);
	/** Totem of Seeking: the map-makers' chests. */
	public static final Map<ResourceKey<LootTable>, Float> SEEKING_LOOT = Map.of(
			BuiltInLootTables.SHIPWRECK_MAP, 0.12F,
			BuiltInLootTables.BURIED_TREASURE, 0.10F,
			BuiltInLootTables.UNDERWATER_RUIN_BIG, 0.08F,
			BuiltInLootTables.VILLAGE_CARTOGRAPHER, 0.10F);
	/** Totem of the End loot. */
	public static final Map<ResourceKey<LootTable>, Float> END_LOOT = Map.of(
			BuiltInLootTables.END_CITY_TREASURE, 0.05F);

	/** Grave Talisman in Bubba's Reworked Mobs' crypts. */
	public static final Map<ResourceKey<LootTable>, Float> TALISMAN_LOOT = Map.of(
			ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
					net.minecraft.resources.Identifier.fromNamespaceAndPath("bubbas_reworked_mobs", "chests/crypt_family")), 0.15F,
			ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
					net.minecraft.resources.Identifier.fromNamespaceAndPath("bubbas_reworked_mobs", "chests/crypt_noble")), 0.25F);

	// --- Totem of the Beginning pulse --------------------------------------------------------------------
	/** Pulse: ground within this many blocks (a cylinder, as far up and down). */
	public static final int BEGINNING_PULSE_RADIUS = 8;
	/** Pulse: the ripple spreads one block every this many ticks (8 blocks in about 3 s). */
	public static final int BEGINNING_PULSE_TICKS_PER_BLOCK = 7;
	/** Pulse: cooldown after a use, shown with the item cooldown sweep. */
	public static final int BEGINNING_PULSE_COOLDOWN_TICKS = 1200;
	/** Pulse: chance each eligible block goes back. */
	public static final float BEGINNING_PULSE_CHANCE = 0.12F;
	/** Pulse: chance a mined-out ore spot gets its ore back (a tenth of terrain's). */
	public static final float BEGINNING_PULSE_ORE_CHANCE = 0.012F;
	/** Pulse: a block a pulse reverted can't be reverted again for this long (10 minutes). */
	public static final int BEGINNING_PULSE_RECENT_TICKS = 12000;

	private TotemsTuning() {
	}
}

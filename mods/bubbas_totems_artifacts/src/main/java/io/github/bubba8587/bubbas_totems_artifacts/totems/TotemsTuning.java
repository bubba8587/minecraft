// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.totems;

import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Numbers for the five totems added 2026-11-16 (specs/totems-artifacts.md, "The five new totems"): Keeping, Gathering,
 * Fortification, Growth, Seeking. Starting values, owner to tune; one place for all of them. (Warding keeps its own
 * {@code warding/WardingTuning}.)
 */
public final class TotemsTuning {
	// --- Gathering -------------------------------------------------------------------------------------
	/** Held: dropped items within this many blocks fly to the holder (vanilla's XP orbs follow a player from 8). */
	public static final double GATHER_RADIUS = 8.0;
	/** Held: at most this many items are pulled per tick (the nearest are not sorted; it only bounds the work). */
	public static final int GATHER_MAX_ITEMS_PER_TICK = 64;
	/** Beacon: at most this many items (and as many XP orbs) move per pulse. */
	public static final int GATHER_BEACON_MAX_PER_PULSE = 256;
	/** Beacon: how far below the beacon's bottom pyramid layer a chest or barrel still counts as "under" it. */
	public static final int GATHER_CHEST_DEPTH_BELOW_PYRAMID = 6;

	// --- Fortification ---------------------------------------------------------------------------------
	/** Held: blocks within this many blocks of the holder are not broken by explosions. */
	public static final double FORTIFY_RADIUS = 8.0;

	// --- Growth ----------------------------------------------------------------------------------------
	/** Held: the box (this many blocks each way from the holder's block) whose crops and saplings grow faster. */
	public static final int GROWTH_RADIUS = 8;
	/**
	 * Held: extra random ticks on top of vanilla's, as a multiple of vanilla's rate (random_tick_speed): 2 makes
	 * crops in range grow about three times as fast.
	 */
	public static final int GROWTH_HELD_EXTRA = 2;
	/** Beacon: extra random ticks per pyramid layer (1 layer: twice as fast, 4 layers: five times). */
	public static final int GROWTH_BEACON_EXTRA_PER_LAYER = 1;
	/** Upper bound on extra random-tick samples per area per tick (a full 4-layer sky beacon needs about 2000). */
	public static final int GROWTH_MAX_SAMPLES_PER_TICK = 2048;

	// --- Seeking ---------------------------------------------------------------------------------------
	/** How often (ticks) a held Totem of Seeking asks for its target. */
	public static final int SEEK_UPDATE_TICKS = 20;
	/** Search radius in structure-spacing steps, like /locate and explorer maps (100). */
	public static final int SEEK_MAX_RINGS = 100;
	/** Search time per server tick, all searches together (nanoseconds; 2 ms). */
	public static final long SEEK_BUDGET_NANOS = 2_000_000L;
	/** Results are cached per area of this size (blocks, a power of two): moving into a new one searches again. */
	public static final int SEEK_CELL_BLOCKS = 256;
	/** Cached results kept (oldest dropped first). */
	public static final int SEEK_CACHE_SIZE = 512;

	// --- beacons ---------------------------------------------------------------------------------------
	/** A beacon's Fortification or Growth area lasts this long after its last pulse (one pulse, 80, plus a second). */
	public static final int AREA_LIFETIME_TICKS = 100;
	/** A compass pointed by a Seeking beacon keeps pointing this long after the last pulse that reached it. */
	public static final int SEEK_COMPASS_LIFETIME_TICKS = 100;

	// --- where they are found (additive pools, one totem at this chance per chest) -------------------
	/** Totem of Keeping: rare and deep (a Totem of Undying for your things). */
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
	/** Totem of Fortification: places built against blasts (the desert pyramid's TNT trap, bastions, strongholds). */
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

	private TotemsTuning() {
	}
}

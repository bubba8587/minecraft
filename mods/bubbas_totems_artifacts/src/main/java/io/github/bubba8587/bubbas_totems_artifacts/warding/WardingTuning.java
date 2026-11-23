// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.warding;

import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/** Numbers for Warding (specs/undead.md "Warding"). Starting values, owner to tune. One place for all of them. */
public final class WardingTuning {
	// --- the held Totem of Warding ---------------------------------------------------------------
	/** Ticks between pulses while held (10 s). Shown as vanilla's item cooldown sweep on the slot. */
	public static final int PULSE_INTERVAL_TICKS = 200;
	/** Monsters within this many blocks of the holder flee at each pulse. */
	public static final double PULSE_RADIUS = 8.0;
	/** How long they flee (ticks, about 5 s). */
	public static final int FLEE_TICKS = 100;
	/** A fleeing monster keeps running while closer than this to the holder (a cat scares creepers within 6). */
	public static final double FLEE_KEEP_AWAY = 12.0;
	/** Positions tried per pick: fleeing takes the farthest from the holder, leaving a ward the first that gets out. */
	public static final int FLEE_TRIES = 6;
	/** Flee speed (vanilla creeper-from-cat: 1.0 walking, 1.2 when the cat is within 7 blocks). */
	public static final double FLEE_WALK_SPEED = 1.0;
	public static final double FLEE_SPRINT_SPEED = 1.2;
	/** Sleepers within PULSE_RADIUS (Reworked Mobs) lose this many disturbance points at each pulse (the wake threshold is 100). */
	public static final float PULSE_CALM_POINTS = 40.0F;

	// --- wards (a Totem of Warding in a beacon) --------------------------------------------------
	/** Every this many ticks, monsters found inside a ward are sent out of it. */
	public static final int WARD_SWEEP_TICKS = 20;
	/** A monster inside a ward with no way out (an enclosed room) stops trying to leave, and isn't pushed, for this long, then looks again (10 s). */
	public static final int TRAPPED_RECHECK_TICKS = 200;
	/**
	 * A ward lasts this long after its source last refreshed it, so a ward whose beacon was unloaded or broken lapses.
	 * One beacon pulse (80 ticks) plus a second of slack.
	 */
	public static final int WARD_DEFAULT_LIFETIME_TICKS = 100;

	// --- where the Totem of Warding is found (additive pools; Totem of Undying: evokers only, 1 each) ---
	/** Chance per roll of each table to add one Totem of Warding. Chests are the main source; evokers rarely carry one (owner). */
	public static final Map<ResourceKey<LootTable>, Float> LOOT = Map.of(
			ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, net.minecraft.resources.Identifier.withDefaultNamespace("entities/evoker")), 0.05F,
			BuiltInLootTables.WOODLAND_MANSION, 0.20F,
			BuiltInLootTables.ANCIENT_CITY, 0.12F,
			BuiltInLootTables.TRIAL_CHAMBERS_REWARD, 0.08F,
			BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, 0.15F,
			// Bubba's Reworked Mobs' noble crypts (specs/graveyards.md), keyed by id: without that mod the table never exists.
			ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
					net.minecraft.resources.Identifier.fromNamespaceAndPath("bubbas_reworked_mobs", "chests/crypt_noble")), 0.12F);

	private WardingTuning() {
	}
}

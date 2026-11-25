// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

/**
 * Every Wither Captain, Soulfire Blaze and Captain's Helm number the owner tunes by playing (specs/wither-captain.md).
 * Starting values are the builder's.
 */
public final class CaptainTuning {
	private CaptainTuning() {
	}

	// --- the captain ----------------------------------------------------------------------------------

	/** Wither skeleton: 20 health. */
	public static final double CAPTAIN_HEALTH = 80.0;
	/** Wither skeleton: 4 (set in its finalizeSpawn), plus the stone sword. */
	public static final double CAPTAIN_ATTACK_DAMAGE = 5.0;
	/** Size against a wither skeleton (the scale attribute: model and hitbox). */
	public static final double CAPTAIN_SCALE = 1.1;
	public static final double CAPTAIN_KNOCKBACK_RESISTANCE = 0.3;
	public static final double CAPTAIN_FOLLOW_RANGE = 24.0;
	/** A player this close (blocks) sees the boss bar. */
	public static final double BOSS_BAR_RANGE = 32.0;
	/** A targeted player this close (blocks) counts as the captain's first fight. */
	public static final double ENGAGE_RANGE = 16.0;

	// --- the summoning phase ---------------------------------------------------------------------------

	/** Hits taken in the normal phase before it summons again. */
	public static final int HITS_TO_SUMMON = 4;
	/** Soulfire Blazes per phase, uniform between these. */
	public static final int BLAZES_MIN = 1;
	public static final int BLAZES_MAX = 3;
	/** Ticks from the phase starting to the first blaze, and between blazes. */
	public static final int FIRST_SUMMON_DELAY = 30;
	public static final int SUMMON_INTERVAL = 50;
	/** Ticks it stays in the phase after the last blaze. */
	public static final int LINGER_AFTER_LAST = 60;
	/** Damage it takes in the phase, as a share of normal. */
	public static final float SUMMONING_DAMAGE_FACTOR = 0.25F;
	/** No more summons while this many of its blazes are alive. */
	public static final int MAX_LIVE_BLAZES = 4;
	/** Summoned blazes appear this far out (horizontal, blocks) and this far up. */
	public static final double SUMMON_RADIUS_MIN = 2.0;
	public static final double SUMMON_RADIUS_MAX = 3.5;
	public static final double SUMMON_HEIGHT = 1.5;
	/** Its summoned blazes are looked for this far out (burning out on its death, the live count). */
	public static final double BLAZE_LINK_RANGE = 48.0;

	// --- loot ------------------------------------------------------------------------------------------
	// Shards 1-2 and the helm 1 in 4: data/bubbas_reworked_mobs/loot_table/entities/wither_captain.json.

	// --- natural spawning ------------------------------------------------------------------------------

	/** Chance a natural fortress wither skeleton spawn is a captain instead. */
	public static final float FORTRESS_CAPTAIN_CHANCE = 1.0F / 50.0F;
	/** Never a natural captain within this many blocks of another. */
	public static final double CAPTAIN_SPACING = 48.0;
	/** Soulfire Blaze weight in the soul sand valley's monster spawns (skeleton 20, ghast 50, enderman 1). */
	public static final int SOULFIRE_BLAZE_VALLEY_WEIGHT = 2;

	// --- Soulfire Blazes -------------------------------------------------------------------------------

	/** A blaze fireball does 5. */
	public static final float CHARGE_DAMAGE = 7.5F;
	/** Seconds a charge sets its target alight (a blaze fireball: 5). */
	public static final float CHARGE_BURN_SECONDS = 5.0F;
	/** How often (ticks) a summoned blaze checks its captain is still alive. */
	public static final int BLAZE_LINK_CHECK = 20;
}

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks;

/** Every number the owner tunes by playing. */
public final class WorldTweaksTuning {
	private WorldTweaksTuning() {
	}

	/** How deep under a lava surface a magma block still makes it bubble. */
	public static final int MAGMA_DEPTH = 4;
	/** Chance per lava display tick that a bubble rises. */
	public static final float BUBBLE_CHANCE = 0.06F;
	/** A bubble swells for this many ticks (plus up to BUBBLE_LIFE_SPREAD more), then pops over POP_TICKS. */
	public static final int BUBBLE_SWELL_TICKS = 50;
	public static final int BUBBLE_LIFE_SPREAD = 30;
	public static final int BUBBLE_POP_TICKS = 6;
	/** The plop. */
	public static final float PLOP_PITCH = 0.5F;
	public static final float PLOP_VOLUME = 0.55F;

	/** Eruption checks: how often, and how far around each player. */
	public static final int GEYSER_CHECK_TICKS = 40;
	public static final int GEYSER_SEARCH_RADIUS = 24;
	/** Every magma block under lava is a vent. Rhythm, in seconds: quiet for QUIET_BASE + up to QUIET_SPREAD (fixed per vent) + QUIET_PER_DEPTH per extra lava block. */
	public static final int VENT_QUIET_BASE = 80;
	public static final int VENT_QUIET_SPREAD = 100;
	public static final int VENT_QUIET_PER_DEPTH = 5;
	public static final long VENT_SALT = 0x6C617661L;
	/** An eruption lasts this long. */
	public static final int GEYSER_DURATION_TICKS = 60;
	/** The column that lifts. */
	public static final double GEYSER_HEIGHT = 14.0;
	public static final double GEYSER_RADIUS = 0.8;

	/** Lava geysers push up like vanilla's sulfur geysers. */
	public static final double LIFT_PUSH = 0.2;
	public static final double LIFT_MAX_RISE = 0.9;
	/** The speed boost (both geysers). */
	public static final double BOOST_FACTOR = 1.06;
	public static final double BOOST_MAX = 1.4;
	/** How far down a player looks for an erupting sulfur geyser under them. */
	public static final int SULFUR_SEARCH_DEPTH = 16;

	// --- Parasitic (the per-level health and repair caps are in the enchantment's data file) ---
	/** After a bite, the next can't come for this many ticks (plus up to PARASITIC_COOLDOWN_SPREAD more). */
	public static final int PARASITIC_COOLDOWN_TICKS = 600;
	public static final int PARASITIC_COOLDOWN_SPREAD = 1200;
	/** Once the cooldown is over, the chance each second that it bites (about 20 s on average). */
	public static final float PARASITIC_BITE_CHANCE = 0.05F;
	/** A bite takes this share of the level's caps at least (up to all of them), health and repair alike. */
	public static final float PARASITIC_MIN_SHARE = 0.25F;

	// --- Amethyst Golem ---
	/** Longest recording, in ticks (5 minutes), and the most notes it keeps. */
	public static final int GOLEM_RECORD_CAP_TICKS = 6000;
	public static final int GOLEM_MAX_NOTES = 4096;
	/** Songs start only when the day clock is a multiple of this (1 in-game hour). */
	public static final int GOLEM_BEAT_TICKS = 1000;
	/** Wind chime: vibrations at or above this vanilla frequency (1-15) chime; how far it feels them (the sculk sensor's 8). */
	public static final int GOLEM_CHIME_FLOOR = 2;
	public static final int GOLEM_CHIME_RADIUS = 8;
	/** How far it looks for a free jukebox, and how often (ticks). */
	public static final int GOLEM_JUKEBOX_RADIUS = 16;
	public static final int GOLEM_JUKEBOX_CHECK_TICKS = 100;
	/** Loudness of its chimes (1 = heard 16 blocks away). */
	public static final float GOLEM_CHIME_VOLUME = 1.0F;
}
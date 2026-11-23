// dte:C8
package io.github.bubba8587.bubbas_world_tweaks;

/** Every number the owner tunes by playing. */
public final class WorldTweaksTuning {
	private WorldTweaksTuning() {
	}

	/** How deep under a lava surface a magma block still makes it bubble. */
	public static final int MAGMA_DEPTH = 4;
	/** Chance per lava display tick that a bubble rises. */
	public static final float BUBBLE_CHANCE = 0.18F;
	/** A bubble swells for this many ticks (plus up to BUBBLE_LIFE_SPREAD more), then pops over POP_TICKS. */
	public static final int BUBBLE_SWELL_TICKS = 16;
	public static final int BUBBLE_LIFE_SPREAD = 8;
	public static final int BUBBLE_POP_TICKS = 5;
	/** The plop. */
	public static final float PLOP_PITCH = 0.5F;
	public static final float PLOP_VOLUME = 0.55F;

	/** Each player checks for eruptions this often, sampling this many columns this far around them. */
	public static final int GEYSER_CHECK_TICKS = 40;
	public static final int GEYSER_COLUMNS_PER_CHECK = 6;
	public static final int GEYSER_SEARCH_RADIUS = 24;
	/** Chance a bubbling spot found by a check erupts. */
	public static final float GEYSER_CHANCE = 0.08F;
	/** An eruption lasts this long, and no two erupt this close together. */
	public static final int GEYSER_DURATION_TICKS = 100;
	public static final double GEYSER_SPACING = 8.0;
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
}

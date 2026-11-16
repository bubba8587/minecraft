// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

/** Every number of the Ancient Fabricators' mechanics (specs/mob-factory.md; F5: starting values, owner playtests). */
public final class FabricatorTuning {
	/** It runs while a player (not a spectator) is within this of the pad. */
	public static final int ACTIVE_RANGE = 64;
	/** Each of the 8 timer lamps lights after this long: a full count is 16 s. */
	public static final int STEP_TICKS = 40;
	/** Lamps in the ring: when all are lit, the lift starts. */
	public static final int TIMER_LAMPS = 8;
	/** Levitation before it fires: whatever is on the pad rises a little. */
	public static final int LIFT_TICKS = 20;
	/** The dripper drops one mob at the start of each count (so one per 17 s while running). */
	public static final int DROP_EVERY_TICKS = STEP_TICKS * TIMER_LAMPS + LIFT_TICKS;
	/** Deactivated mobs on the deck before the dripper stops dropping (they pile up while the teleporter is stopped). */
	public static final int PILE_CAP = 12;
	/** Teleport destinations: within this sideways of the pad, on the surface (natural-spawn rules for mobs). */
	public static final int SURFACE_RANGE = 96;
	/** Tries for a destination each time it fires; a mob with none stays on the pad for the next count. */
	public static final int SURFACE_TRIES = 24;
	/** No mob arrives within this of a player (vanilla's natural-spawn rule). */
	public static final int NO_PLAYER_RANGE = 24;
	/** A destination must be at least this far above the pad ("far up"). */
	public static final int MIN_RISE = 16;
	/** An arrived mob lies limp in its particles this long, then wakes. */
	public static final int WAKE_TICKS = 40;
	/** Debuffs: every 4 s (a beacon pulse), lasting 13 s (a 1-layer beacon's length). */
	public static final int DEBUFF_EVERY_TICKS = 80;
	public static final int DEBUFF_TICKS = 260;
	/** The debuff box without Reworked Beacons: a 1-layer pair's (60 sideways; 10 past both pyramids). */
	public static final int DEBUFF_RANGE = 60;
	/** Mining Fatigue III (like an elder guardian), Weakness I, Slowness I. */
	public static final int FATIGUE_AMPLIFIER = 2;
	/** The Warden hears a player this close to the hidden spawner, the ceiling beacon or the floor beacon. */
	public static final double HEAR_SPAWNER = 6.0;
	public static final double HEAR_CEILING_BEACON = 5.0;
	public static final double HEAR_FLOOR_BEACON = 4.0;
	/** ... or up in the roof: this high over the deck, within this of the pad sideways. */
	public static final int HEAR_ROOF_HEIGHT = 9;
	public static final int HEAR_ROOF_RADIUS = 16;
	/** Anger a heard player gets (vanilla's angry level is 80). */
	public static final int WAKE_ANGER = 100;

	private FabricatorTuning() {
	}
}

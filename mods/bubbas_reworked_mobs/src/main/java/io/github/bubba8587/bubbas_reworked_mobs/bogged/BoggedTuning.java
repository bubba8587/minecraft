// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.bogged;

/** Numbers for the lurking bogged (bogged/BoggedLurk). Builder's starting values, owner to playtest. */
public final class BoggedTuning {
	/** A bogged notices players this far away and starts looking for a hiding spot near them. */
	public static final double NOTICE_RANGE = 32.0;
	/** Hiding spots: random columns this many blocks from the player... */
	public static final double SPOT_MIN = 5.0;
	public static final double SPOT_MAX = 14.0;
	/** ...ideally this far... */
	public static final double SPOT_BEST = 8.0;
	/** ...and at most this far from the bogged. */
	public static final double SPOT_TRAVEL = 24.0;
	public static final int SPOT_COLUMNS = 40;
	/** Spots are searched this many blocks above and below the player's feet. */
	public static final int SPOT_HEIGHT = 6;
	/** It gives up getting to a spot after this long (ticks; 30 s). */
	public static final int TRAVEL_TIMEOUT = 600;
	/** It ambushes a player within this range whose back is turned... */
	public static final double AMBUSH_RANGE = 10.0;
	/** ...or anyone this close. */
	public static final double TOO_CLOSE = 4.0;
	/** "Back turned": the bogged is outside this half-angle of where the player looks (100 degrees off their view). */
	public static final double BACK_TURNED_HALF_ANGLE = 100.0;
	/** "Unseen": nobody looks within this half-angle of it (about the screen's edge, as for the drowned). */
	public static final double UNSEEN_HALF_ANGLE = 70.0;
	/** Unseen, it slips to a new spot with 1 in this chance a tick (about every 30 s)... */
	public static final int MOVE_ONE_IN = 600;
	/** ...or 1 in this (about every 4 s) once its player is farther than FOLLOW_DISTANCE from its spot. */
	public static final int MOVE_ONE_IN_FOLLOWING = 80;
	public static final double FOLLOW_DISTANCE = 12.0;
	/** After a fight, with no target for this long (ticks; 10 s), it goes back to hiding. */
	public static final int HUNT_LINGER = 200;
	/** Entity tag of a bogged that stays vanilla (from a spawner or trial spawner). */
	public static final String VANILLA_TAG = "bubbas_reworked_mobs.vanilla_bogged";

	private BoggedTuning() {
	}
}

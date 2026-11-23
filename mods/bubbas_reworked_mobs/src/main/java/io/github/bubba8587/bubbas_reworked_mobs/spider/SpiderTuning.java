// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spider;

/**
 * Every spider number the owner tunes by playing (specs/spiders.md). Starting values are the builder's; the spec's
 * "Open" list names them.
 */
public final class SpiderTuning {
	private SpiderTuning() {
	}

	// --- cave nests: worldgen -------------------------------------------------------------------------

	/** Chance a chunk tries for a nest at all ("decently common": about one chunk in six). */
	public static final float NEST_CHUNK_CHANCE = 0.17F;
	/** Columns tried per chunk to find the top of a tall room. */
	public static final int NEST_COLUMN_TRIES = 6;
	/** Search band for cave ceilings. */
	public static final int NEST_MIN_Y = -52;
	public static final int NEST_MAX_Y = 56;
	/** "Tall room": at least this much open air straight under the ceiling ("a long drop"). */
	public static final int NEST_MIN_DROP = 10;
	/** The ceiling must be open this far out on all four sides (the top of a room, not a shaft). */
	public static final int NEST_ROOM_HALF_WIDTH = 3;
	/** Cobwebs per nest, and how far they spread from the centre. */
	public static final int NEST_WEBS_MIN = 9;
	public static final int NEST_WEBS_MAX = 15;
	public static final int NEST_RADIUS = 3;
	/** Sleepers a new nest starts with, and the most it ever holds. */
	public static final int NEST_SLEEPERS_MIN = 2;
	public static final int NEST_SLEEPERS_MAX = 3;
	public static final int NEST_SLEEPER_CAP = 4;

	// --- cave nests: the ambush -----------------------------------------------------------------------

	/** The nest looks for players below it every this many ticks. */
	public static final int NEST_SCAN_TICKS = 4;
	/** A player within this horizontal distance of the nest's centre, anywhere below it, sets off a drop. */
	public static final double TRIGGER_RADIUS = 2.5;
	/** Ticks between two drops from the same nest. */
	public static final int DROP_COOLDOWN_TICKS = 40;
	/** Refill: checked every this many ticks, with this chance, only with no player within REFILL_QUIET_RADIUS. */
	public static final int REFILL_CHECK_TICKS = 200;
	public static final float REFILL_CHANCE = 0.04F;
	public static final double REFILL_QUIET_RADIUS = 12.0;
	/** A nest with fewer cobwebs than this left around it is gone (mined out). */
	public static final int NEST_MIN_WEBS_ALIVE = 3;

	/** Drop speed, blocks a tick (straight down, no steering: it has to actually hit). */
	public static final double DROP_SPEED = 0.9;
	/** A miss stops this far above the floor and hangs on its thread this long before the thread snaps (owner: about a second). */
	public static final double DANGLE_ABOVE_FLOOR = 1.0;
	public static final int MISS_DANGLE_TICKS = 20;
	/** Damage of the landing bite. */
	public static final float DROP_DAMAGE = 4.0F;

	/** Reel speed up the thread, blocks a tick. */
	public static final double REEL_SPEED = 0.12;

	// --- the grip (adapted from the drowned grab, specs/drowned.md "Danger rework") --------------------

	/** Grip starts here, rises each tick while it holds, and lets go at 0. Hits loosen it (DrownedMind.gripPoints). */
	public static final float GRIP_START = 50.0F;
	public static final float GRIP_FULL = 100.0F;
	public static final float GRIP_RISE_PER_TICK = 0.2F;
	/** Its bites while it holds: chance a tick and damage, from a fresh grip to a full one. */
	public static final float BITE_CHANCE_PER_TICK = 0.03F;
	public static final float BITE_CHANCE_PER_TICK_FULL = 0.08F;
	public static final float BITE_DAMAGE = 2.0F;
	public static final float BITE_DAMAGE_FULL = 4.0F;
	/** Push on letting go. */
	public static final double RECOIL_SPEED = 0.3;
}

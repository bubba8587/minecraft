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

	// --- giant spiders (old-growth taiga) --------------------------------------------------------------

	/** Size: vanilla's scale attribute ("not too giant", 1.5-2x). */
	public static final double GIANT_SCALE = 1.75;
	public static final double GIANT_HEALTH = 40.0;
	public static final double GIANT_ATTACK = 5.0;
	public static final double GIANT_SPEED = 0.3;
	/** Spawning: weight among the biome's monsters, and never within this many blocks of another giant. */
	public static final int GIANT_SPAWN_WEIGHT = 12;
	public static final double GIANT_SPACING = 48.0;

	/** Livestock: how far it notices an animal to snatch, and how close an animal must be to lure it off a player. */
	public static final double SNATCH_RANGE = 16.0;
	public static final double LURE_RADIUS = 10.0;
	/** A player who hurt it in the last this many ticks isn't dropped for livestock. */
	public static final int LURE_IGNORED_AFTER_HURT_TICKS = 100;

	/** Hauling: where it takes a catch (its own webs, or a tree), climb speed, and how long it feeds. */
	public static final double LARDER_SEARCH = 14.0;
	public static final int LARDER_HEIGHT_MIN = 4;
	public static final int LARDER_HEIGHT_MAX = 7;
	public static final double CLIMB_SPEED = 0.15;
	public static final int HAUL_GIVE_UP_TICKS = 300;
	public static final int FEED_TICKS = 300;
	public static final int FEED_BITE_TICKS = 30;
	public static final float FEED_BITE_DAMAGE = 2.0F;

	/** Spinning: every 400-800 ticks while roaming, an orb web between two trunks 3-7 apart. */
	public static final int SPIN_INTERVAL_MIN = 400;
	public static final int SPIN_INTERVAL_MAX = 800;
	public static final int SPIN_SEARCH = 8;
	public static final int SPAN_MIN = 3;
	public static final int SPAN_MAX = 7;
	/** Cap: no spinning when this many spun webs are already within AREA_RADIUS (so a forest isn't buried). */
	public static final int AREA_WEB_CAP = 5;
	public static final int AREA_RADIUS = 12;
	/** Webs (larder sites) one giant remembers. */
	public static final int SITES_REMEMBERED = 8;

	/** Little spiders: summoned when it's hurt, this many, at most this many alive near it, then a cooldown. */
	public static final int SPIDERLINGS_PER_CALL = 2;
	public static final int SPIDERLINGS_CAP = 4;
	public static final int SPIDERLING_COOLDOWN_TICKS = 200;
	public static final double SPIDERLING_SCALE = 0.5;
	public static final double SPIDERLING_HEALTH = 6.0;
	public static final double SPIDERLING_ATTACK = 1.0;

	// --- spun webs (owner, 2026-11-22: "something really outstanding looking") ---------------------------

	/** Movement through the strands is multiplied by this (a cobweb block is 0.25): a little slower. */
	public static final double WEB_SLOW = 0.7;
	/** Hits to bring one down, how torn each hit leaves it, and string from a clean cut with shears. */
	public static final int WEB_HITS_TO_CUT = 3;
	public static final float WEB_TEAR_PER_HIT = 0.25F;
	public static final int WEB_SHEAR_STRING = 2;
	/** Ageing: torn up to WEB_AGE_TEAR_MAX over this many ticks (five in-game days). */
	public static final int WEB_AGE_TICKS_TO_TATTERS = 24000 * 5;
	public static final float WEB_AGE_TEAR_MAX = 0.45F;
	/** The tangle (owner: thick strands criss-crossing between many random points). Shortest strand. */
	public static final double WEB_STRAND_MIN = 2.5;
	/** Between trees: rays cast for anchors, how far they reach, strands strung. */
	public static final int TREE_WEB_RAYS = 22;
	public static final double TREE_WEB_REACH = 7.0;
	public static final int TREE_WEB_STRANDS = 14;
	/** Cobweb blocks where its busiest strands meet the trunks (ties the strands to Minecraft's own web). */
	public static final int TREE_WEB_TIE_OFFS = 3;
	/** Round a nest's cobweb core: one tangle reaching the ceiling and upper walls. */
	public static final int NEST_SPUN_WEBS = 1;
	public static final int NEST_WEB_RAYS = 26;
	public static final double NEST_WEB_REACH = 11.0;
	public static final int NEST_WEB_STRANDS = 16;
}

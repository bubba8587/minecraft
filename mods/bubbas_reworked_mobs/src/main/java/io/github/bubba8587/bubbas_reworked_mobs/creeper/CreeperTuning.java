// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

/**
 * Numbers for the nervous-watcher creeper (specs/creepers.md). One place to tweak them. Every value here is a starting
 * value picked by the builder (2026-11-16), owner to playtest and tune, unless it says "vanilla".
 * Distances in blocks, speeds in blocks per tick (a player walks 0.216, sprints 0.28), times in ticks (20 a second).
 */
public final class CreeperTuning {
	// --- the startle meter ---------------------------------------------------------------------
	/** A full meter starts the hiss. */
	public static final float METER_FULL = 100.0F;
	/** Points lost per tick while nothing startles it (10 a second: one hit's worth calms in 4 s). */
	public static final float METER_DECAY_PER_TICK = 0.5F;
	/** One attack (any damage with an attacker behind it). Three sword hits in a row (0.6 s apart) set it off; one or two calm down. */
	public static final float ATTACK_POINTS = 40.0F;
	/** Each motion scan, per living thing within FAST_RANGE moving faster than FAST_SPEED. Sprinting circles fill it in about 3.3 s. */
	public static final float FAST_MOVE_POINTS = 8.0F;
	/** A projectile passing within NEAR_MISS_RADIUS (once per projectile). Four snowballs in a row set it off. */
	public static final float NEAR_MISS_POINTS = 25.0F;
	/** What the meter drops to when the hiss is defused: still jumpy, but it has to be startled again to re-light. */
	public static final float DEFUSED_METER = 60.0F;

	// --- motion senses -------------------------------------------------------------------------
	/** Fast movement this close startles (owner: "about 6 blocks"). Needs line of sight, not the field of view. */
	public static final double FAST_RANGE = 6.0;
	/** "Faster than a walk": between a player's walk (0.216) and sprint (0.28). Averaged over one scan. */
	public static final double FAST_SPEED = 0.24;
	/** Vertical movement counts this much toward the speed (so jumping on the spot isn't a sprint, but a falling or launched body is). */
	public static final double VERTICAL_SPEED_WEIGHT = 0.5;
	/** Anything moving at least this fast (and in its field of view, in sight) can catch its eye. */
	public static final double NOTICE_SPEED = 0.03;
	/** It notices motion and keeps watching a player this far (owner, 2026-11-16: they stay farther away; was vanilla's 16). */
	public static final double WATCH_RANGE = 24.0;
	/** Its navigation may plan paths this long (vanilla: its follow range, 16), so it can reach cover far off. Targeting range is unchanged. */
	public static final float WATCH_PATH_LENGTH = 32.0F;
	/** Half the field of view, degrees (120 degrees in all). */
	public static final double FOV_HALF_ANGLE = 60.0;
	/** Ticks between motion scans (one box query of WATCH_RANGE; speeds are measured across this gap). Creepers are spread over the phases. */
	public static final int MOTION_SCAN_INTERVAL = 4;
	/** Ticks between projectile scans (a small box query for moving projectiles). */
	public static final int PROJECTILE_SCAN_INTERVAL = 2;
	/** A projectile whose path comes this close to the creeper's middle is a near-miss. */
	public static final double NEAR_MISS_RADIUS = 2.0;
	/** Projectiles are looked for this far around (fast arrows cover ~3 blocks a tick). */
	public static final double PROJECTILE_SCAN_RANGE = 8.0;
	/** A non-player stays watched this long before the creeper looks for something else (players are watched while within WATCH_RANGE). */
	public static final int NON_PLAYER_WATCH_TICKS = 300;
	/** Creative players are watched and startle creepers like anyone (owner tests in creative). True would match vanilla targeting, which ignores them. */
	public static final boolean IGNORE_CREATIVE_PLAYERS = false;

	// --- the hiss (vanilla's swell, 30 ticks = 1.5 s) ------------------------------------------------
	/** Defusing distance: vanilla's SwellGoal stops the swell past 7 blocks (its "trigger range"). */
	public static final double DEFUSE_RANGE = 7.0;

	// --- watching --------------------------------------------------------------------------------
	/** Open ground, no cover: it holds this far off (owner, 2026-11-16: 6.5 was far too close; they keep their distance). */
	public static final double COMFORT_DISTANCE = 12.0;
	/** Closer than COMFORT_DISTANCE minus this and it backs up. */
	public static final double BACKOFF_MARGIN = 2.0;
	/** Open ground: looked at, it edges back to COMFORT_DISTANCE plus this. */
	public static final double OPEN_LOOKED_AT_EXTRA = 4.0;
	/** Backing up: speed modifier on its 0.25 base (vanilla flees cats at 1.2). */
	public static final double BACKOFF_SPEED = 1.2;
	/** Walking to a watch spot or toward its target. */
	public static final double WATCH_SPEED = 1.0;
	/** A first peek spot (half behind cover) is picked up to this far from the target (and no nearer than APPROACH_MIN_DISTANCE), best near the preferred one ... */
	public static final double PEEK_PREFERRED_DISTANCE = 15.0;
	public static final double PEEK_MAX_DISTANCE = 22.0;
	/** ... from cells sampled within this many blocks of the creeper and on its side of a ring round the target (owner: it seeks walls much farther off) ... */
	public static final int SPOT_SEARCH_RADIUS = 14;
	/** ... every cell within this many blocks of it (an 11 x 11 square), plus ... */
	public static final int SPOT_NEAR_GRID = 5;
	/** ... this many random cells per search (half round the creeper, half on the ring) ... */
	public static final int SPOT_SEARCH_SAMPLES = 72;
	/** ... no farther than this from the creeper (it walks there). */
	public static final double SPOT_MAX_WALK = 22.0;
	/** At most this many sampled cells, and this many cells behind cover edges, get the raycast cover test per search (most fail after 2 rays). */
	public static final int SPOT_MAX_EVALUATIONS = 16;
	public static final int SPOT_EDGE_EVALUATIONS = 24;
	/** Cover sweep: this many level rays fanned out from the target's eyes over the creeper's side ... */
	public static final int SWEEP_RAYS = 32;
	/** ... within this many degrees either side of the creeper's bearing ... */
	public static final double SWEEP_HALF_ANGLE = 75.0;
	/** ... and this many extra rays to pin down each cover edge found between two neighbours. */
	public static final int SWEEP_BISECTIONS = 2;
	/** This many searches in a row finding nothing before it takes the open-ground stance (C5: only with no cover at all). */
	public static final int OPEN_GROUND_AFTER_FAILED_SEARCHES = 3;

	// --- creeping closer, cover to cover ----------------------------------------------------------
	/** The closest peek spot it creeps to (e.g. outside your wall, peeking in at a window). */
	public static final double APPROACH_MIN_DISTANCE = 3.0;
	/** A new spot must be at least this much closer than where it is ... */
	public static final double APPROACH_STEP = 2.0;
	/** ... and it prefers one about this much closer. */
	public static final double APPROACH_STRIDE = 6.0;
	/** Not looked at for this long before it moves closer. */
	public static final int APPROACH_UNSEEN_TICKS = 60;
	/** Ticks between tries to move closer, plus up to APPROACH_JITTER more. */
	public static final int APPROACH_INTERVAL = 100;
	public static final int APPROACH_JITTER = 60;
	/** After running off it waits this long before creeping closer again. */
	public static final int APPROACH_AFTER_FLEE = 200;
	/** Closer spots compared per try (the one with the least-seen route and a hidden place out of sight wins). */
	public static final int APPROACH_CANDIDATES = 4;
	/** Points along a route checked for being in the target's view. */
	public static final int ROUTE_SAMPLES = 6;

	// --- running off when approached ----------------------------------------------------------------
	/** The player's own movement toward it is measured over this many ticks ... */
	public static final int APPROACH_WINDOW = 20;
	/** ... and this many blocks closer in that time (0.1 a tick; a sneak is 0.065, a walk 0.216) means "coming at it". */
	public static final double FLEE_CLOSING = 2.0;
	/** A sneaking player this many blocks closer in that time is sneaking up on it. */
	public static final double SNEAK_CLOSING = 0.3;
	/** Approaches from this far or nearer count. */
	public static final double FLEE_NOTICE_RANGE = 20.0;
	/** Running off: speed modifier on its 0.25 base (0.35 blocks a tick, faster than a sprint). */
	public static final double FLEE_SPEED = 1.4;
	/** The spot it runs to is at least this much farther than it is now, at most this much, best near the preferred gain. */
	public static final double FLEE_MIN_GAIN = 3.0;
	public static final double FLEE_MAX_GAIN = 14.0;
	public static final double FLEE_PREFERRED_GAIN = 7.0;

	// --- leaving by day (mod-spawned watchers only) ------------------------------------------------
	/** Once it's day, each watcher starts leaving after a random delay in this range (10 s to 2.5 min) ... */
	public static final int LEAVE_DELAY_MIN = 200;
	public static final int LEAVE_DELAY_MAX = 3000;
	/** ... which runs twice as fast while its player hasn't looked at it for this long. */
	public static final int LEAVE_UNSEEN_TICKS = 600;
	/** Leaving and seen by no player: chance a second that it is gone, plus this much per second since its player last looked at it. */
	public static final double LEAVE_CHANCE = 0.2;
	public static final double LEAVE_CHANCE_PER_UNSEEN_SECOND = 0.05;
	/** Players farther than this don't count as seeing it (vanilla despawns anything past 128). */
	public static final double LEAVE_SEEN_RANGE = 128.0;
	/** Ticks between spot searches when it has none. */
	public static final int SPOT_SEARCH_INTERVAL = 20;
	/** Ticks between re-checks that its spot still half-hides it from the target (the target moves). */
	public static final int SPOT_RECHECK_INTERVAL = 10;
	/** Out of sight this long and it walks to see its target again. */
	public static final int LOST_VIEW_TICKS = 20;
	/** Half-hiding: the creeper's middle sits this far on the open side of the cover's edge (its face peeks out, a bit under half its body is hidden; less and a window's frame hides it). */
	public static final double HALF_HIDE_INSET = 0.12;
	/** Peekaboo: looked at, it slides this far past the edge into cover (body 0.6 wide: about 95 % hidden). */
	public static final double HIDDEN_DEPTH = 0.27;
	/** Sideways slide speed into and out of cover (blocks a tick). */
	public static final double SLIDE_SPEED = 0.08;
	/** Peekaboo: a player looking within this many degrees of the creeper is looking at it (a screen's half-width at FOV 70 is ~51). */
	public static final double PLAYER_LOOK_HALF_ANGLE = 45.0;
	/** A sneaking player closer than this and getting closer makes it back behind cover. */
	public static final double SNEAK_NOTICE_RANGE = 18.0;

	// --- spawning (owner: vanilla's random creeper spawns off; one watcher per player at night) -------
	/** At most this many mod-natural watcher creepers per player (owner: per player). */
	public static final int WATCHERS_PER_PLAYER = 1;
	/** Ticks between spawn attempts per player. */
	public static final int SPAWN_INTERVAL = 200;
	/** Random spots tried per attempt. */
	public static final int SPAWN_TRIES = 16;
	/** Watching distance it spawns at (inside WATCH_RANGE so it keeps watching). */
	public static final double SPAWN_MIN_DISTANCE = 15.0;
	public static final double SPAWN_MAX_DISTANCE = 21.0;
	/** It spawns outside this half-angle of the player's view (degrees), so never on screen. */
	public static final double SPAWN_OUTSIDE_VIEW_HALF_ANGLE = 70.0;
	/** A player's watcher this far away no longer counts as theirs (a new one may come). */
	public static final double WATCHER_RELEASE_DISTANCE = 48.0;

	private CreeperTuning() {
	}
}

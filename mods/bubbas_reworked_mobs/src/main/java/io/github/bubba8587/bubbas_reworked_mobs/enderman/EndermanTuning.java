// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

/**
 * Numbers for the caretaker endermen (specs/endermen.md). One place to tweak them. Every value here is a starting value
 * picked by the builder (2026-11-16), owner to playtest and tune, unless it says "vanilla" or "owner".
 * Distances in blocks, times in ticks (20 a second), chances per check.
 */
public final class EndermanTuning {
	// --- the target world ----------------------------------------------------------------------
	/** Regenerated chunks kept per level (about 50-100 KB each). Endermen work near where they stand, so few are needed. */
	public static final int TERRAIN_CACHE_CHUNKS = 96;
	/** Structure pieces are left alone with this margin around their boxes (village paths, foundations). */
	public static final int STRUCTURE_MARGIN = 2;

	// --- cairn sites (E2) ----------------------------------------------------------------------
	/** Side of a placement region in chunks: one candidate site per 384 x 384 blocks. */
	public static final int SITE_REGION_CHUNKS = 24;
	/** Candidates keep this many chunks from their region's edges, so two sites are at least 128 blocks apart. */
	public static final int SITE_SEPARATION_CHUNKS = 4;
	/** Placement salt (any fixed number; changing it moves every site). */
	public static final int SITE_SALT = 20261116;
	/** Ground is sampled this far out from the centre to judge flatness. */
	public static final int SITE_FLAT_RADIUS = 4;
	/** ...and may differ from the centre by at most this much, or the region has no site. */
	public static final int SITE_FLAT_TOLERANCE = 2;

	// --- cairn designs -------------------------------------------------------------------------
	/** Height of the main trunk, from the footing to its top knuckle. */
	public static final int CAIRN_MIN_HEIGHT = 8;
	public static final int CAIRN_MAX_HEIGHT = 14;
	/** Chance that one offering of a cairn is TNT (owner: rare TNT allowed); a third of cairns in deserts and badlands. */
	public static final float CAIRN_TNT_CHANCE = 0.16F;

	// --- jobs ----------------------------------------------------------------------------------
	/** Ticks between job searches for an idle enderman (spread over endermen by id). */
	public static final int JOB_SCAN_INTERVAL = 20;
	/** Every column within this distance is checked first (nearest first), then the random ones below. */
	public static final int NEAR_RADIUS = 5;
	/** Columns sampled per pickup search, within SCAN_RADIUS. */
	public static final int SCAN_COLUMNS = 16;
	public static final int SCAN_RADIUS = 12;
	/** Each pickup column is checked from its real top, but at most this far above the original ground, down to one below it. */
	public static final int SCAN_HEIGHT = 8;
	/** Columns sampled per search for a hole to fill, within FILL_RADIUS; each scans down from the original surface. */
	public static final int FILL_COLUMNS = 16;
	public static final int FILL_RADIUS = 16;
	/** Holes deeper than this below the original surface are left (mines, deep pits). */
	public static final int FILL_DEPTH = 12;
	/** E1: chance an unnatural block (planks, glass...) found where it doesn't belong is taken, per time it is found. */
	public static final float UNNATURAL_CHANCE = 0.05F;
	/** Cairn sites within this distance give an enderman work (blocks to deliver, offerings to fetch). */
	public static final int SITE_WORK_RADIUS = 48;
	/** Reach for placing or taking a block, from the eyes to the block centre. */
	public static final double REACH = 4.0;
	/** Farther than this from the job, or stuck, it teleports next to it. */
	public static final double TELEPORT_DISTANCE = 20.0;
	/** Ticks without getting closer before it teleports. */
	public static final int STUCK_TICKS = 60;
	/** A job not done in this long is dropped (the block, if carried, is kept). */
	public static final int JOB_TIMEOUT = 600;
	/** Vigil (owner, 2026-11-16): endermen with nothing to do at a complete cairn stand in a ring around it, staring at it. */
	/** The ring is this far outside the cairn's footprint. */
	public static final int VIGIL_GAP = 3;
	/** A vigil lasts this long, then it looks around for work (and most likely takes up its place again). */
	public static final int VIGIL_TICKS = 1200;
	/** By day a caretaker under open sky leaves with a 1-in-this chance each tick (about 20 s on average; owner, 2026-11-16). */
	public static final int DAY_LEAVE_ONE_IN = 400;
	/** A builder whose block can't go in yet stands in the ring around its cairn (the vigil's place), checking every second whether it can go in, and plans afresh after this long. */
	public static final int WAIT_TICKS = 400;
	/**
	 * New tasks (owner, 2026-11-16: endermen should occasionally pick up a new task): an empty-handed enderman with
	 * nothing to do near it (and one whose vigil just ended) has this chance per look round to go on an errand: it looks
	 * for something to take or trample round a random spot ERRAND_MIN-ERRAND_MAX blocks away and goes there.
	 */
	public static final float ERRAND_CHANCE = 0.25F;
	public static final int ERRAND_MIN = 12;
	public static final int ERRAND_MAX = 40;

	// --- particles on complete cairns (owner) -------------------------------------------------
	/** Ticks between puffs (a few portal particles and one end rod sparkle per complete cairn). */
	public static final int GLOW_TICKS = 4;
	/** Ticks between checks that a cairn is still complete. */
	public static final int GLOW_CHECK_TICKS = 40;
	/** Cairns within this distance of a player glow (the end rod sparkle is sent long-distance). */
	public static final int GLOW_RANGE = 96;

	// --- spawning (D2) -------------------------------------------------------------------------
	/** Ticks between spawn checks. */
	public static final int SPAWN_INTERVAL = 100;
	/** A site spawns only while a player is within this distance of it (and its chunk is loaded). */
	public static final int SITE_ACTIVE_RANGE = 96;
	/** Endermen within this distance of a site count as its own (snowball and cap). */
	public static final int SITE_RADIUS = 64;
	/** Spawn spots: this far from the site centre (out of the end rods' light). */
	public static final int SPAWN_MIN_DISTANCE = 12;
	public static final int SPAWN_MAX_DISTANCE = 28;
	public static final int SPAWN_TRIES = 8;
	/** Spawn spots are within this many blocks up or down of the site's ground (not on a mountain above it or in a cave below). */
	public static final int SPAWN_HEIGHT = 6;
	/**
	 * Chance per spawn check that a site spawns one enderman (owner, 2026-11-16: about 1-2 spire blocks a night for a
	 * while, so players wonder; once it's obvious the endermen are building something, it speeds up). It follows the
	 * spire's growth g (CairnSite.growth, 0 for a fresh stub): SPAWN_CHANCE_EMPTY + (SPAWN_CHANCE_ALMOST -
	 * SPAWN_CHANCE_EMPTY) * g^SPAWN_CURVE, and SPAWN_CHANCE_COMPLETE once the whole design stands (E2). Tuned on a model
	 * of nights (110 checks each): a 30-block spire takes about 13 nights with a player nearby (1-2 blocks a night for
	 * the first week, 4-5 at the end); 20 blocks about 8, 45 about 20.
	 */
	public static final float SPAWN_CHANCE_EMPTY = 0.04F;
	public static final float SPAWN_CHANCE_ALMOST = 0.35F;
	public static final float SPAWN_CURVE = 3.0F;
	public static final float SPAWN_CHANCE_COMPLETE = 0.35F;
	/** Snowball: each enderman already at the site raises the chance by this fraction of itself (left alone, more come). */
	public static final float SNOWBALL_PER_ENDERMAN = 0.3F;
	/**
	 * A site stops spawning at this many of its own endermen: CAP_EMPTY + (CAP_ALMOST - CAP_EMPTY) * g^2 while building
	 * (2 until the spire is about a third up: endermen linger, so this is what keeps early nights to a block or two),
	 * CAP_COMPLETE once done (the vigil ring).
	 */
	public static final int CAP_EMPTY = 2;
	public static final int CAP_ALMOST = 8;
	public static final int CAP_COMPLETE = 8;
	/**
	 * Fillers (owner, 2026-11-16: endermen should arrive already carrying what's missing round about, not only for
	 * cairns): natural ground broken within HOLE_RANGE of a site is remembered (at most HOLE_LIST_CAP per level). While a
	 * hole waits near a site, a newcomer there arrives carrying its block (at a finished cairn too), the site's spawn
	 * chance is at least HOLE_SPAWN_CHANCE, and it may spawn up to HOLE_EXTRA past its cap for them.
	 */
	public static final int HOLE_RANGE = 48;
	public static final int HOLE_LIST_CAP = 512;
	public static final float HOLE_SPAWN_CHANCE = 0.35F;
	public static final int HOLE_EXTRA = 4;
	/** At an unfinished site, the share of new endermen that arrive carrying one of its blocks: rising with growth. */
	public static final float BUILDER_SHARE_EMPTY = 0.5F;
	public static final float BUILDER_SHARE_ALMOST = 0.9F;

	// --- hostility (E4) ------------------------------------------------------------------------
	/** Breaking a cairn block, or hitting an enderman near a site, angers endermen within this distance. */
	public static final double ANGER_RADIUS = 24.0;

	// --- cairn offerings (Totems & Artifacts' Totem of the Beginning, through api/Endermen) -----------------
	/** The share of cairns (picked from each site's seed) whose endermen leave an offering once it is complete. */
	public static final float OFFERING_SHARE = 0.15F;
	/** How often (ticks) complete cairns near players are checked for their offering. */
	public static final int OFFERING_CHECK_TICKS = 100;
	/** Each check, a chosen complete cairn with one of its endermen at it leaves its offering with 1 in this chance (~1 min). */
	public static final int OFFERING_ONE_IN = 12;
	/** One of the site's own endermen must be within this many blocks of its centre to leave the offering. */
	public static final int OFFERING_WITNESS_RANGE = 16;
	/** The offering lies this many blocks outside the cairn's footprint. */
	public static final int OFFERING_GAP = 2;

	// --- a nexus area (a Totem of the Beginning in a beacon) -----------------------------------------------
	/** Each spawn check (SPAWN_INTERVAL), an area draws an enderman with this chance (a complete cairn's). */
	public static final float NEXUS_SPAWN_CHANCE = 0.35F;
	/** At most this many caretakers in an area (plus NEXUS_MARGIN round it) before it draws more. */
	public static final int NEXUS_CAP = 6;
	public static final int NEXUS_MARGIN = 8;
	/** An area's newcomers never appear within this many blocks of a player (a cairn's ring keeps 24). */
	public static final int NEXUS_PLAYER_DISTANCE = 16;
	/** Spawn spot tries per check. */
	public static final int NEXUS_TRIES = 8;

	// --- the ripple (a used Totem of the Beginning) -------------------------------------------------------
	/** Columns whose generated terrain isn't worked out yet are retried for this long, then skipped. */
	public static final int RIPPLE_PENDING_TICKS = 600;
	/** A column is left alone when any column this close (horizontally) holds a built or placed-looking block. */
	public static final int RIPPLE_PROTECT_RADIUS = 2;
	/** At most this many particles a tick (one per changed block, at most). */
	public static final int RIPPLE_PARTICLES_PER_TICK = 48;

	private EndermanTuning() {
	}
}

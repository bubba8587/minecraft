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
	/** A builder whose block can't go in yet waits within this distance of its cairn (teleports back if farther). */
	public static final int WAIT_RADIUS = 8;

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
	 * Chance per spawn check that a site spawns one enderman (owner, 2026-11-16: building is slow, and speeds up as the
	 * cairn fills in): from SPAWN_CHANCE_EMPTY with nothing standing, rising in step with the share of the design that
	 * stands, to SPAWN_CHANCE_ALMOST one block short; SPAWN_CHANCE_COMPLETE once it matches its design (E2).
	 */
	public static final float SPAWN_CHANCE_EMPTY = 0.02F;
	public static final float SPAWN_CHANCE_ALMOST = 0.20F;
	public static final float SPAWN_CHANCE_COMPLETE = 0.35F;
	/** Snowball: each enderman already at the site raises the chance by this fraction of itself (left alone, more come). */
	public static final float SNOWBALL_PER_ENDERMAN = 0.3F;
	/** A site stops spawning at this many of its own endermen: CAP_EMPTY rising to CAP_ALMOST as it fills in, CAP_COMPLETE once done. */
	public static final int CAP_EMPTY = 2;
	public static final int CAP_ALMOST = 6;
	public static final int CAP_COMPLETE = 8;
	/** At an unfinished site, the share of new endermen that arrive carrying one of its blocks. */
	public static final float BUILDER_SHARE = 0.5F;

	// --- hostility (E4) ------------------------------------------------------------------------
	/** Breaking a cairn block, or hitting an enderman near a site, angers endermen within this distance. */
	public static final double ANGER_RADIUS = 24.0;

	private EndermanTuning() {
	}
}

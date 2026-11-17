// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

/**
 * Numbers for the lurking drowned (specs/drowned.md). One place to tweak them. Every value here is a starting value
 * picked by the builder (2026-11-16), owner to playtest and tune, unless it says "vanilla".
 * Distances in blocks, speeds in blocks per tick (a player walks 0.216, sprints 0.28, swims about 0.1), times in ticks
 * (20 a second). Air: a full breath is 300 (vanilla), shown as 10 bubbles of 30.
 */
public final class DrownedTuning {
	// --- fewer, much stronger (attributes; vanilla drowned in brackets) ---------------------------------
	/** Health (vanilla 20). A sword (6-7 damage) needs about 9 hits. */
	public static final double MAX_HEALTH = 60.0;
	/** Armor points (vanilla 2). */
	public static final double ARMOR = 6.0;
	/** Knockback resistance (vanilla 0): hard to shove off its spot, but hits still count toward the grip meter. */
	public static final double KNOCKBACK_RESISTANCE = 0.6;
	/** Base speed (vanilla 0.23). Swimming and dragging use the speeds below. */
	public static final double MOVEMENT_SPEED = 0.23;
	/** On land it crawls: its speed is multiplied by 1 + this (vanilla walks upright at full speed). */
	public static final double LAND_CRAWL_SPEED_BONUS = -0.4;

	// --- waking up: lurk spots near players ------------------------------------------------------
	/** A drowned with a player this close (any mode but spectator) goes looking for a lurk spot; farther, it is vanilla (wanders). */
	public static final double ACTIVATE_RANGE = 32.0;
	/** Lurk spots are looked for around the nearest player, this far from them ... */
	public static final double SPOT_MIN_PLAYER_DISTANCE = 3.0;
	public static final double SPOT_MAX_PLAYER_DISTANCE = 14.0;
	/** ... and at most this far from the drowned (it swims there). */
	public static final double SPOT_MAX_TRAVEL = 32.0;
	/** Random columns tried per search. */
	public static final int SPOT_SEARCH_COLUMNS = 40;
	/** Each column is scanned for a water surface from this many blocks above the player's feet ... */
	public static final int SPOT_SCAN_UP = 3;
	/** ... down to this many below. */
	public static final int SPOT_SCAN_DOWN = 6;
	/** Water exactly this deep over a solid floor (owner: "2 deep, so a standing drowned's head just breaks the surface"). */
	public static final int LURK_DEPTH = 2;
	/** Land (level with the surface, or one block above it) must be this close, in one of 8 directions: the shore it faces. */
	public static final int SHORE_MAX_DISTANCE = 3;
	/** Ticks between spot searches while it has none (a phase-spread scan, not a gameplay cooldown). */
	public static final int SPOT_SEARCH_INTERVAL = 20;
	/** A spot whose nearest player is farther than this is given up (it looks for one near them). */
	public static final double SPOT_RELEASE_DISTANCE = 20.0;
	/** Within this horizontal distance of the spot it stops swimming and takes its place. */
	public static final double ARRIVE_DISTANCE = 1.5;
	/** Swimming to its spot: speed modifier for vanilla's drowned swim control. */
	public static final double TRAVEL_SPEED = 1.2;
	/** Swimming into a ledge it's heading over: rises at this speed (blocks a tick; with water drag about 0.4 block in all). */
	public static final double LEDGE_RISE_SPEED = 0.04;

	// --- lurking ----------------------------------------------------------------------------------
	/** Lurking: its eyes this far above the water's surface (owner: only the eyes and the top of the head show, the waterline just under the eyes; it floats a little off the floor). */
	public static final double LURK_EYES_ABOVE_SURFACE = 0.12;
	/** Sunk: the top of its head this far below the surface, or as low as the floor allows (in 2-deep water it stands on the floor and crouches). */
	public static final double SUNK_HEAD_BELOW_SURFACE = 0.1;
	/** How fast it rises and sinks (blocks a tick; the spring toward its height is capped at this). */
	public static final double BOB_SPEED = 0.06;
	/** Ticks between senses scans (one box query); drowned are spread over the phases. */
	public static final int SCAN_INTERVAL = 4;
	/** Things that could look at it or be grabbed are noticed this far (one box query). */
	public static final double SENSE_RANGE = 16.0;
	/** Stared at: something within SENSE_RANGE looking within this many degrees of any point of its head or the water round it, with a clear line of sight to that point (Gaze: a cone, not a direct line; owner, 2026-11-16; was 30 to a single point). */
	public static final double STARE_HALF_ANGLE = 40.0;
	/** It only moves (changes spot) while nobody looks within this many degrees of it: about the edge of the screen. */
	public static final double UNSEEN_HALF_ANGLE = 70.0;

	// --- restless lurking (owner, 2026-11-16: not parked in one spot forever) ------------------------
	/** Unseen, it moves to another lurk spot with a 1-in-this chance a tick (about every 40 s) ... */
	public static final int SHIFT_ONE_IN = 800;
	/** ... or 1 in this (about every 4 s) once its nearest player is farther than SHIFT_FOLLOW_DISTANCE from its spot: it keeps up with you along the shore. */
	public static final int SHIFT_FOLLOW_ONE_IN = 80;
	public static final double SHIFT_FOLLOW_DISTANCE = 10.0;
	/** A new spot is at most this far from the drowned (it swims there under the surface). */
	public static final double SHIFT_MAX_DISTANCE = 16.0;
	/** Lurking, it drifts this far round its spot's centre, slowly (it isn't a statue). */
	public static final double SWAY = 0.15;
	/** Now and then it dips under for a moment: 1 in this a tick (about every 30 s) ... */
	public static final int DIP_ONE_IN = 600;
	/** ... for this many ticks plus up to DIP_TICKS_EXTRA more. */
	public static final int DIP_TICKS = 20;
	public static final int DIP_TICKS_EXTRA = 40;
	/** Creative players' stares count (so the owner can try the sinking); creative players are never grabbed (they can't drown). */
	public static final boolean GRAB_CREATIVE_PLAYERS = false;

	// --- the surge --------------------------------------------------------------------------------
	/** A victim this close (horizontally) ... */
	public static final double SURGE_RANGE = 4.5;
	/** ... standing between this far below the water's surface ... */
	public static final double SURGE_BELOW_SURFACE = 1.0;
	/** ... and this far above it (on the bank) ... */
	public static final double SURGE_ABOVE_SURFACE = 2.5;
	/** ... with its back turned: the drowned is more than this many degrees from where it faces (its screen ends at about 50) ... */
	public static final double BACK_TURNED_ANGLE = 100.0;
	/** ... and nothing staring at the drowned (a friend watching the water keeps you safe). */
	public static final boolean WATCHED_BLOCKS_SURGE = true;
	/** Lunge speed toward the victim (blocks a tick: faster than a sprint, for a moment). */
	public static final double SURGE_SPEED = 0.3;
	/** Upward kick when the victim stands higher than it (climbing out onto the bank). */
	public static final double SURGE_CLIMB = 0.42;
	/** It leaves the water at most this far (horizontally from its lurk spot) to reach a victim at the edge. */
	public static final double LAND_REACH = 3.5;
	/** It gives up the surge when the victim gets this far away. */
	public static final double SURGE_GIVE_UP = 7.0;
	/** It grabs when the victim's box is within this of its own. */
	public static final double GRAB_REACH = 0.8;

	// --- the grab ---------------------------------------------------------------------------------
	/**
	 * Held by the ankles (owner, 2026-11-16): upright, the victim's soles sit at its outstretched hands (vanilla's
	 * zombie arm pose, about 80 degrees forward from hanging), this far in front of it (centre to centre) ...
	 */
	public static final double HOLD_FORWARD = 0.72;
	/** ... and this far above its own feet, so the victim floats above it and goes down feet first. */
	public static final double HOLD_UP = 1.2;
	/** Lying (crawling on land), its hands reach past its head along the ground: the victim's feet this far in front ... */
	public static final double HOLD_LYING_FORWARD = 1.2;
	/** ... at ground level. Between the two, the hold follows its swim amount (the posture easing in and out). */
	public static final double HOLD_LYING_UP = 0.0;
	/** Deep water is looked for this far around (a square, every DEEP_SEARCH_STEP columns). */
	public static final int DEEP_SEARCH_RADIUS = 10;
	public static final int DEEP_SEARCH_STEP = 2;
	/** Depth is measured down to this many blocks. */
	public static final int DEEP_MAX_DEPTH = 24;
	/** Dragging backwards through water (blocks a tick; a player swims about 0.1). */
	public static final double DRAG_SPEED = 0.07;
	/** Crawling backwards on land, dragging. */
	public static final double DRAG_LAND_SPEED = 0.06;
	/** Within this of its deep-water spot it looks for deeper water again (and stays if there is none). */
	public static final double DRAG_ARRIVE = 0.8;
	/*
	 * The grip tightens the longer it drags you (owner, 2026-11-16: "make the drowned dangerous"). The hit numbers below
	 * follow the grip meter (GRIP_START or less: start values; GRIP_FULL: full values, in a straight line between), so
	 * the danger is the meter's own visible state (B19). It also shows as more and harder hits, a louder, lower squeeze
	 * sound with each, and more bubbles streaming off the victim. Death times: tools/drowned-danger.py.
	 */
	/** Its hits: a chance each tick (no fixed rhythm, B19), from 1/50 (about every 2.5 s) ... */
	public static final float HIT_CHANCE_PER_TICK = 1.0F / 50.0F;
	/** ... to 1/10 (about every half second). */
	public static final float HIT_CHANCE_FULL = 1.0F / 10.0F;
	/** Damage of one hit, from 2 (vanilla drowned melee: 3 on normal) ... */
	public static final float HIT_DAMAGE = 2.0F;
	/** ... to 4. */
	public static final float HIT_DAMAGE_FULL = 4.0F;
	/** Air knocked out by a hit, from 45 (1.5 bubbles) ... Respiration (oxygen bonus) divides it by 1 + its value, as vanilla does for each tick's loss; Water Breathing and Conduit Power stop it (vanilla: no air is lost under them). */
	public static final int HIT_AIR_LOSS = 45;
	/** ... to 90 (3 bubbles). */
	public static final int HIT_AIR_LOSS_FULL = 90;
	/** Bubbles streaming off the victim every 2 ticks: 1 at the start, up to 1 + this when the grip is at its tightest. */
	public static final int ESCALATION_BUBBLES = 6;

	// --- others join in -----------------------------------------------------------------------------
	/** Drowned already this close to one that drags someone swim over and grab the other leg (no new ones spawn). */
	public static final double JOIN_RANGE = 16.0;
	/** At most this many join (2: three holding in all). */
	public static final int MAX_EXTRA_HOLDERS = 2;
	/** Ticks between the dragging drowned's looks for helpers (one box query of JOIN_RANGE; a scan rate, not a gameplay cooldown). */
	public static final int JOIN_SCAN_INTERVAL = 20;
	/** Swimming over to the victim (blocks a tick). */
	public static final double JOIN_SPEED = 0.15;
	/** It takes hold within this of its grip point. */
	public static final double JOIN_REACH = 1.0;
	/** It gives up joining past this distance from the victim. */
	public static final double JOIN_GIVE_UP = 24.0;
	/** Each extra holder grips from the side: this many degrees round from the first one's grip. */
	public static final float JOIN_SIDE_ANGLE = 90.0F;
	/** Grip-break points per hit are divided by 1 + this per extra holder (two holding: /1.6; three: /2.2). Hits on any holder count. */
	public static final float GRIP_EXTRA_HOLDER_DIVISOR = 0.6F;
	/** Each extra holder makes the drag this much faster (+35 %). */
	public static final double DRAG_SPEED_PER_HELPER = 0.35;

	// --- the grip meter (D3, owner 2026-11-16; B19: a meter, no hidden timer) ----------------------------
	/** The grip, 0 to this: at 0 you're free; at the top its hits are at their worst. */
	public static final float GRIP_FULL = 100.0F;
	/** It starts here when a drowned grabs (half). */
	public static final float GRIP_START = 50.0F;
	/** It rises this much each tick it drags you (5 a second: from half to full in 10 s if nobody fights) ... */
	public static final float GRIP_RISE_PER_TICK = 0.25F;
	/** ... and this much faster per extra holder (+50 %: two holding, 7.5 a second; three, 10). */
	public static final float GRIP_RISE_PER_HELPER = 0.5F;
	/** Every hit with an attacker (anyone's: the victim, a friend, an arrow) ... */
	public static final float GRIP_HIT_POINTS = 6.0F;
	/** ... plus this per point of damage (before armor). An iron sword hit (6) is 24; fists (1, at most two a second through vanilla's hurt time) 9. */
	public static final float GRIP_DAMAGE_POINTS = 3.0F;
	/** Axes multiply the hit's points (an iron axe hit, 9 damage: 49.5; two fast ones free you from a fresh grab). */
	public static final float GRIP_AXE_MULTIPLIER = 1.5F;
	/** Per level of Impaling on the weapon (a thrown or stabbing trident). */
	public static final float GRIP_IMPALING_POINTS_PER_LEVEL = 12.0F;
	/** Per level of Knockback on the weapon. */
	public static final float GRIP_KNOCKBACK_POINTS_PER_LEVEL = 10.0F;
	/** Released: the drowned is pushed back this fast (away from the victim) ... */
	public static final double RECOIL_SPEED = 0.5;
	/** ... and the victim nudged up (toward the surface) this fast. */
	public static final double RELEASE_LIFT = 0.3;

	// --- tridents (D6) ---------------------------------------------------------------------------
	/** Buried treasure gets one more pool rolling a trident at this chance ("a rare item"). Drowned no longer spawn holding one. */
	public static final float TREASURE_TRIDENT_CHANCE = 0.1F;

	private DrownedTuning() {
	}
}

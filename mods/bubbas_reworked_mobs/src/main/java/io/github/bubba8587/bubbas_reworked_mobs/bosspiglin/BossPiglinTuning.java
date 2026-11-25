// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

/** Every Boss Piglin number the owner tunes by playing (specs/boss-piglin.md). Starting values are the builder's. */
public final class BossPiglinTuning {
	private BossPiglinTuning() {
	}

	// --- stats ---------------------------------------------------------------------------------------

	/** Piglin 16, piglin brute 50. */
	public static final double HEALTH = 70.0;
	/** Piglin and brute: 0.35. */
	public static final double MOVEMENT_SPEED = 0.25;
	/** A piglin's punch (piglin 5, brute 7); it holds nothing. */
	public static final double ATTACK_DAMAGE = 5.0;
	/** The slap's knockback (vanilla attack knockback attribute; a piglin has none). */
	public static final double ATTACK_KNOCKBACK = 1.2;
	/** Piglin and brute: 0. */
	public static final double KNOCKBACK_RESISTANCE = 0.3;
	/** Paths this long (blocks) reach from the throne down to the portal room. */
	public static final double FOLLOW_RANGE = 48.0;
	public static final int XP = 20;

	// --- seated --------------------------------------------------------------------------------------

	/** Its head follows a player this close (blocks) while it sits. */
	public static final double LOOK_RANGE = 16.0;
	/** Average ticks between shouts (vanilla mobs: 80). */
	public static final int SHOUT_INTERVAL = 160;
	/** Voice pitch against a piglin's. */
	public static final float VOICE_PITCH = 0.75F;
	/** Getting up: it steps this far forward off the seat (blocks), the first that is clear. */
	public static final double[] STAND_STEPS = { 2.0, 1.95, 2.1 };
	/** How often (ticks) it checks whether its outpost's roster still lives. */
	public static final int ROSTER_CHECK = 10;

	// --- fleeing -------------------------------------------------------------------------------------

	/** It keeps this far (blocks) from players while it flees. */
	public static final float FLEE_DISTANCE = 10.0F;
	/** Speed against its own movement speed: walking away, and with a player within 7 blocks. */
	public static final double FLEE_WALK = 1.0;
	public static final double FLEE_SPRINT = 1.2;
	/** Heading for a lit portal. */
	public static final double ESCAPE_SPEED = 1.1;
	/** Heading for the portal, it re-plans its path this often (ticks)... */
	public static final int PORTAL_REPATH_TICKS = 20;
	/** ...and gives up (a frustrated grunt; it just flees) when no path fits it, or it is no closer by this much (blocks)... */
	public static final double PORTAL_PROGRESS = 0.5;
	/** ...for this many ticks... */
	public static final int PORTAL_STALL_TICKS = 80;
	/** ...then tries again after this many ticks. */
	public static final int PORTAL_GIVE_UP_TICKS = 600;
	/** It steps through a portal block this close (blocks, sideways from its hitbox): from the floor in front of the frame. */
	public static final double PORTAL_REACH = 1.0;
	/** Its path to the portal may end this many blocks short (the floor in front of the frame). */
	public static final int PORTAL_PATH_ACCURACY = 1;
	/** Wandering round the outpost when it has nowhere to go. */
	public static final double HIDE_SPEED = 0.8;
	/** It hides within the outpost's radius plus this. */
	public static final int HOME_MARGIN = 4;

	// --- cornered ------------------------------------------------------------------------------------

	/** A player this close (blocks)... */
	public static final double CORNER_RANGE = 2.0;
	/** ...for this many ticks in a row gets slapped... */
	public static final int CORNER_TICKS = 20;
	/** ...and not again for this many ticks. */
	public static final int SLAP_COOLDOWN = 40;

	// --- client --------------------------------------------------------------------------------------

	/** The seated and fleeing looks blend in or out by this much a tick (10 ticks). */
	public static final float BLEND_STEP = 0.1F;
}

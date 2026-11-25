// dte:C1,B15,B17,B21
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import net.minecraft.util.RandomSource;

/** Numbers for monster camps (specs/monster-camps.md). Builder's starting values, for the owner to playtest. */
public final class CampTuning {
	/** Camp sizes: clearing radius, roster (not counting the leader), most towers, banners, outcrops, largest room half-width, chance of each. */
	public enum Size {
		SMALL(6, 3, 1, 1, 1, 2, 0.45F),
		MEDIUM(8, 5, 2, 2, 2, 3, 0.35F),
		LARGE(11, 7, 3, 3, 3, 4, 0.20F);

		public final int radius;
		public final int roster;
		public final int maxTowers;
		public final int banners;
		public final int outcrops;
		public final int maxRoomHalf;
		final float weight;

		Size(int radius, int roster, int maxTowers, int banners, int outcrops, int maxRoomHalf, float weight) {
			this.radius = radius;
			this.roster = roster;
			this.maxTowers = maxTowers;
			this.maxRoomHalf = maxRoomHalf;
			this.banners = banners;
			this.outcrops = outcrops;
			this.weight = weight;
		}

		public static Size pick(RandomSource random) {
			float r = random.nextFloat();
			for (Size size : values()) {
				if ((r -= size.weight) < 0.0F) {
					return size;
				}
			}
			return LARGE;
		}

		public String id() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** The roster appears when a survival player comes within this many blocks of the camp's centre... */
	public static final double TRIGGER_RANGE = 48.0;
	/** ...and a cleared camp may reset only while nobody is within this many blocks. */
	public static final double AWAY_RANGE = 96.0;
	/** Chance a day that a camp with its roster dead and a banner standing resets (about 1 in 3, owner). */
	public static final float RESET_CHANCE = 1.0F / 3.0F;
	/** Chase distance from home before a camp mob walks back. */
	public static final double LEASH_RANGE = 32.0;
	/** Players are checked for nearby camps this often (ticks). */
	public static final int CHECK_TICKS = 20;
	/** Camps and outposts are looked for this many chunks round each player. */
	public static final int DISCOVER_CHUNKS = 4;

	/** The leader: this much larger (vanilla scale attribute)... */
	public static final double LEADER_SCALE = 1.25;
	/** ...this many times the health... */
	public static final double LEADER_HEALTH = 2.5;
	/** ...this much more attack damage (melee kinds)... */
	public static final double LEADER_DAMAGE = 3.0;
	/** ...and this knockback resistance. */
	public static final double LEADER_KNOCKBACK_RESISTANCE = 0.5;
	/** Chance the leader drops its weapon, and each piece of armour. */
	public static final float LEADER_WEAPON_DROP = 0.5F;
	public static final float LEADER_ARMOR_DROP = 0.2F;
	/** Chance a roster member holds a stone weapon (melee kinds). */
	public static final float MEMBER_WEAPON = 0.4F;
	/** Nether outposts: chance a ground member is a piglin brute, and hoglins per pen by size (small, medium, large). */
	public static final float OUTPOST_BRUTE = 0.25F;
	static final int[] OUTPOST_HOGLINS = { 1, 2, 3 };

	/** Entity tag on every camp mob; a second tag {@code CAMP_TAG + ":" + id} says which camp. */
	public static final String CAMP_TAG = "bubbas_reworked_mobs.camp";

	// --- patrols (specs/monster-camps.md "Camp patrols") ---

	/** An armed camp's territory: the land within this many blocks of its centre. */
	public static final int PATROL_TERRITORY = 320;
	/** Each player is checked this often (ticks)... */
	public static final int PATROL_CHECK_TICKS = 600;
	/** ...with this chance of a patrol. */
	public static final float PATROL_CHANCE = 0.1F;
	/** Ticks after a camp's patrol appears before it may send another. */
	public static final int PATROL_COOLDOWN = 6000;
	/** A patrol appears this far from the player (horizontal)... */
	public static final int PATROL_MIN_DISTANCE = 24;
	public static final int PATROL_MAX_DISTANCE = 48;
	/** ...or this far for the command. */
	public static final int PATROL_COMMAND_MIN_DISTANCE = 12;
	public static final int PATROL_COMMAND_MAX_DISTANCE = 20;
	/** Spots tried per patrol (an unseen one wins). */
	public static final int PATROL_SPOT_TRIES = 10;
	/** Most block light at a patrol's spawn spot (vanilla's patrol rule). */
	public static final int PATROL_MAX_BLOCK_LIGHT = 8;
	/** Patrol size, leader included: the smallest (small camp; +1 per size step) and the spread. */
	public static final int PATROL_SIZE_MIN = 2;
	public static final int PATROL_SIZE_SPREAD = 2;
	/** Camps with a stable: chance a patrol is mounted, and each follower's chance to ride (the leader always does). */
	public static final float PATROL_MOUNTED_CHANCE = 0.5F;
	public static final float PATROL_RIDER_SHARE = 0.5F;
	/** Chance a zombie or husk rider carries an iron spear. */
	public static final float PATROL_SPEAR_CHANCE = 0.5F;
	/** The leader's next point: this far off (min, max)... */
	public static final int PATROL_LEG_MIN = 32;
	public static final int PATROL_LEG_MAX = 64;
	/** ...picked again within this of it (vanilla 10)... */
	public static final double PATROL_ARRIVE = 10.0;
	/** ...walked to in steps of this length (vanilla 10). */
	public static final double PATROL_STEP = 10.0;
	/** A step whose surface is more than this above or below the mob stays at the mob's height (overhangs, roofs). */
	public static final int PATROL_OVERHANG = 4;
	/** Companions within this of a member keep the patrol going (vanilla 16). */
	public static final double PATROL_COMPANION_RANGE = 16.0;
	/** Walking speed of followers and the (slower) leader. */
	public static final double PATROL_SPEED = 1.0;
	public static final double PATROL_LEADER_SPEED = 0.85;
	/** Ticks a member waits after a path fails (vanilla 200). */
	public static final int PATROL_STUCK_COOLDOWN = 200;

	/** Entity tag on every patrol member; a second tag {@code PATROL_TAG + ":" + id} says which camp. */
	public static final String PATROL_TAG = "bubbas_reworked_mobs.patrol";
	/** Entity tag on a patrol's leader (the banner carrier). */
	public static final String PATROL_LEADER_TAG = PATROL_TAG + ".leader";
	/** Entity tag on a patrol's mounts (they despawn like the riders). */
	public static final String PATROL_MOUNT_TAG = PATROL_TAG + ".mount";

	private CampTuning() {
	}
}

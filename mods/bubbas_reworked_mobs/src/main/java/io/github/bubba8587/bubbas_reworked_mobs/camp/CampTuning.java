// dte:C1
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
	/** Chance a roster member wears a helmet, and holds a weapon (melee kinds). */
	public static final float MEMBER_HELMET = 0.5F;
	public static final float MEMBER_WEAPON = 0.4F;
	/** Nether outposts: chance a ground member is a piglin brute, and hoglins per pen by size (small, medium, large). */
	public static final float OUTPOST_BRUTE = 0.25F;
	static final int[] OUTPOST_HOGLINS = { 1, 2, 3 };

	/** Entity tag on every camp mob; a second tag {@code CAMP_TAG + ":" + id} says which camp. */
	public static final String CAMP_TAG = "bubbas_reworked_mobs.camp";

	private CampTuning() {
	}
}

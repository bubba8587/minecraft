// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Numbers for part 1 (specs/undead.md "Tuning" and "Hearing and waking"). One place to tweak them. */
public final class UndeadTuning {
	// Woken mobs: much slower, much tougher, weak fists (vanilla: 20 health, zombie speed 0.23 / attack 3, skeleton speed 0.25 / attack 2).
	public static final double SKELETON_HEALTH = 60.0;
	public static final double SKELETON_SPEED = 0.15;
	public static final double ZOMBIE_HEALTH = 80.0;
	public static final double ZOMBIE_SPEED = 0.13;
	public static final double FIST_DAMAGE = 1.0;

	// Combat (specs/undead.md "Tuning"). Starting values, owner to tune.
	/** Fist hits from the same undead on the same target keep a streak going while each comes within this many ticks of the last. */
	public static final int FIST_STREAK_WINDOW_TICKS = 60;
	/** Extra armor durability per fist hit by streak length (1st hit, 2nd, 3rd, 4th and later), spread over the worn pieces. */
	public static final int[] FIST_STREAK_ARMOR_WEAR = {0, 1, 3, 6};
	/** Smite's chance per level to kill one of our undead outright (level V = 20 %). */
	public static final float SMITE_KILL_CHANCE_PER_LEVEL = 0.04F;
	/** Arrows deal this fraction of their damage to our skeletons (arrows pass between the bones). */
	public static final float SKELETON_ARROW_DAMAGE_FACTOR = 0.25F;
	/** Arrows that hit a zombie's head deal this many times their damage. */
	public static final float ZOMBIE_HEADSHOT_FACTOR = 2.0F;
	/** The top quarter of a zombie's hitbox counts as its head (adult: 0.49 of 1.95 blocks, the head cube is 0.5). */
	public static final float HEAD_FRACTION_OF_HEIGHT = 0.25F;
	/** Idle woken zombies look for other woken zombies this often (ticks) ... */
	public static final int DRIFT_INTERVAL_TICKS = 200;
	/** ... within this many blocks horizontally (6 up and down) ... */
	public static final double DRIFT_RANGE = 24.0;
	/** ... where zombies within this distance of the nearest one count as its group ... */
	public static final double DRIFT_GROUP_RADIUS = 8.0;
	/** ... and walk to the group's centre, stopping this close. */
	public static final double DRIFT_STOP_DISTANCE = 3.0;

	// The zombie shamble (ZombieGait): a long drag, then a fall onto the good leg. Starting values, owner to tune.
	/** Ticks per shamble cycle (one drag and one step). */
	public static final int GAIT_PERIOD_TICKS = 30;
	/** The first this much of the cycle is the slow drag; the rest is the step, a fall onto the good leg. */
	public static final float GAIT_DRAG_FRACTION = 0.75F;
	/** Body speed during the drag, as a fraction of the average speed (the step makes up the rest). */
	public static final float GAIT_DRAG_SPEED = 0.9F;

	// Robbing: one item per empty-hand use; armor always risks more than the weapon.
	public static final float ROB_WEAPON_WAKE_CHANCE = 0.1F;
	public static final float ROB_ARMOR_WAKE_CHANCE = 0.3F;

	// Hearing: a disturbance meter per sleeper, on the warden's anger model (specs/undead.md "Hearing and waking").
	// Starting values, owner to tune. Every heard noise adds points x distance falloff x sensitivity; the meter
	// decays steadily; crossing WAKE wakes the sleeper, crossing STIR makes it groan.
	public static final int LISTENER_RADIUS = 16;
	/** The meter value that wakes a sleeper. */
	public static final float WAKE_THRESHOLD = 100.0F;
	/** Crossing this (upward) makes a sleeper groan quietly: the player's warning. */
	public static final float STIR_THRESHOLD = 50.0F;
	/** Points lost per tick (10 a second: a half-disturbed sleeper settles in 5 s, a nearly woken one in 10 s). */
	public static final float DISTURBANCE_DECAY_PER_TICK = 0.5F;
	/** Noise counts in full this close ... */
	public static final double NOISE_FULL_RANGE = 2.0;
	/** ... and falls off as the square of the distance left, to nothing here (a different hallway is silent). */
	public static final double NOISE_SILENT_RANGE = 12.0;
	/** Each solid block (opaque full cube) on the straight line between noise and sleeper multiplies the noise by this. */
	public static final float SOLID_BLOCK_FACTOR = 0.3F;
	/** One footstep (vanilla frequency 1: step, swim, flap). A player steps about every 1.7 blocks. */
	public static final float STEP_POINTS = 40.0F;
	/** Sprinting footsteps count this many times. */
	public static final float SPRINT_STEP_FACTOR = 2.0F;
	/** Every other vibration: BASE + PER_FREQUENCY x its vanilla vibration frequency (2 to 15: landing 60 ... block break 85, place 87.5, death 92.5). */
	public static final float EVENT_BASE_POINTS = 55.0F;
	public static final float EVENT_POINTS_PER_FREQUENCY = 2.5F;
	/** Explosions and lightning: wakes everything in earshot not shielded by walls. */
	public static final float DEAFENING_POINTS = 1000.0F;
	/** A neighbour waking (the awaken event): wakes sleepers within about 6 blocks at once, stirs those a little farther. */
	public static final float AWAKEN_POINTS = 300.0F;
	/** One awaken game event wakes at most this many sleepers (Lorespawn capped cascades at 4). */
	public static final int AWAKEN_CAP = 4;

	private UndeadTuning() {
	}

	public static AttributeSupplier.Builder wokenSkeletonAttributes() {
		return AbstractSkeleton.createAttributes()
				.add(Attributes.MAX_HEALTH, SKELETON_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, SKELETON_SPEED)
				.add(Attributes.ATTACK_DAMAGE, FIST_DAMAGE);
	}

	public static AttributeSupplier.Builder wokenZombieAttributes() {
		return Zombie.createAttributes()
				.add(Attributes.MAX_HEALTH, ZOMBIE_HEALTH)
				.add(Attributes.MOVEMENT_SPEED, ZOMBIE_SPEED)
				.add(Attributes.ATTACK_DAMAGE, FIST_DAMAGE)
				.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
	}
}

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

	// Robbing: one item per empty-hand use; armor always risks more than the weapon.
	public static final float ROB_WEAPON_WAKE_CHANCE = 0.1F;
	public static final float ROB_ARMOR_WAKE_CHANCE = 0.3F;

	// Hearing.
	public static final int LISTENER_RADIUS = 16;
	/** After hearing something (woken or not) a sleeper ignores sounds for this long. */
	public static final int HEARING_COOLDOWN_TICKS = 20;
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

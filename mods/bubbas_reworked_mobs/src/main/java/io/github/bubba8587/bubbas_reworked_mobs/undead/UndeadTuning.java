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

	// Robbing: one item per empty-hand use; armor always risks more than the weapon.
	public static final float ROB_WEAPON_WAKE_CHANCE = 0.1F;
	public static final float ROB_ARMOR_WAKE_CHANCE = 0.3F;

	// Hearing.
	public static final int LISTENER_RADIUS = 16;
	/** After hearing something (woken or not) a sleeper ignores sounds for this long. */
	public static final int HEARING_COOLDOWN_TICKS = 20;
	/** One awaken game event wakes at most this many sleepers (Lorespawn capped cascades at 4). */
	public static final int AWAKEN_CAP = 4;
	/** Empty-handed woken undead grab a weapon this close: from a loot display first, else (zombies) one lying loose. */
	public static final double WEAPON_PICKUP_RANGE = 3.0;

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

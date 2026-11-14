// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import io.github.bubba8587.bubbas_reworked_mobs.loot.GrabLootGoal;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** A zombie that woke from a sleeper block (see {@link WokenSkeleton}). */
public class WokenZombie extends Zombie {
	public WokenZombie(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	/** Plus grabbing what's lying around when idle and empty-handed, and drifting into groups (below the attack goals, above strolling). */
	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(5, new GrabLootGoal(this, 1.0));
		this.goalSelector.addGoal(6, new DriftToHordeGoal(this, 1.0));
	}

	/** Headshots with arrows hurt more (UndeadCombat). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return super.hurtServer(level, source, UndeadCombat.zombieDamage(this, source, damage));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}

	/** Stays our type: vanilla would turn it into a vanilla drowned underwater. */
	@Override
	protected boolean convertsInWater() {
		return false;
	}
}

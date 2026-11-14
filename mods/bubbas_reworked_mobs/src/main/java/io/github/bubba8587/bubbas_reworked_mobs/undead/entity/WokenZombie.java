// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import io.github.bubba8587.bubbas_reworked_mobs.loot.GrabLootGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** A zombie that woke from a sleeper block (see {@link WokenSkeleton}). */
public class WokenZombie extends Zombie {
	public WokenZombie(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	/** Plus grabbing what's lying around when idle and empty-handed (below the attack goals, above strolling). */
	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(5, new GrabLootGoal(this, 1.0));
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

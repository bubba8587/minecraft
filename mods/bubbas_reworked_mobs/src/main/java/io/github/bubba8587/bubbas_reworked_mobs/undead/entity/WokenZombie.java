// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

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

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}

	/** Stays our type: vanilla would turn it into a vanilla drowned underwater. */
	@Override
	protected boolean convertsInWater() {
		return false;
	}
}

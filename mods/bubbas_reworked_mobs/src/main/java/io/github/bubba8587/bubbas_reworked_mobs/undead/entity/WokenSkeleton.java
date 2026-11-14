// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

/**
 * A skeleton that woke from a sleeper block. Its own entity type (tagged as a skeleton and undead) so tuning and
 * later behaviour stay scoped to this mod's undead; rendered with the vanilla skeleton renderer.
 */
public class WokenSkeleton extends Skeleton {
	public WokenSkeleton(EntityType<? extends Skeleton> type, Level level) {
		super(type, level);
	}

	/** Gear comes from the sleeper, never from the vanilla spawn roll (a skeleton would get a bow). */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}
}

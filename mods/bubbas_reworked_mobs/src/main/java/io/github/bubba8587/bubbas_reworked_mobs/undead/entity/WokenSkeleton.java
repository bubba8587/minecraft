// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import io.github.bubba8587.bubbas_reworked_mobs.loot.GrabLootGoal;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
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
	/** Plus grabbing what's lying around when idle and empty-handed (below the attack goals, above strolling). */
	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(5, new GrabLootGoal(this, 1.0));
	}

	/** Arrows mostly pass between the bones (UndeadCombat). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return super.hurtServer(level, source, UndeadCombat.skeletonDamage(source, damage));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}
}

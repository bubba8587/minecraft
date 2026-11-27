// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A frostbite that woke from a sleeper frozen in ice: our woken zombie with vanilla Frostbite's melee ways
 * (freezing hits, powder snow doesn't hold it, its sounds). No ice ball throwing.
 */
public class WokenFrostbite extends WokenZombie {
	public WokenFrostbite(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.FROSTBITE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.FROSTBITE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.FROSTBITE_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.FROSTBITE_STEP;
	}

	/** Vanilla Frostbite: a hit freezes the target for 3 s. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.FREEZING, 60), this);
		}
		return hit;
	}

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
		if (!state.is(Blocks.POWDER_SNOW)) {
			super.makeStuckInBlock(state, speedMultiplier);
		}
	}
}

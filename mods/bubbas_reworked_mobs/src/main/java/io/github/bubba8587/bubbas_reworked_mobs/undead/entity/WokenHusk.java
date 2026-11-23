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

/**
 * A husk that woke from a sleeper buried in sand (specs/undead.md "Undead variants"). Our woken zombie (its shamble,
 * loot grabbing, headshots) with the husk's own ways, copied from vanilla's Husk: it doesn't burn by day, its bare-handed
 * hits make you hungry, and it sounds like a husk. Drawn with the husk's texture.
 */
public class WokenHusk extends WokenZombie {
	public WokenHusk(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	@Override
	protected boolean isSunSensitive() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HUSK_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.HUSK_STEP;
	}

	/** Vanilla's husk: a bare-handed hit makes the target hungry, longer on harder difficulties. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && this.getMainHandItem().isEmpty() && target instanceof LivingEntity living) {
			float difficulty = level.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
			living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140 * (int) difficulty), this);
		}
		return hit;
	}
}

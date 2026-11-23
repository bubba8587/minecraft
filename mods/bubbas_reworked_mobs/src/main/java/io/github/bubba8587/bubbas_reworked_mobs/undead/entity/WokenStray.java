// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A stray that woke from a sleeper frozen in ice (specs/undead.md "Undead variants"). Our woken skeleton (loot grabbing,
 * arrows passing between its bones) with the stray's own ways, copied from vanilla's Stray: its arrows slow what they
 * hit, and it sounds like a stray. Drawn with the stray's texture and tattered clothes.
 */
public class WokenStray extends WokenSkeleton {
	public WokenStray(EntityType<? extends Skeleton> type, Level level) {
		super(type, level);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.STRAY_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.STRAY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.STRAY_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.STRAY_STEP;
	}

	/** Vanilla's stray: its arrows carry Slowness (30 s). */
	@Override
	protected AbstractArrow getArrow(ItemStack projectile, float power, @Nullable ItemStack firingWeapon) {
		AbstractArrow arrow = super.getArrow(projectile, power, firingWeapon);
		if (arrow instanceof Arrow tipped) {
			tipped.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600));
		}
		return arrow;
	}
}

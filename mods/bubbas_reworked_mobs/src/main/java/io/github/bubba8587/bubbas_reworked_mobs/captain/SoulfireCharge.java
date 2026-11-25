// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Soulfire Blaze's charge: a blaze small fireball that hits harder. Where it lands it places fire as a blaze's does
 * (vanilla {@code BaseFireBlock.getState}: soul fire on soul sand and soul soil, fire elsewhere).
 */
public class SoulfireCharge extends SmallFireball {
	public SoulfireCharge(EntityType<? extends SoulfireCharge> type, Level level) {
		super(type, level);
	}

	/** A charge shot by {@code owner} along {@code direction} (as SmallFireball's owner constructor). */
	public static SoulfireCharge shoot(Level level, LivingEntity owner, Vec3 direction) {
		SoulfireCharge charge = new SoulfireCharge(Captains.SOULFIRE_CHARGE, level);
		charge.snapTo(owner.getX(), owner.getY(), owner.getZ(), charge.getYRot(), charge.getXRot());
		charge.reapplyPosition();
		charge.setDeltaMovement(direction.normalize().scale(charge.accelerationPower));
		charge.needsSync = true;
		charge.setOwner(owner);
		charge.setRot(owner.getYRot(), owner.getXRot());
		return charge;
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		if (this.level() instanceof ServerLevel level) {
			Entity target = hitResult.getEntity();
			int fireBefore = target.getRemainingFireTicks();
			boolean soulProof = target instanceof LivingEntity living && CaptainsHelm.isWornBy(living);
			if (!soulProof) {
				target.igniteForSeconds(CaptainTuning.CHARGE_BURN_SECONDS);
			}
			DamageSource source = this.damageSources().fireball(this, this.getOwner());
			if (!target.hurtServer(level, source, CaptainTuning.CHARGE_DAMAGE)) {
				target.setRemainingFireTicks(fireBefore);
			} else {
				EnchantmentHelper.doPostAttackEffects(level, target, source);
			}
		}
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.SOUL_FIRE_FLAME;
	}
}

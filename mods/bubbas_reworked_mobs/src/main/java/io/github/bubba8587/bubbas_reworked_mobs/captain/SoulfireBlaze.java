// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Soulfire Blaze (specs/wither-captain.md): a blaze whose small fireballs are Soulfire Charges
 * ({@code BlazeAttackGoalMixin}). One a captain summoned burns out when that captain is gone.
 */
public class SoulfireBlaze extends Blaze {
	private static final String CAPTAIN_KEY = "bubbas_reworked_mobs:captain";

	private @Nullable UUID captain;

	public SoulfireBlaze(EntityType<? extends SoulfireBlaze> type, Level level) {
		super(type, level);
	}

	/** Ties it to the captain that summoned it. */
	public void linkTo(WitherCaptain captain) {
		this.captain = captain.getUUID();
	}

	public @Nullable UUID captain() {
		return this.captain;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
			this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.captain != null && this.tickCount % CaptainTuning.BLAZE_LINK_CHECK == 0) {
			Entity owner = level.getEntity(this.captain);
			if (!(owner instanceof WitherCaptain c) || !c.isAlive()) {
				this.burnOut(level);
			}
		}
	}

	/** Goes out in a puff of soul fire: no drops. */
	public void burnOut(ServerLevel level) {
		if (this.isRemoved()) {
			return;
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(0.5), this.getZ(), 25, 0.3, 0.5, 0.3, 0.03);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
		this.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.8F);
		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable(CAPTAIN_KEY, UUIDUtil.CODEC, this.captain);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.captain = input.read(CAPTAIN_KEY, UUIDUtil.CODEC).orElse(null);
	}
}

// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulfireTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Phasing: the mob effect that carries the state; keeps its holder Soulfire-burning for as long as it lasts. */
public class PhasingEffect extends MobEffect {
	PhasingEffect() {
		super(MobEffectCategory.NEUTRAL, SoulfireTuning.PHASING_COLOR);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
		// Soulfire for the whole Phasing time, unless Soulguard
		MobEffectInstance phasing = entity.getEffect(Phasing.PHASING);
		if (phasing == null || SoulBurning.hasSoulguard(entity)) {
			return true;
		}
		int ticks = phasing.isInfiniteDuration() ? SoulfireTuning.PHASING_TICKS : phasing.getDuration();
		if (entity.getRemainingFireTicks() < ticks || !SoulBurning.isSoulBurning(entity)) {
			SoulBurning.igniteForTicks(entity, ticks);
		}
		return true;
	}
}

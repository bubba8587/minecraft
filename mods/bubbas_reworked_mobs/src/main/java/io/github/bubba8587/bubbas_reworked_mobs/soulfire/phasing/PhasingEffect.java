// dte:C1,B15,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing;

import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulfireTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Phasing: the mob effect that carries the state; keeps its holder Soulfire-burning for as long as it lasts. */
public class PhasingEffect extends MobEffect {
	/** Walk distance at each holder's last movement sound. */
	private static final Map<LivingEntity, Float> LAST_SOUND_AT = new WeakHashMap<>();

	PhasingEffect() {
		super(MobEffectCategory.NEUTRAL, SoulfireTuning.PHASING_COLOR);
		setBlendDuration(SoulfireTuning.PHASING_FADE_IN_TICKS, SoulfireTuning.PHASING_FADE_OUT_TICKS, SoulfireTuning.PHASING_FADE_OUT_TICKS);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
		MobEffectInstance phasing = entity.getEffect(Phasing.PHASING);
		if (phasing == null) {
			return true;
		}
		sounds(level, entity, phasing);
		// Soulfire for the whole Phasing time, unless Soulguard
		if (SoulBurning.hasSoulguard(entity)) {
			return true;
		}
		int ticks = phasing.isInfiniteDuration() ? SoulfireTuning.PHASING_TICKS : phasing.getDuration();
		if (entity.getRemainingFireTicks() < ticks || !SoulBurning.isSoulBurning(entity)) {
			SoulBurning.igniteForTicks(entity, ticks);
		}
		return true;
	}

	/** Movement sounds while passing through blocks (by walk distance, like footsteps) and the finish sound. */
	private static void sounds(ServerLevel level, LivingEntity entity, MobEffectInstance phasing) {
		var random = entity.getRandom();
		if (Phasing.climbing(entity)) {
			float last = LAST_SOUND_AT.getOrDefault(entity, entity.moveDist);
			if (entity.moveDist - last >= SoulfireTuning.PHASING_SOUND_STRIDE) {
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS,
						0.35F, 0.8F + random.nextFloat() * 0.4F);
				last = entity.moveDist;
			}
			LAST_SOUND_AT.put(entity, last);
		} else {
			LAST_SOUND_AT.remove(entity);
		}
		if (phasing.getDuration() == 1) {
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8F, 1.1F);
		}
	}
}

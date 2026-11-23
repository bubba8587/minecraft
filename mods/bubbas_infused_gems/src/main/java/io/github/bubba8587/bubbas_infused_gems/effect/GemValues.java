// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** How much of one effect an entity gets from gems right now, on the effect's own scale. */
public final class GemValues {
	private GemValues() {
	}

	/** Armor gems only, before any vanilla effect. */
	public static double rawWorn(LivingEntity entity, String key) {
		GemTotals t = WornEffects.of(entity);
		if (t.isEmpty()) {
			return 0.0;
		}
		Balance b = Balance.get();
		String from = key;
		if (b.undeadInversion && entity.isInvertedHealAndHarm()) {
			from = switch (key) {
				case "instant_health" -> "instant_damage";
				case "instant_damage" -> "instant_health";
				default -> key;
			};
		}
		double s = t.value(from);
		if (s <= 0.0) {
			return 0.0;
		}
		if (!from.equals(key)) {
			Balance.EffectData fe = b.effect(from);
			Balance.EffectData ke = b.effect(key);
			double fc = fe != null && fe.wornCap() > 0.0 ? fe.wornCap() : b.cap;
			double kc = ke != null && ke.wornCap() > 0.0 ? ke.wornCap() : b.cap;
			s = s * kc / fc;
		}
		Holder<MobEffect> vanilla = ModEffects.vanilla(key);
		if (vanilla != null && !entity.canBeAffected(new MobEffectInstance(vanilla))) {
			return 0.0;
		}
		return s;
	}

	/** The fraction of the effect's 1.0 level a vanilla instance of it on this entity gives. */
	public static double vanillaFraction(LivingEntity entity, String key) {
		if (!Balance.get().strongerApplies) {
			return 0.0;
		}
		Holder<MobEffect> vanilla = ModEffects.vanilla(key);
		MobEffectInstance inst = vanilla == null ? null : entity.getEffect(vanilla);
		return inst == null ? 0.0 : levelFraction(key, inst.getAmplifier());
	}

	/** A vanilla level as a fraction of the effect's 1.0 level. */
	public static double levelFraction(String key, int amplifier) {
		Balance.EffectData d = Balance.get().effect(key);
		if (d == null) {
			return 1.0;
		}
		int top = d.wornAmplifier();
		if (d.hitKind().equals("instant")) {
			return Math.min(1.0, (1 << Math.min(amplifier, 20)) / (double) (1 << top));
		}
		if (amplifier >= top || top == 0) {
			return 1.0;
		}
		return d.levelOne() + (1.0 - d.levelOne()) * amplifier / top;
	}

	/** Armor gems after the vanilla effect of the same kind: only the part above it. */
	public static double worn(LivingEntity entity, String key) {
		double s = rawWorn(entity, key);
		if (s <= 0.0) {
			return 0.0;
		}
		return Math.max(0.0, s - Math.max(vanillaFraction(entity, key), hit(entity, key)));
	}

	/** The potency of a gem effect a weapon put on this entity. */
	public static double hit(LivingEntity entity, String key) {
		Holder<MobEffect> gem = ModEffects.gem(key);
		MobEffectInstance inst = gem == null ? null : entity.getEffect(gem);
		return inst == null ? 0.0 : GemMobEffect.potency(inst.getAmplifier());
	}

	/** What the entity gets of this effect from gems. */
	public static double value(LivingEntity entity, String key) {
		return Math.max(worn(entity, key), hit(entity, key));
	}
}

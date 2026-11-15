// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * How much of one effect an entity gets from gems right now, on the effect's own scale (1.0 = its strongest vanilla
 * level): its armor gems (after the undead swap and immunities, topped up only above a vanilla effect of the same kind:
 * the stronger applies, they don't add) or a gem effect a weapon put on it, whichever is stronger. Works on the server
 * and, for the local player, on the client (both keep the worn totals). The hooks (gravity, water drag, hunger,
 * visibility, prices, fire, night vision...) all read it.
 */
public final class GemValues {
	private GemValues() {
	}

	/** Armor gems only, before any vanilla effect: undead swap Healing and Harming (rule), immune entities get nothing. */
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
		Holder<MobEffect> vanilla = ModEffects.vanilla(key);
		if (vanilla != null && !entity.canBeAffected(new MobEffectInstance(vanilla))) {
			return 0.0;
		}
		return s;
	}

	/** The fraction of the effect's 1.0 level a vanilla instance of it on this entity gives (0 when none, or rule off). */
	public static double vanillaFraction(LivingEntity entity, String key) {
		if (!Balance.get().strongerApplies) {
			return 0.0;
		}
		Holder<MobEffect> vanilla = ModEffects.vanilla(key);
		MobEffectInstance inst = vanilla == null ? null : entity.getEffect(vanilla);
		return inst == null ? 0.0 : levelFraction(key, inst.getAmplifier());
	}

	/** A vanilla level (amplifier) as a fraction of the effect's 1.0 level (the model's level_one and 1.0 amplifier). */
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

	/** Armor gems after the vanilla effect of the same kind: only the part above it (the stronger applies). */
	public static double worn(LivingEntity entity, String key) {
		double s = rawWorn(entity, key);
		if (s <= 0.0) {
			return 0.0;
		}
		return Math.max(0.0, s - Math.max(vanillaFraction(entity, key), hit(entity, key)));
	}

	/** The potency of a gem effect a weapon put on this entity (0 when none). */
	public static double hit(LivingEntity entity, String key) {
		Holder<MobEffect> gem = ModEffects.gem(key);
		MobEffectInstance inst = gem == null ? null : entity.getEffect(gem);
		return inst == null ? 0.0 : GemMobEffect.potency(inst.getAmplifier());
	}

	/** What the entity gets of this effect from gems: armor or a weapon's gem effect, the stronger. */
	public static double value(LivingEntity entity, String key) {
		return Math.max(worn(entity, key), hit(entity, key));
	}
}

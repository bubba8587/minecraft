// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.Locale;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;

/** What a gem strength does in game, in plain words (the /gems command). Numbers from the balance model. */
public final class GemDescribe {
	private GemDescribe() {
	}

	private static String f(String format, Object... args) {
		return String.format(Locale.ROOT, format, args);
	}

	private static double attr(String key, int i, double s) {
		return Balance.get().effect(key).wornAttributes().get(i).at(s);
	}

	/** Worn (armor) at total s. */
	public static String worn(String key, double s) {
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(key);
		return switch (key) {
			case "speed", "slowness" -> f("%+.1f%% speed", 100 * attr(key, 0, s));
			case "strength", "weakness" -> f("%+.2f attack damage", attr(key, 0, s));
			case "jump_boost" -> f("%+.2f jump strength, %+.1f safe fall blocks", attr(key, 0, s), attr(key, 1, s));
			case "haste" -> f("%+.1f%% mining speed, %+.1f%% attack speed", 100 * attr(key, 0, s), 100 * attr(key, 1, s));
			case "mining_fatigue" -> f("mining x%.2f, %+.1f%% attack speed", 1 + attr(key, 0, s), 100 * attr(key, 1, s));
			case "luck", "unluck" -> f("%+.2f luck", attr(key, 0, s));
			case "regeneration" -> f("heals %.2f HP/s", e.wornNumber("heal_per_second", 0.8) * s);
			case "resistance" -> f("%.1f%% less damage taken", 100 * e.wornNumber("damage_reduction", 0.8) * s);
			case "fire_resistance" -> f("%.1f%% less fire damage, burning %.0f%% shorter", 100 * s, 100 * s);
			case "water_breathing" -> s >= 1 ? "air never runs out" : f("air lasts %.0f s (normally 15 s)", 15 / (1 - s));
			case "night_vision" -> f("%.0f%% of night vision brightness", 100 * s);
			case "slow_falling" -> f("%.0f%% less fall damage, falls slower", 100 * s);
			case "instant_health" -> f("%.1f%% of each survived hit healed back", 100 * b.healFraction(s));
			case "instant_damage" -> f("%.1f%% more damage from each hit taken", 100 * b.healFraction(s));
			case "absorption" -> f("%.1f HP shield, refills 30 s after a hit", attr(key, 0, s));
			case "hunger" -> f("extra hunger: 1 food point per %.0f s", 4 / (e.wornNumber("exhaustion_per_tick", 0.015) * 20 * s));
			case "poison" -> f("poisons you %.2f HP/s (never below 1 HP)", e.wornNumber("damage_per_second", 1.667) * s);
			case "wither" -> f("withers you %.2f HP/s", e.wornNumber("damage_per_second", 0.5) * s);
			default -> "not working yet (a later version)";
		};
	}

	/** One hit at weapon strength s (or a drawback of size s). */
	public static String hit(GemHits.Entry entry) {
		Balance b = Balance.get();
		String key = entry.key();
		Balance.EffectData e = b.effect(key);
		if (!entry.drawback() && e.hitKind().equals("instant")) {
			return f(key.equals("instant_damage") ? "%.1f magic damage, once per %d s" : "heals the target %.1f HP, once per %d s",
					e.hitUnit() * entry.s(), b.harmChargeSeconds);
		}
		double p = GemHits.potency(entry);
		double seconds = GemHits.duration(entry) / 20.0;
		String what = switch (key) {
			case "poison" -> f("%.2f HP/s poison", e.hitUnit() * p);
			case "wither" -> f("%.2f HP/s wither", e.hitUnit() * p);
			case "regeneration" -> f("heals %.2f HP/s", e.hitUnit() * p);
			case "resistance" -> f("%.0f%% less damage taken", 100 * e.hitUnit() * p);
			case "hunger" -> "extra hunger";
			default -> e.wornAttributes().isEmpty() ? "" : worn(key, p);
		};
		return f("%s for %.1f s", what, seconds);
	}
}

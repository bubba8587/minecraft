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

	private static String air(double s) {
		return s >= 1 ? "air never runs out" : f("air lasts %.0f s (normally 15 s)", 15 / (1 - s));
	}

	/** Worn (armor) at total s, or what a gem effect at potency s does to whoever has it. */
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
			case "resistance" -> f("%.1f%% less damage taken", 100 * e.wornNumber("damage_reduction", 0.4) * s);
			case "turtle_master" -> f("%.1f%% less damage taken, %+.1f%% speed", 100 * e.wornNumber("damage_reduction", 0.8) * s,
					100 * attr(key, 0, s));
			case "fire_resistance" -> f("%.1f%% less fire damage, burning %.0f%% shorter", 100 * s, 100 * s);
			case "water_breathing" -> air(s);
			case "night_vision" -> f("%.0f%% of night vision brightness", 100 * s);
			case "slow_falling" -> f("%.0f%% less fall damage, falls slower", 100 * s);
			case "instant_health" -> f("%.1f%% of each survived hit healed back", 100 * b.healFraction(s));
			case "instant_damage" -> f("%.1f%% more damage from each hit taken", 100 * b.healFraction(s));
			case "absorption" -> f("%.1f HP shield, full when put on, refills 30 s after a hit", attr(key, 0, s));
			case "health_boost" -> f("%+.1f max health", attr(key, 0, s));
			case "invisibility" -> f("mobs notice you at %.0f%% of the range%s%s", 100 * (1 - e.wornNumber("visibility_reduction", 0.93) * s),
					attr(key, 0, s) < 0 ? ", hidden on the locator bar" : "",
					s >= e.wornNumber("full_at", 1.0) - 1e-9 ? ", real invisibility" : "");
			case "dolphins_grace" -> f("swims %.0f%% of the way to Dolphin's Grace", 100 * s);
			case "saturation" -> f("restores 1 food point every %.1f s", 1.0 / WornEffects.saturationRate(s));
			case "conduit_power" -> f("in water or rain: %s, underwater mining %+.2f", air(s), attr(key, 1, s));
			case "hero_of_the_village" -> f("villager trades %.0f%% cheaper", 100 * e.wornNumber("price_cut", 0.55) * s);
			case "hunger" -> f("extra hunger: 1 food point per %.0f s", 4 / (e.wornNumber("exhaustion_per_tick", 0.015) * 20 * s));
			case "poison" -> f("poisons you %.2f HP/s (never below 1 HP)", e.wornNumber("damage_per_second", 1.667) * s);
			case "wither" -> f("withers you %.2f HP/s", e.wornNumber("damage_per_second", 0.5) * s);
			case "glowing" -> f("%s on you all the time", e.name());
			case "darkness" -> f("your screen dims, %.0f%% of the way to Darkness", 100 * s);
			case "blindness" -> f("fog closes in, %.0f%% of the way to Blindness", 100 * s);
			case "nausea" -> f("your screen wobbles, %.0f%% of Nausea", 100 * s);
			case "levitation" -> f("lifts you at %.2f blocks/s", WornEffects.levitationRise(s) * 20 * 0.98);
			case "wind_charged" -> f("a wind burst where you die, %.0f%% chance", 100 * s);
			case "weaving" -> f("cobwebs where you die, %.0f%% chance", 100 * s);
			case "oozing" -> f("2 slimes where you die, %.0f%% chance", 100 * s);
			case "infested" -> f("%.1f%% chance of silverfish each time you are hurt", 10 * s);
			default -> f("%.0f%%", 100 * s);
		};
	}

	/** One part of a combined gem (Turtle Master's Resistance or Slowness) at strength s. */
	public static String part(String part, double amount, double s) {
		return part.equals("resistance") ? f("%.1f%% less damage taken", 100 * amount * s) : f("%+.1f%% speed", 100 * amount * s);
	}

	/** One hit at weapon strength s (or a drawback of size s). */
	public static String hit(GemHits.Entry entry) {
		Balance b = Balance.get();
		String key = entry.key();
		Balance.EffectData e = b.effect(key);
		double seconds = GemHits.duration(entry) / 20.0;
		if (!entry.drawback()) {
			if (e.hitKind().equals("instant")) {
				return f(key.equals("instant_damage") ? "+%.1f magic damage every hit" : "heals the target %.1f HP every hit",
						GemHits.strike(key, entry.s()));
			}
			if (e.hitKind().equals("onoff")) {
				return f("%s for %.1f s", e.name(), seconds);
			}
		}
		double p = GemHits.potency(entry);
		String what = switch (key) {
			case "poison" -> f("%.2f HP/s poison", e.hitUnit() * p);
			case "wither" -> f("%.2f HP/s wither", e.hitUnit() * p);
			case "hunger" -> "extra hunger";
			default -> worn(key, p);
		};
		return f("%s for %.1f s", what, seconds);
	}
}

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
		return s >= 1 - 1e-6 ? "air never runs out" : f("air lasts %.0f s (normally 15 s)", 15 / (1 - s));
	}

	/** Worn (armor) at total s, or what a gem effect at potency s does to whoever has it. */
	public static String worn(String key, double s) {
		Balance b = Balance.get();
		Balance.EffectData e = b.effect(key);
		// Short labels (owner): the effect's name comes first in every line, so the value doesn't repeat it.
		return switch (key) {
			case "speed", "slowness" -> f("%+.1f%%", 100 * attr(key, 0, s));
			case "strength", "weakness" -> f("%+.2f damage", attr(key, 0, s));
			case "jump_boost" -> f("%+.2f jump, %+.1f safe fall", attr(key, 0, s), attr(key, 1, s));
			case "haste" -> f("%+.1f%% mining, %+.1f%% attack speed", 100 * attr(key, 0, s), 100 * attr(key, 1, s));
			case "mining_fatigue" -> f("mining x%.2f, %+.1f%% attack speed", 1 + attr(key, 0, s), 100 * attr(key, 1, s));
			case "luck", "unluck" -> f("%+.2f", attr(key, 0, s));
			case "regeneration" -> f("%.2f HP/s", e.wornNumber("heal_per_second", 0.8) * s);
			case "resistance" -> f("-%.1f%% damage", 100 * e.wornNumber("damage_reduction", 0.4) * s);
			case "turtle_master" -> f("-%.1f%% damage, %+.1f%% speed", 100 * e.wornNumber("damage_reduction", 0.8) * s, 100 * attr(key, 0, s));
			case "fire_resistance" -> f("-%.1f%% fire damage, -%.0f%% burning", 100 * s, 100 * s);
			case "water_breathing" -> air(s);
			case "night_vision" -> f("%.0f%% brightness", 100 * s);
			case "slow_falling" -> s <= 1.0 ? f("-%.0f%% fall damage, slower falls", 100 * s) : f("no fall damage, falls %.1fx slower than the potion", s);
			case "instant_health" -> f("%.1f%% of each hit healed back", 100 * b.healFraction(s));
			case "instant_damage" -> f("+%.1f%% damage taken", 100 * b.healFraction(s));
			case "absorption" -> f("%.1f HP shield", attr(key, 0, s));
			case "health_boost" -> f("%+.1f max health", attr(key, 0, s));
			case "invisibility" -> f("seen at %.0f%% range%s%s", 100 * (1 - e.wornNumber("visibility_reduction", 0.93) * s),
					attr(key, 0, s) < 0 ? ", off the locator bar" : "",
					s >= e.wornNumber("full_at", 1.0) - 1e-6 ? ", invisible" : "");
			case "dolphins_grace" -> f("%.0f%% of the potion's swim boost", 100 * s);
			case "saturation" -> f("1 food per %.1f s, up to %d", 1.0 / WornEffects.saturationRate(s), WornEffects.saturationFoodTarget(s));
			case "conduit_power" -> f("in water: %s, mining %+.2f", air(s), attr(key, 1, s));
			case "hero_of_the_village" -> f("-%.0f%% prices", 100 * e.wornNumber("price_cut", 0.55) * s);
			case "hunger" -> f("1 extra food per %.0f s", 4 / (e.wornNumber("exhaustion_per_tick", 0.015) * 20 * s));
			case "poison" -> f("%.2f HP/s (not below 1 HP)", e.wornNumber("damage_per_second", 1.667) * s);
			case "wither" -> f("%.2f HP/s", e.wornNumber("damage_per_second", 0.5) * s);
			case "glowing" -> "always on";
			case "darkness" -> f("%.0f%% dimming", 100 * s);
			case "blindness" -> f("%.0f%% fog", 100 * s);
			case "nausea" -> f("%.0f%% wobble", 100 * s);
			case "levitation" -> f("rises %.2f blocks/s", WornEffects.levitationRise(s) * 20 * 0.98);
			case "wind_charged" -> f("bounce %.2f, fall damage %.0f%%", e.wornNumber("land_bounce", 0.6) * s / b.cap, 100 * Math.max(0.0, 1 - s / b.cap));
			case "weaving" -> f("%.0f%% cobweb speed", 100 * Math.min(1.0, s / b.cap));
			case "oozing" -> f("bounce %.0f%%, fall damage %.0f%%", 100 * e.wornNumber("bounce", 0.9) * s / b.cap, 100 * Math.max(0.0, 1 - s / b.cap));
			case "silence" -> f("%.0f%% silenced", 100 * Math.min(1.0, s / b.cap));
			case "ender" -> f("%.0f%% blink chance", 100 * Math.min(1.0, e.wornNumber("chance", 0.5) * s / b.cap));
			case "breeze" -> f("%.0f%% deflect chance", 100 * Math.min(1.0, e.wornNumber("chance", 0.5) * s / b.cap));
			case "infested" -> f("%.1f%% silverfish chance when hurt", 10 * s);
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
				return f(key.equals("instant_damage") ? "+%.1f magic damage" : "heals %.1f HP",
						GemHits.strike(key, entry.s()));
			}
			if (e.hitKind().equals("action")) {
				return switch (key) {
					case "wind_charged" -> {
						double[] l = GemActions.windLaunch(entry.s(), false);
						yield f("thrown %.2f up, %.2f away", l[0], l[1]);
					}
					case "weaving" -> f("%.0f%% web chance", 100 * e.wornNumber("web_chance", 0.35) * entry.s() / b.cap);
					default -> "kills split off your slimes";
				};
			}
			if (e.hitKind().equals("onoff")) {
				return f("for %.1f s", seconds);
			}
		}
		double p = GemHits.potency(entry);
		String what = switch (key) {
			case "poison", "wither" -> f("%.2f HP/s", e.hitUnit() * p);
			case "hunger" -> "extra hunger";
			default -> worn(key, p);
		};
		return f("%s for %.1f s", what, seconds);
	}
}

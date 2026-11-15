// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

/** Names and keys shared by the items, tooltips and the command. */
public final class GemText {
	public static final String KEY = BubbasInfusedGems.MOD_ID + ".";

	private GemText() {
	}

	/** The balance model's key for an effect: its id path ("speed", "instant_health"...). */
	public static String key(Holder<MobEffect> effect) {
		return effect.unwrapKey().map(k -> k.identifier().getPath()).orElse("unknown");
	}

	/** Healing and Harming read better than vanilla's "Instant Health"/"Instant Damage" on a gem. */
	public static MutableComponent effectName(Holder<MobEffect> effect) {
		String key = key(effect);
		if (key.equals("instant_health") || key.equals("instant_damage") || key.equals("wind_charged")) {
			return Component.translatable(KEY + "effect." + key);
		}
		return effect.value().getDisplayName().copy();
	}

	public static MutableComponent effectName(String key) {
		return effectName(holder(key));
	}

	public static Holder<MobEffect> holder(String key) {
		Balance.EffectData e = Balance.get().effect(key);
		Identifier id = e == null ? Identifier.withDefaultNamespace(key) : Identifier.parse(e.id());
		return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(id)
				.map(h -> (Holder<MobEffect>) h).orElseThrow(() -> new IllegalArgumentException("no effect " + key));
	}

	public static String numeral(int infusion) {
		return Balance.get().infusion(infusion).numeral();
	}

	public static String percent(double x) {
		return Math.round(x * 100) + "%";
	}
}

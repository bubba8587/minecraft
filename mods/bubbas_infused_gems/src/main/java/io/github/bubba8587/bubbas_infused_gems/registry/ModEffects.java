// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemMobEffect;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/**
 * Gem mob effects ({@code bubbas_infused_gems:gem_<effect>}), one per scaled hit-applied effect v0 implements: what a
 * gemmed weapon or arrow puts on its target. Healing and Harming strike at once instead (effect/GemHits).
 */
public final class ModEffects {
	public static final Map<String, Holder<MobEffect>> GEM_EFFECTS = new LinkedHashMap<>();

	private ModEffects() {
	}

	public static Holder<MobEffect> gem(String key) {
		return GEM_EFFECTS.get(key);
	}

	public static void initialize() {
		Balance b = Balance.get();
		for (String key : b.v0Hit.stream().sorted().toList()) {
			Balance.EffectData e = b.effect(key);
			if (e.hitKind().equals("instant")) {
				continue;
			}
			MobEffect vanilla = GemText.holder(key).value();
			GEM_EFFECTS.put(key, Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BubbasInfusedGems.id("gem_" + key),
					new GemMobEffect(key, vanilla.getCategory(), vanilla.getColor())));
		}
	}
}

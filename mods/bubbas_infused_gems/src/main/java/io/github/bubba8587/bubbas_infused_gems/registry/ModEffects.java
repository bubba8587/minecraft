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
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Gem mob effects, one per scaled hit-applied effect. */
public final class ModEffects {
	public static final Map<String, Holder<MobEffect>> GEM_EFFECTS = new LinkedHashMap<>();

	private ModEffects() {
	}

	public static Holder<MobEffect> gem(String key) {
		return GEM_EFFECTS.get(key);
	}

	public static void initialize() {
		Balance b = Balance.get();
		for (Balance.EffectData e : b.effects.values()) {
			if (own(e)) {
				int color = !e.parts().isEmpty() ? PotionContents.getColorOptional(Potions.TURTLE_MASTER.value().getEffects()).orElse(e.color()) : e.color();
				GEM_EFFECTS.put(e.key(), Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.parse(e.id()),
						new GemMobEffect(e.key(), MobEffectCategory.BENEFICIAL, color)));
			}
		}
		for (String key : b.effects.keySet().stream().sorted().toList()) {
			Balance.EffectData e = b.effect(key);
			if (!(e.hitKind().equals("scaled") || e.hitKind().equals("control")) || !e.id().startsWith("minecraft:")) {
				continue;
			}
			MobEffect vanilla = GemText.holder(key).value();
			GEM_EFFECTS.put(key, Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BubbasInfusedGems.id("gem_" + key),
					new GemMobEffect(key, vanilla.getCategory(), vanilla.getColor())));
		}
	}

	/** Registered here: the mod's own status effects (Turtle Master, Silence...), not vanilla's or another mod's. */
	private static boolean own(Balance.EffectData e) {
		return e.id().startsWith(BubbasInfusedGems.MOD_ID + ":");
	}

	/** The real status effect of this key (vanilla's, or another mod's such as Soulguard), or null. */
	public static Holder<MobEffect> vanilla(String key) {
		Balance.EffectData e = Balance.get().effect(key);
		if (e == null || own(e)) {
			return null;
		}
		return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(e.id())).map(h -> (Holder<MobEffect>) h).orElse(null);
	}
}

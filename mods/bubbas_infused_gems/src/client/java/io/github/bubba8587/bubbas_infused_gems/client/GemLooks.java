// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.effect.GemValues;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

/** A gem effect looks like the vanilla effect wherever vanilla shows it. */
public final class GemLooks {
	private GemLooks() {
	}

	public static boolean has(Player player, Holder<MobEffect> vanilla) {
		String key = vanilla.unwrapKey().map(k -> k.identifier().getPath()).orElse(null);
		if (key == null || !vanilla.unwrapKey().get().identifier().getNamespace().equals("minecraft") || Balance.get().effect(key) == null) {
			return false;
		}
		return GemValues.value(player, key) > 0.0 || GemValues.rawWorn(player, key) > 0.0;
	}
}

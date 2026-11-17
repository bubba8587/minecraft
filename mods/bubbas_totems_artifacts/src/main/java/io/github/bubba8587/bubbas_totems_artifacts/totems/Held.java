// dte:C6,B18
package io.github.bubba8587.bubbas_totems_artifacts.totems;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * "Held" for the totems that work from a hand (Gathering, Fortification, Growth; like Warding): the main or off hand of
 * any living thing, players and mobs alike (B18). {@link #firstThisTick} keeps a holder with a totem in each hand from
 * doubling up.
 */
public final class Held {
	private final Map<Entity, Long> lastTick = new WeakHashMap<>();

	/** Whether {@code slot} is a hand of a living, non-spectator holder. */
	public static boolean inHand(Entity owner, @Nullable EquipmentSlot slot) {
		return (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && owner instanceof LivingEntity living
				&& living.isAlive() && !living.isSpectator();
	}

	/** True the first time it's asked for {@code holder} in game tick {@code now}. */
	public boolean firstThisTick(Entity holder, long now) {
		Long last = this.lastTick.put(holder, now);
		return last == null || last != now;
	}
}

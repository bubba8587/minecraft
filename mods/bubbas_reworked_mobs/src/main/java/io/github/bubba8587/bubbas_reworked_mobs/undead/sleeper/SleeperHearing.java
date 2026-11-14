// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Loudness table: the chance one heard event wakes a sleeper, before its sensitivity. Falls off linearly with
 * distance. Candidate for a data-driven table later.
 */
public final class SleeperHearing {
	private SleeperHearing() {
	}

	public static float baseChance(Holder<GameEvent> event) {
		if (event.is(ModGameEvents.AWAKEN)) {
			return 0.6F;
		}
		if (event.is(GameEvent.EXPLODE) || event.is(GameEvent.LIGHTNING_STRIKE)) {
			return 1.0F;
		}
		if (event.is(GameEvent.STEP) || event.is(GameEvent.SWIM)) {
			return 0.04F;
		}
		if (event.is(GameEvent.HIT_GROUND) || event.is(GameEvent.PROJECTILE_LAND) || event.is(GameEvent.ENTITY_DAMAGE)) {
			return 0.15F;
		}
		if (event.is(GameEvent.CONTAINER_OPEN) || event.is(GameEvent.CONTAINER_CLOSE)) {
			return 0.2F;
		}
		if (event.is(GameEvent.BLOCK_DESTROY) || event.is(GameEvent.BLOCK_PLACE) || event.is(GameEvent.INSTRUMENT_PLAY)
				|| event.is(GameEvent.NOTE_BLOCK_PLAY) || event.is(GameEvent.PRIME_FUSE) || event.is(GameEvent.ENTITY_DIE)) {
			return 0.3F;
		}
		return 0.1F;
	}

	public static float wakeChance(Holder<GameEvent> event, double distance) {
		double falloff = 1.0 - Math.min(distance, UndeadTuning.LISTENER_RADIUS) / UndeadTuning.LISTENER_RADIUS;
		return (float) (baseChance(event) * (0.25 + 0.75 * falloff));
	}
}

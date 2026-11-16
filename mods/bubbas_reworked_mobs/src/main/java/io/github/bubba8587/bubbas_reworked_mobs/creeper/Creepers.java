// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import net.minecraft.world.entity.monster.Creeper;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Wiring for the watcher creeper (specs/creepers.md): attacks startle it; the per-player watcher spawner ticks. */
public final class Creepers {
	private Creepers() {
	}

	public static void initialize() {
		java.util.Objects.requireNonNull(WatcherSpawning.WATCHER_FOR); // registers the attachment now
		// One attack fills the meter partway (owner). Only damage with an attacker counts (hits, arrows, thrown things);
		// a fall or a cactus doesn't startle it (vanilla's fall-swell still happens).
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseTaken, taken, blocked) -> {
			if (entity instanceof Creeper creeper && source.getEntity() != null && CreeperMind.active(creeper)) {
				CreeperAccess.mind(creeper).attacked(source.getEntity(), creeper.level().getGameTime());
			}
		});
		ServerTickEvents.END_LEVEL_TICK.register(WatcherSpawning::tick);
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ModGameEvents {
	/** Emitted when a sleeper wakes; other sleepers hear it (chain waking). Not in #minecraft:vibrations, so sculk ignores it. */
	public static final Holder.Reference<GameEvent> AWAKEN = Registry.registerForHolder(BuiltInRegistries.GAME_EVENT,
			BubbasReworkedMobs.id("awaken"), new GameEvent(12));

	private ModGameEvents() {
	}

	public static void initialize() {
	}
}

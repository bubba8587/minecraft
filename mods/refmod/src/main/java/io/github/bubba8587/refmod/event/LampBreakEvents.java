// dte:A2,A4
package io.github.bubba8587.refmod.event;

import java.util.concurrent.atomic.AtomicInteger;

import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.registry.ModBlocks;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

/** Event: logs and counts signal lamps broken by players (server side, since server start). */
public final class LampBreakEvents {
	private static final AtomicInteger LAMPS_BROKEN = new AtomicInteger();

	private LampBreakEvents() {
	}

	public static int lampsBroken() {
		return LAMPS_BROKEN.get();
	}

	public static void initialize() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (state.is(ModBlocks.SIGNAL_LAMP)) {
				int n = LAMPS_BROKEN.incrementAndGet();
				RefMod.LOGGER.info("{} broke a signal lamp at {} ({} broken so far)", player.getName().getString(), pos.toShortString(), n);
			}
		});
	}
}

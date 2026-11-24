// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.end;

import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Totem of the End held in water: enderman-style damage and escape teleport (specs/totems-artifacts.md). */
public final class EndWater {
	private EndWater() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(EndWater::tick);
	}

	private static void tick(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (player.isInWater() && player.isAlive() && TotemOfTheEndItem.held(player)) {
				inWater(level, player);
			}
		}
	}

	/** One tick in water while holding the totem; true if the player was moved out. */
	public static boolean inWater(ServerLevel level, ServerPlayer player) {
		if (player.hurtServer(level, player.damageSources().drown(), TotemsTuning.END_WATER_DAMAGE)) {
			return EndTeleport.escapeWater(level, player);
		}
		return false;
	}
}

// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.beginning;

import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** The Totem of the Beginning in a beacon or a statue: the hand pulse, with no player, on a fixed rhythm. */
public final class BeginningAreas {
	private BeginningAreas() {
	}

	/** Whether a source that runs every {@code callTicks} ticks pulses on this call (once per rhythm, on the world clock). */
	public static boolean due(ServerLevel level, int callTicks) {
		return level.getGameTime() % TotemsTuning.BEGINNING_AREA_PULSE_TICKS < callTicks;
	}

	/** One area pulse round {@code centre}. */
	public static boolean pulse(ServerLevel level, BlockPos centre, int radius) {
		return SleeperHook.get().pulseAround(level, centre, radius, TotemsTuning.BEGINNING_AREA_TICKS_PER_BLOCK,
				TotemsTuning.BEGINNING_PULSE_CHANCE, TotemsTuning.BEGINNING_PULSE_ORE_CHANCE, null);
	}
}

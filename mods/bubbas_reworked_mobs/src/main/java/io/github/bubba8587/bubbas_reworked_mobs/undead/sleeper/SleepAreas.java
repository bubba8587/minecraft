// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.world.LapsingAreas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Areas where sleepers never wake from noise, set through api/Sleepers (specs/undead.md "Warding"). */
public final class SleepAreas {
	private static final LapsingAreas AREAS = new LapsingAreas();

	private SleepAreas() {
	}

	public static void put(ServerLevel level, Object source, AABB box, int lifetimeTicks) {
		AREAS.put(level, source, box, lifetimeTicks);
	}

	public static void remove(ServerLevel level, Object source) {
		AREAS.remove(level, source);
	}

	/** Whether the sleeper at {@code pos} is inside an active keep-asleep area. */
	public static boolean isKeptAsleep(Level level, BlockPos pos) {
		return AREAS.contains(level, pos);
	}
}

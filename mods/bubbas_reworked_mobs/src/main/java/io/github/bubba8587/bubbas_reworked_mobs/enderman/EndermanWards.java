// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import io.github.bubba8587.bubbas_reworked_mobs.world.LapsingAreas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Areas endermen leave alone, set through api/Endermen (specs/endermen.md, E2). */
public final class EndermanWards {
	private static final LapsingAreas AREAS = new LapsingAreas();

	private EndermanWards() {
	}

	public static void put(ServerLevel level, Object source, AABB box, int lifetimeTicks) {
		AREAS.put(level, source, box, lifetimeTicks);
	}

	public static void remove(ServerLevel level, Object source) {
		AREAS.remove(level, source);
	}

	/** Whether this block is inside an active ward. */
	public static boolean isWarded(Level level, BlockPos pos) {
		return AREAS.contains(level, pos);
	}
}

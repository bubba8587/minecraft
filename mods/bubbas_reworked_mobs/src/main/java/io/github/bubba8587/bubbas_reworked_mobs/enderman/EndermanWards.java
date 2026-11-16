// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Areas endermen leave alone (E2: "a Totem of Warding area keeps endermen and their work out"), set by other mods
 * through api/Endermen (Bubba's Totems & Artifacts, for every ward). Inside one, endermen judge no block (no repair,
 * no building) and a cairn site whose centre is inside spawns nothing. Each area belongs to a source key and lapses
 * unless refreshed, like {@code SleepAreas}. Server thread only.
 */
public final class EndermanWards {
	private record Area(AABB box, long expiresAt) {
	}

	private static final Map<ServerLevel, Map<Object, Area>> AREAS = new WeakHashMap<>();

	private EndermanWards() {
	}

	public static void put(ServerLevel level, Object source, AABB box, int lifetimeTicks) {
		AREAS.computeIfAbsent(level, l -> new HashMap<>()).put(source, new Area(box, level.getGameTime() + Math.max(1, lifetimeTicks)));
	}

	public static void remove(ServerLevel level, Object source) {
		Map<Object, Area> areas = AREAS.get(level);
		if (areas != null) {
			areas.remove(source);
		}
	}

	/** Whether this block is inside an active ward. One map check when there are none. */
	public static boolean isWarded(Level level, BlockPos pos) {
		if (AREAS.isEmpty() || !(level instanceof ServerLevel server)) {
			return false;
		}
		Map<Object, Area> areas = AREAS.get(server);
		if (areas == null || areas.isEmpty()) {
			return false;
		}
		long now = server.getGameTime();
		areas.values().removeIf(area -> area.expiresAt <= now);
		for (Area area : areas.values()) {
			if (area.box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
				return true;
			}
		}
		return false;
	}
}

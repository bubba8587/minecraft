// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Areas where sleepers never wake from noise, set by other mods through api/Sleepers (a Totem of Warding in a full
 * beacon pyramid, from Bubba's Totems & Artifacts). Each area belongs to a source key and lapses unless the source
 * refreshes it, so a broken or unloaded source needs no cleanup hook. Expired areas are dropped lazily on lookup; no
 * ticker. Server thread only.
 */
public final class SleepAreas {
	private record Area(AABB box, long expiresAt) {
	}

	private static final Map<ServerLevel, Map<Object, Area>> AREAS = new WeakHashMap<>();

	private SleepAreas() {
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

	/** Whether the sleeper at {@code pos} is inside an active keep-asleep area. One map check when there are none. */
	public static boolean isKeptAsleep(Level level, BlockPos pos) {
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

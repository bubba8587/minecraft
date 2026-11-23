// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Boxes per level, one per source key, that lapse unless the source refreshes them. Server thread only. */
public final class LapsingAreas {
	private record Area(AABB box, long expiresAt) {
	}

	private final Map<ServerLevel, Map<Object, Area>> areas = new WeakHashMap<>();

	public void put(ServerLevel level, Object source, AABB box, int lifetimeTicks) {
		this.areas.computeIfAbsent(level, l -> new HashMap<>()).put(source, new Area(box, level.getGameTime() + Math.max(1, lifetimeTicks)));
	}

	public void remove(ServerLevel level, Object source) {
		Map<Object, Area> byLevel = this.areas.get(level);
		if (byLevel != null) {
			byLevel.remove(source);
		}
	}

	/** Whether the block at {@code pos} is inside a live area. One map check when there are none. */
	public boolean contains(Level level, BlockPos pos) {
		if (this.areas.isEmpty() || !(level instanceof ServerLevel server)) {
			return false;
		}
		for (AABB box : live(server)) {
			if (box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
				return true;
			}
		}
		return false;
	}

	/** The live boxes in {@code level}; expired ones are dropped. */
	public List<AABB> live(ServerLevel level) {
		Map<Object, Area> byLevel = this.areas.get(level);
		if (byLevel == null || byLevel.isEmpty()) {
			return List.of();
		}
		long now = level.getGameTime();
		byLevel.values().removeIf(area -> area.expiresAt <= now);
		List<AABB> out = new ArrayList<>(byLevel.size());
		for (Area area : byLevel.values()) {
			out.add(area.box);
		}
		return out;
	}
}

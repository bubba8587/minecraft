// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.totems;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * Active totem areas set by beacons (Fortification: no explosion block damage; Growth: faster growth), the same idea
 * as Warding's {@code Wards}: each source (a beacon position) puts its box with a lifetime and refreshes it on every
 * pulse, so a beacon that stops, breaks or unloads lapses on its own with no cleanup hook. Nothing here depends on
 * Bubba's Reworked Beacons. Server thread only; one map lookup when a level has no areas.
 */
public final class TotemAreas {
	public enum Kind {
		FORTIFICATION, GROWTH
	}

	/** One area; {@code strength} is what the kind makes of it (Growth: extra random ticks per vanilla tick). */
	public record Area(AABB box, int strength, long expiresAt) {
	}

	private static final Map<ServerLevel, Map<Kind, Map<Object, Area>>> AREAS = new WeakHashMap<>();

	private TotemAreas() {
	}

	public static void put(ServerLevel level, Kind kind, Object source, AABB box, int strength, int lifetimeTicks) {
		AREAS.computeIfAbsent(level, l -> new EnumMap<>(Kind.class)).computeIfAbsent(kind, k -> new HashMap<>())
				.put(source, new Area(box, strength, level.getGameTime() + Math.max(1, lifetimeTicks)));
	}

	public static void remove(ServerLevel level, Kind kind, Object source) {
		Map<Kind, Map<Object, Area>> byKind = AREAS.get(level);
		if (byKind != null && byKind.get(kind) != null) {
			byKind.get(kind).remove(source);
		}
	}

	/** The live areas of one kind (expired ones are dropped as they are found). */
	public static List<Area> all(ServerLevel level, Kind kind) {
		Map<Kind, Map<Object, Area>> byKind = AREAS.get(level);
		Map<Object, Area> areas = byKind == null ? null : byKind.get(kind);
		if (areas == null || areas.isEmpty()) {
			return List.of();
		}
		long now = level.getGameTime();
		List<Area> live = new ArrayList<>(areas.size());
		for (Iterator<Area> it = areas.values().iterator(); it.hasNext(); ) {
			Area area = it.next();
			if (area.expiresAt() < now) {
				it.remove();
			} else {
				live.add(area);
			}
		}
		return live;
	}

	/** Whether the block at {@code pos} is inside a live area of this kind. */
	public static boolean contains(ServerLevel level, Kind kind, BlockPos pos) {
		for (Area area : all(level, kind)) {
			if (area.box().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
				return true;
			}
		}
		return false;
	}
}

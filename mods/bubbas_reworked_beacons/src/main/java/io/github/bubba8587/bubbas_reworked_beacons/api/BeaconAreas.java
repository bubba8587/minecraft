// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Which beacon areas are active right now, on the server (specs/reworked-beacons.md, "Payload API"). A beacon
 * appears here on each pulse while it has a working item, and drops out when it goes idle, is broken or its
 * chunk unloads; an entry also counts as gone if it missed two pulses. Cheap enough for spawn checks: one list
 * per level, one box test per active beacon.
 *
 * <p>Callers in other mods must only reach this class when Reworked Beacons is loaded
 * ({@code FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")}). Main server thread only.
 */
public final class BeaconAreas {
	/** An active beacon: its position, how it reaches, its pyramid, the item's power after the cap and its area. */
	public record ActiveBeacon(BlockPos pos, BeaconMode mode, int levels, int effectivePower, AABB area, ItemStack item, long lastPulse) {
		public boolean contains(Vec3 point) {
			return this.area.contains(point);
		}
	}

	private static final long STALE_AFTER = 2L * BeaconContext.PULSE_TICKS + 20L;
	private static final Map<Level, Map<BlockPos, ActiveBeacon>> ACTIVE = new WeakHashMap<>();

	private BeaconAreas() {
	}

	/** Whether {@code pos} (block centre) is inside an active beacon area whose item matches {@code item}. */
	public static boolean isInside(Level level, BlockPos pos, Predicate<ItemStack> item) {
		return isInside(level, Vec3.atCenterOf(pos), item);
	}

	/** Whether {@code point} is inside an active beacon area whose item matches {@code item}. */
	public static boolean isInside(Level level, Vec3 point, Predicate<ItemStack> item) {
		Map<BlockPos, ActiveBeacon> map = ACTIVE.get(level);
		if (map == null || map.isEmpty()) {
			return false;
		}
		long now = level.getGameTime();
		for (ActiveBeacon beacon : map.values()) {
			if (now - beacon.lastPulse() <= STALE_AFTER && beacon.contains(point) && item.test(beacon.item())) {
				return true;
			}
		}
		return false;
	}

	/** Every active beacon area containing {@code point} (any item). */
	public static List<ActiveBeacon> at(Level level, Vec3 point) {
		List<ActiveBeacon> found = new ArrayList<>();
		Map<BlockPos, ActiveBeacon> map = ACTIVE.get(level);
		if (map != null) {
			long now = level.getGameTime();
			for (ActiveBeacon beacon : map.values()) {
				if (now - beacon.lastPulse() <= STALE_AFTER && beacon.contains(point)) {
					found.add(beacon);
				}
			}
		}
		return found;
	}

	/** Every active beacon in the level. */
	public static List<ActiveBeacon> all(Level level) {
		Map<BlockPos, ActiveBeacon> map = ACTIVE.get(level);
		return map == null ? List.of() : List.copyOf(map.values());
	}

	// Below: written by Reworked Beacons itself, not for other mods.

	/** Internal: record a beacon's pulse. */
	public static void put(Level level, ActiveBeacon beacon) {
		ACTIVE.computeIfAbsent(level, l -> new HashMap<>()).put(beacon.pos(), beacon);
	}

	/** Internal: a beacon went idle or was removed. */
	public static void remove(Level level, BlockPos pos) {
		Map<BlockPos, ActiveBeacon> map = ACTIVE.get(level);
		if (map != null) {
			map.remove(pos);
		}
	}

	/** Internal: forget a level (server stopping, tests). */
	public static void clear(Level level) {
		ACTIVE.remove(level);
	}
}

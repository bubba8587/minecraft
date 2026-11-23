// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.warding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Active ward areas. */
public final class Wards {
	/** One active ward. */
	public record Ward(AABB area, boolean keepsSleepersAsleep, long expiresAt) {
	}

	private static final Map<ServerLevel, Map<Object, Ward>> WARDS = new WeakHashMap<>();

	private Wards() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(Wards::tick);
	}

	/** Sets or refreshes the ward of source. */
	public static void put(ServerLevel level, Object source, AABB area, boolean keepsSleepersAsleep, int lifetimeTicks) {
		WARDS.computeIfAbsent(level, l -> new HashMap<>())
				.put(source, new Ward(area, keepsSleepersAsleep, level.getGameTime() + Math.max(1, lifetimeTicks)));
		SleeperHook.get().keepEndermenOut(level, source, area, lifetimeTicks);
		if (keepsSleepersAsleep) {
			SleeperHook.get().keepAsleep(level, source, area, lifetimeTicks);
		} else {
			SleeperHook.get().release(level, source);
		}
	}

	public static void remove(ServerLevel level, Object source) {
		Map<Object, Ward> wards = WARDS.get(level);
		if (wards != null) {
			wards.remove(source);
		}
		SleeperHook.get().release(level, source);
		SleeperHook.get().releaseEndermen(level, source);
	}

	private static @Nullable Map<Object, Ward> wardsIn(Level level) {
		if (WARDS.isEmpty() || !(level instanceof ServerLevel server)) {
			return null;
		}
		Map<Object, Ward> wards = WARDS.get(server);
		return wards == null || wards.isEmpty() ? null : wards;
	}

	/** Whether the point is inside any active ward. */
	public static boolean isWarded(Level level, double x, double y, double z) {
		Map<Object, Ward> wards = wardsIn(level);
		if (wards == null) {
			return false;
		}
		for (Ward ward : wards.values()) {
			if (ward.area.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/** Whether a ward stops this natural monster spawn. */
	public static boolean blocksNaturalSpawn(Level level, EntityType<?> type, BlockPos pos) {
		return type.getCategory() == MobCategory.MONSTER && isWarded(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	/** Whether a ward blocks this path node for a kept-out mob. */
	public static boolean blocksEntry(Mob mob, int x, int y, int z) {
		Map<Object, Ward> wards = wardsIn(mob.level());
		if (wards == null || !WardBarrier.keptOut(mob)) {
			return false;
		}
		for (Ward ward : wards.values()) {
			if (ward.area.contains(x + 0.5, y + 0.5, z + 0.5) && !ward.area.contains(mob.position())) {
				return true;
			}
		}
		return false;
	}

	/** Called at the end of every mob's tick. */
	public static void afterMobTick(Mob mob) {
		Map<Object, Ward> wards = wardsIn(mob.level());
		if (wards == null || !WardBarrier.keptOut(mob)) {
			return;
		}
		Vec3 pos = mob.position();
		for (Ward ward : wards.values()) {
			if (ward.area.inflate(3.0).contains(pos)) {
				WardBarrier.afterMove(mob, ward.area, mob.level().getGameTime());
			}
		}
	}

	private static void tick(ServerLevel level) {
		Map<Object, Ward> wards = WARDS.get(level);
		if (wards == null || wards.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		wards.values().removeIf(ward -> ward.expiresAt <= now);
		List<Ward> active = List.copyOf(wards.values());
		if (now % WardingTuning.WARD_SWEEP_TICKS != 0) {
			return;
		}
		for (Ward ward : active) {
			for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, ward.area, m -> EntitySelector.ENTITY_STILL_ALIVE.test(m) && m instanceof Enemy && WardBarrier.keptOut(m) && WardFleeGoal.isGoalDriven(m))) {
				WardFleeGoal.of(mob).leave(ward.area, now + WardingTuning.WARD_SWEEP_TICKS * 2L);
			}
		}
	}
}

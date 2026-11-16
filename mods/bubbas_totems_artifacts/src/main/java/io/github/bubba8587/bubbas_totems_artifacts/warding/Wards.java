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

/**
 * Active ward areas (specs/undead.md "Warding", "Wards"; moved here, specs/totems-artifacts.md). A ward is a box in one level, set by whatever holds a Totem
 * of Warding in place (a Bubba's Reworked Beacons beacon; nothing in this mod depends on where it comes from). While a
 * ward is active:
 * <ul>
 * <li>no natural monster spawns inside it ({@link #blocksNaturalSpawn}; spawners, trial spawners, raids, bosses and
 * commands are untouched);</li>
 * <li>monsters ({@link Enemy}, all treated the same) and happy ghasts don't path into it ({@link #blocksEntry}, used
 * by the node evaluator mixins); ones found inside walk out of it (a sweep every {@link WardingTuning#WARD_SWEEP_TICKS});
 * and whatever doesn't follow paths (flyers, slimes, a ridden happy ghast) is pushed back at the edge every tick
 * ({@link WardBarrier});</li>
 * <li>if it {@code keepsSleepersAsleep} (a full pyramid), sleepers inside never wake ({@link #keepsSleepersAsleep}; the sleepers
 * belong to Bubba's Reworked Mobs, told through compat/SleeperHook, so this does nothing without it);</li>
 * <li>Reworked Mobs' caretaker endermen do no work inside it and a cairn site inside spawns none (also through
 * compat/SleeperHook; specs/endermen.md, E2).</li>
 * </ul>
 * A ward lapses unless its source refreshes it: sources call {@link #put} on their own tick, so a broken or unloaded
 * source needs no cleanup hook. Server thread only.
 */
public final class Wards {
	/** One active ward. {@code expiresAt} is a game time. */
	public record Ward(AABB area, boolean keepsSleepersAsleep, long expiresAt) {
	}

	private static final Map<ServerLevel, Map<Object, Ward>> WARDS = new WeakHashMap<>();

	private Wards() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(Wards::tick);
	}

	/**
	 * Sets or refreshes the ward of {@code source} (any key: a beacon's position, its block entity) for
	 * {@code lifetimeTicks}. Call again before it runs out to keep it.
	 */
	public static void put(ServerLevel level, Object source, AABB area, boolean keepsSleepersAsleep, int lifetimeTicks) {
		WARDS.computeIfAbsent(level, l -> new HashMap<>())
				.put(source, new Ward(area, keepsSleepersAsleep, level.getGameTime() + Math.max(1, lifetimeTicks)));
		// Sleepers and endermen are Reworked Mobs' (when installed): it keeps its own areas per source, on the same lifetime.
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

	/** The wards active in a level right now (a copy). */
	public static List<Ward> active(Level level) {
		Map<Object, Ward> wards = level instanceof ServerLevel server ? WARDS.get(server) : null;
		return wards == null ? List.of() : new ArrayList<>(wards.values());
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

	/** Natural spawning only: a monster-category mob may not spawn inside a ward. */
	public static boolean blocksNaturalSpawn(Level level, EntityType<?> type, BlockPos pos) {
		return type.getCategory() == MobCategory.MONSTER && isWarded(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	/** Sleepers at this block never wake: it is inside a full-pyramid ward. */
	public static boolean keepsSleepersAsleep(Level level, BlockPos pos) {
		Map<Object, Ward> wards = wardsIn(level);
		if (wards == null) {
			return false;
		}
		for (Ward ward : wards.values()) {
			if (ward.keepsSleepersAsleep && ward.area.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * For pathfinding: a monster (or a happy ghast) may not path onto the block {@code (x, y, z)} when it lies in a ward the monster is
	 * not already inside (one already inside may move about, so it can walk out).
	 */
	public static boolean blocksEntry(Mob mob, int x, int y, int z) {
		if (!WardBarrier.keptOut(mob)) {
			return false;
		}
		Map<Object, Ward> wards = wardsIn(mob.level());
		if (wards == null) {
			return false;
		}
		for (Ward ward : wards.values()) {
			if (ward.area.contains(x + 0.5, y + 0.5, z + 0.5) && !ward.area.contains(mob.position())) {
				return true;
			}
		}
		return false;
	}

	/** Called at the end of every mob's tick: one map check when there are no wards. */
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
			for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, ward.area, m -> m instanceof Enemy && m.isAlive() && WardFleeGoal.isGoalDriven(m))) {
				WardFleeGoal.of(mob).leave(ward.area, now + WardingTuning.WARD_SWEEP_TICKS * 2L);
			}
		}
	}
}

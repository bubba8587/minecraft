// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.warding;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_totems_artifacts.mixin.MobAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Vanilla's flee behavior. */
public final class WardFleeGoal extends Goal {
	private static final Map<PathfinderMob, WardFleeGoal> GOALS = new WeakHashMap<>();

	private final PathfinderMob mob;
	private @Nullable Entity scarer;
	private long scaredUntil;
	private @Nullable AABB ward;
	private long leaveUntil;
	private long trappedUntil;
	private @Nullable Path path;

	private WardFleeGoal(PathfinderMob mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	/** This mob's flee goal, added to its goals the first time. */
	public static WardFleeGoal of(PathfinderMob mob) {
		return GOALS.computeIfAbsent(mob, m -> {
			WardFleeGoal goal = new WardFleeGoal(m);
			((MobAccessor) m).bta$goalSelector().addGoal(0, goal);
			return goal;
		});
	}

	/** Whether the mob runs on goals (zombies, skeletons, creepers, spiders, endermen...). */
	public static boolean isGoalDriven(PathfinderMob mob) {
		return GOALS.containsKey(mob) || !((MobAccessor) mob).bta$goalSelector().getAvailableGoals().isEmpty();
	}

	/** Whether this mob is fleeing a totem holder right now (for tests). */
	public static boolean isScared(PathfinderMob mob) {
		WardFleeGoal goal = GOALS.get(mob);
		return goal != null && goal.scarer != null && mob.level().getGameTime() < goal.scaredUntil;
	}

	/** Run from scarer until until (game time). */
	public void scare(Entity scarer, long until) {
		this.scarer = scarer;
		this.scaredUntil = until;
	}

	/** Walk out of area until until (game time). */
	public void leave(AABB area, long until) {
		this.ward = area;
		this.leaveUntil = until;
	}

	private long now() {
		return mob.level().getGameTime();
	}

	private boolean scared() {
		return scarer != null && scarer.isAlive() && now() < scaredUntil
				&& mob.distanceToSqr(scarer) < WardingTuning.FLEE_KEEP_AWAY * WardingTuning.FLEE_KEEP_AWAY;
	}

	private boolean leaving() {
		return ward != null && now() < leaveUntil && now() >= trappedUntil && ward.contains(mob.position());
	}

	/** Inside a ward with no way out (an enclosed room under a beacon pair). */
	public static boolean isTrapped(PathfinderMob mob) {
		WardFleeGoal goal = GOALS.get(mob);
		return goal != null && mob.level().getGameTime() < goal.trappedUntil;
	}

	@Override
	public boolean canUse() {
		Vec3 pos = null;
		if (scared()) {
			pos = fleeTarget();
		}
		if (pos == null && leaving()) {
			Path out = wayOut();
			if (out == null) {
				trappedUntil = now() + WardingTuning.TRAPPED_RECHECK_TICKS;
				return false;
			}
			path = out;
			return true;
		}
		if (pos == null) {
			return false;
		}
		path = mob.getNavigation().createPath(pos.x, pos.y, pos.z, 0);
		return path != null;
	}

	/** A path that ends outside the ward, from a few vanilla positions toward the nearest side, or null. */
	private @Nullable Path wayOut() {
		Vec3 exit = exitPoint(ward, mob.position());
		for (int i = 0; i < WardingTuning.FLEE_TRIES; i++) {
			Vec3 pos = DefaultRandomPos.getPosTowards(mob, 16, 7, exit, Math.PI / 2);
			Path out = pos == null ? null : mob.getNavigation().createPath(pos.x, pos.y, pos.z, 0);
			Node end = out == null ? null : out.getEndNode();
			if (end != null && !ward.contains(end.x + 0.5, end.y + 0.5, end.z + 0.5)) {
				return out;
			}
		}
		return null;
	}

	/** The farthest from the scarer of a few vanilla flee positions, or null if none gets farther away. */
	private @Nullable Vec3 fleeTarget() {
		Vec3 best = null;
		double bestDistance = scarer.distanceToSqr(mob);
		for (int i = 0; i < WardingTuning.FLEE_TRIES; i++) {
			Vec3 pos = DefaultRandomPos.getPosAway(mob, 16, 7, scarer.position());
			if (pos != null && scarer.distanceToSqr(pos) > bestDistance) {
				best = pos;
				bestDistance = scarer.distanceToSqr(pos);
			}
		}
		return best;
	}

	@Override
	public boolean canContinueToUse() {
		return !mob.getNavigation().isDone() && (scared() || leaving());
	}

	@Override
	public void start() {
		mob.getNavigation().moveTo(path, WardingTuning.FLEE_WALK_SPEED);
	}

	@Override
	public void stop() {
		path = null;
	}

	@Override
	public void tick() {
		boolean close = scarer != null && scared() && mob.distanceToSqr(scarer) < 49.0;
		mob.getNavigation().setSpeedModifier(close ? WardingTuning.FLEE_SPRINT_SPEED : WardingTuning.FLEE_WALK_SPEED);
	}

	/** A point a few blocks outside the box through its nearest side (walking mobs leave sideways, never up or down). */
	static Vec3 exitPoint(AABB box, Vec3 from) {
		double margin = 3.0;
		Vec3 out = WardBarrier.exitNormal(box, from, false);
		double x = out.x < 0 ? box.minX - margin : out.x > 0 ? box.maxX + margin : from.x;
		double z = out.z < 0 ? box.minZ - margin : out.z > 0 ? box.maxZ + margin : from.z;
		return new Vec3(x, from.y, z);
	}
}

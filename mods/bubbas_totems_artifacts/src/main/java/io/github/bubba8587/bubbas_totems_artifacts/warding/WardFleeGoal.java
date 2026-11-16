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
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's flee behavior (AvoidEntityGoal, the goal that makes creepers run from cats and skeletons from wolves),
 * for monsters scared by a Totem of Warding: run from the holder of a pulsing totem for a few seconds, or walk out of
 * a ward they were found in. Same picks as vanilla (DefaultRandomPos 16 out, 7 up and down; walk 1.0, sprint 1.2
 * within 7 blocks). AvoidEntityGoal itself can't be reused: it only flees from entities a monster could target, which
 * leaves out creative players and other monsters holding a totem.
 *
 * <p>Not added to every mob: {@link #of} adds it the first time a monster is scared, at priority 0 with the MOVE flag,
 * so it overrides attacking and wandering while it runs and is idle otherwise.
 */
public final class WardFleeGoal extends Goal {
	private static final Map<PathfinderMob, WardFleeGoal> GOALS = new WeakHashMap<>();

	private final PathfinderMob mob;
	private @Nullable Entity scarer;
	private long scaredUntil;
	private @Nullable AABB ward;
	private long leaveUntil;
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

	/**
	 * Whether the mob runs on goals (zombies, skeletons, creepers, spiders, endermen...). Brain-driven monsters
	 * (piglins, hoglins, the warden, breezes) have no goals, and a goal would fight their brain for the navigation.
	 */
	public static boolean isGoalDriven(PathfinderMob mob) {
		return GOALS.containsKey(mob) || !((MobAccessor) mob).bta$goalSelector().getAvailableGoals().isEmpty();
	}

	/** Whether this mob is fleeing a totem holder right now (for tests). */
	public static boolean isScared(PathfinderMob mob) {
		WardFleeGoal goal = GOALS.get(mob);
		return goal != null && goal.scarer != null && mob.level().getGameTime() < goal.scaredUntil;
	}

	/** Run from {@code scarer} until {@code until} (game time). */
	public void scare(Entity scarer, long until) {
		this.scarer = scarer;
		this.scaredUntil = until;
	}

	/** Walk out of {@code area} until {@code until} (game time); the ward sweep refreshes it while the mob is inside. */
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
		return ward != null && now() < leaveUntil && ward.contains(mob.position());
	}

	@Override
	public boolean canUse() {
		Vec3 pos = null;
		if (scared()) {
			pos = DefaultRandomPos.getPosAway(mob, 16, 7, scarer.position());
			if (pos != null && scarer.distanceToSqr(pos) < scarer.distanceToSqr(mob)) {
				pos = null;
			}
		}
		if (pos == null && leaving()) {
			pos = DefaultRandomPos.getPosTowards(mob, 16, 7, exitPoint(ward, mob.position()), Math.PI / 2);
		}
		if (pos == null) {
			return false;
		}
		path = mob.getNavigation().createPath(pos.x, pos.y, pos.z, 0);
		return path != null;
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
		double[] distances = {from.x - box.minX, box.maxX - from.x, from.z - box.minZ, box.maxZ - from.z};
		int best = 0;
		for (int i = 1; i < distances.length; i++) {
			if (distances[i] < distances[best]) {
				best = i;
			}
		}
		return switch (best) {
			case 0 -> new Vec3(box.minX - margin, from.y, from.z);
			case 1 -> new Vec3(box.maxX + margin, from.y, from.z);
			case 2 -> new Vec3(from.x, from.y, box.minZ - margin);
			default -> new Vec3(from.x, from.y, box.maxZ + margin);
		};
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.entity;

import java.util.EnumSet;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Idle woken zombies drift into groups (specs/undead.md "Tuning"): every {@link UndeadTuning#DRIFT_INTERVAL_TICKS}
 * (each zombie on its own random offset) a zombie with no target looks for other woken zombies nearby (one entity
 * query), picks the nearest one's group and walks to the group's centre, stopping a few blocks short. Lower priority
 * than attacking and grabbing loot, above strolling, so a target always wins.
 */
public class DriftToHordeGoal extends Goal {
	private final WokenZombie zombie;
	private final double speed;
	private int nextCheck = -1;
	private @Nullable Vec3 goal;

	public DriftToHordeGoal(WokenZombie zombie, double speed) {
		this.zombie = zombie;
		this.speed = speed;
		this.setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (zombie.getTarget() != null || zombie.isPassenger()) {
			return false;
		}
		if (nextCheck < 0) {
			nextCheck = zombie.tickCount + zombie.getRandom().nextInt(UndeadTuning.DRIFT_INTERVAL_TICKS);
		}
		if (zombie.tickCount < nextCheck) {
			return false;
		}
		nextCheck = zombie.tickCount + UndeadTuning.DRIFT_INTERVAL_TICKS;
		goal = findGroupCentre();
		return goal != null && zombie.position().distanceToSqr(goal) > square(UndeadTuning.DRIFT_STOP_DISTANCE + 1.0);
	}

	private @Nullable Vec3 findGroupCentre() {
		double range = UndeadTuning.DRIFT_RANGE;
		List<WokenZombie> near = zombie.level().getEntitiesOfClass(WokenZombie.class, zombie.getBoundingBox().inflate(range, 6.0, range),
				other -> other != zombie && other.isAlive());
		if (near.isEmpty()) {
			return null;
		}
		WokenZombie nearest = near.getFirst();
		for (WokenZombie other : near) {
			if (other.distanceToSqr(zombie) < nearest.distanceToSqr(zombie)) {
				nearest = other;
			}
		}
		double groupSq = square(UndeadTuning.DRIFT_GROUP_RADIUS);
		Vec3 sum = Vec3.ZERO;
		int count = 0;
		for (WokenZombie other : near) {
			if (other.distanceToSqr(nearest) <= groupSq) {
				sum = sum.add(other.position());
				count++;
			}
		}
		return sum.scale(1.0 / count);
	}

	@Override
	public void start() {
		if (goal != null) {
			zombie.getNavigation().moveTo(goal.x, goal.y, goal.z, speed);
		}
	}

	@Override
	public boolean canContinueToUse() {
		return goal != null && zombie.getTarget() == null && !zombie.getNavigation().isDone()
				&& zombie.position().distanceToSqr(goal) > square(UndeadTuning.DRIFT_STOP_DISTANCE);
	}

	@Override
	public void stop() {
		zombie.getNavigation().stop();
		goal = null;
	}

	private static double square(double value) {
		return value * value;
	}
}

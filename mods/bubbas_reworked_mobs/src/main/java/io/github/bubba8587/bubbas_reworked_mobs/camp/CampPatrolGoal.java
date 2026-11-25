// dte:B15,B17,B21,C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A camp patrol on the move (specs/monster-camps.md "Camp patrols"): vanilla's
 * {@code PatrollingMonster.LongDistancePatrolGoal} for our mobs. The leader walks toward its patrol target in steps,
 * hands each step to the members round it, and picks a new target on arrival; a member with no companions left
 * stops patrolling.
 */
public final class CampPatrolGoal extends Goal {
	/** Each patrol member's current target (goals aren't saved; a reloaded leader picks a new one). */
	private static final Map<Mob, BlockPos> TARGETS = Collections.synchronizedMap(new WeakHashMap<>());

	private final Mob mob;
	private final String patrol;
	private long cooldownUntil = -1L;
	private boolean done;

	public CampPatrolGoal(Mob mob, String patrol) {
		this.mob = mob;
		this.patrol = patrol;
		setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	/** The patrol tag ({@code PATROL_TAG:<camp id>}) a mob carries, or null. */
	public static @Nullable String patrolOf(Mob mob) {
		for (String tag : mob.entityTags()) {
			if (tag.startsWith(CampTuning.PATROL_TAG + ":")) {
				return tag;
			}
		}
		return null;
	}

	/** Gives a patrol member its target. */
	public static void setTarget(Mob mob, BlockPos target) {
		TARGETS.put(mob, target.immutable());
	}

	public static @Nullable BlockPos target(Mob mob) {
		return TARGETS.get(mob);
	}

	private boolean leader() {
		return mob.entityTags().contains(CampTuning.PATROL_LEADER_TAG);
	}

	@Override
	public boolean canUse() {
		if (done || mob.getTarget() != null || mob.hasControllingPassenger() || mob.level().getGameTime() < cooldownUntil) {
			return false;
		}
		if (leader() && target(mob) == null) {
			newTarget();
		}
		return target(mob) != null;
	}

	@Override
	public void tick() {
		PathNavigation navigation = mob.getNavigation();
		if (!navigation.isDone()) {
			return;
		}
		boolean leader = leader();
		List<Mob> companions = companions();
		BlockPos target = target(mob);
		if (companions.isEmpty() || target == null) {
			done = companions.isEmpty();
			return;
		}
		if (leader && target.closerToCenterThan(mob.position(), CampTuning.PATROL_ARRIVE)) {
			newTarget();
			return;
		}
		// Vanilla's step: aim a little to the side of the target, a step at a time.
		Vec3 longTarget = Vec3.atBottomCenterOf(target);
		Vec3 self = mob.position();
		longTarget = self.subtract(longTarget).yRot(90.0F).scale(0.4).add(longTarget);
		Vec3 step = longTarget.subtract(self).normalize().scale(CampTuning.PATROL_STEP).add(self);
		BlockPos pathTarget = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(step));
		if (Math.abs(pathTarget.getY() - mob.getBlockY()) > CampTuning.PATROL_OVERHANG) {
			pathTarget = pathTarget.atY(mob.getBlockY()); // an overhang: stay on this level
		}
		double speed = leader ? CampTuning.PATROL_LEADER_SPEED : CampTuning.PATROL_SPEED;
		if (!navigation.moveTo(pathTarget.getX(), pathTarget.getY(), pathTarget.getZ(), speed)
				&& !navigation.moveTo(target.getX(), mob.getBlockY(), target.getZ(), speed)) {
			moveRandomly();
			cooldownUntil = mob.level().getGameTime() + CampTuning.PATROL_STUCK_COOLDOWN;
		} else if (leader) {
			for (Mob companion : companions) {
				setTarget(companion, pathTarget);
			}
		}
	}

	/** The leader's next target: a leg off, pulled back toward the camp when it would leave the territory. */
	private void newTarget() {
		RandomSource random = mob.getRandom();
		double angle = random.nextDouble() * Math.PI * 2.0;
		int leg = CampTuning.PATROL_LEG_MIN + random.nextInt(CampTuning.PATROL_LEG_MAX - CampTuning.PATROL_LEG_MIN + 1);
		BlockPos next = mob.blockPosition().offset((int) Math.round(Math.cos(angle) * leg), 0, (int) Math.round(Math.sin(angle) * leg));
		BlockPos centre = CampPatrols.campCentre(mob);
		if (centre != null) {
			double dx = next.getX() - centre.getX();
			double dz = next.getZ() - centre.getZ();
			double far = Math.sqrt(dx * dx + dz * dz);
			double max = CampTuning.PATROL_TERRITORY - CampTuning.PATROL_LEG_MIN;
			if (far > max) {
				next = new BlockPos(centre.getX() + (int) (dx / far * max), next.getY(), centre.getZ() + (int) (dz / far * max));
			}
		}
		setTarget(mob, next);
	}

	private List<Mob> companions() {
		return mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(CampTuning.PATROL_COMPANION_RANGE),
				other -> other != mob && other.isAlive() && other.entityTags().contains(patrol));
	}

	private void moveRandomly() {
		RandomSource random = mob.getRandom();
		BlockPos to = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				mob.blockPosition().offset(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
		if (Math.abs(to.getY() - mob.getBlockY()) > CampTuning.PATROL_OVERHANG) {
			to = to.atY(mob.getBlockY());
		}
		mob.getNavigation().moveTo(to.getX(), to.getY(), to.getZ(), CampTuning.PATROL_SPEED);
	}
}

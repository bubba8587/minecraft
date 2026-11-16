// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The nervous watcher (specs/creepers.md): walks to a spot half behind cover where it can see what it watches and
 * stares; peekaboo (slides about 95 % behind the cover while the watched player looks at it, eases back out when they
 * look away); on open ground holds the "uncomfortably close" distance and backs up when you come closer; walks to
 * see its target again when it loses sight; backs behind cover from a sneaking player who closes in. Replaces
 * vanilla's MeleeAttackGoal slot (priority 4) for uncharged creepers; vanilla's strolling and looking around run when
 * it has nothing to watch.
 */
public final class CreeperWatchGoal extends Goal {
	private final Creeper creeper;
	private int ticks;
	private int nextSearch;
	private int lastSeen;
	private double lastDistance;
	private boolean hiding;
	private @Nullable LivingEntity spotFor;

	public CreeperWatchGoal(Creeper creeper) {
		this.creeper = creeper;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	private CreeperMind mind() {
		return CreeperAccess.mind(creeper);
	}

	@Override
	public boolean canUse() {
		return CreeperMind.active(creeper) && !mind().isFusing() && mind().watched() != null;
	}

	@Override
	public void start() {
		ticks = 0;
		nextSearch = 0;
		lastSeen = 0;
		hiding = false;
		LivingEntity target = mind().watched();
		lastDistance = target == null ? 0.0 : Math.sqrt(creeper.distanceToSqr(target));
	}

	@Override
	public void stop() {
		creeper.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		CreeperMind mind = mind();
		LivingEntity target = mind.watched();
		if (target == null) {
			return;
		}
		ticks++;
		if (spotFor != target) {
			spotFor = target;
			hiding = false;
		}
		creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = Math.sqrt(creeper.distanceToSqr(target));
		boolean sees = creeper.getSensing().hasLineOfSight(target);
		if (sees) {
			lastSeen = ticks;
		}

		// Sneaking up on it: it stares and backs behind cover (owner, "Sneaking up on it").
		if (ticks % 10 == 0) {
			boolean sneaking = target instanceof Player && target.isShiftKeyDown() && distance < CreeperTuning.SNEAK_NOTICE_RANGE;
			boolean closing = distance < lastDistance - 0.1;
			if (sneaking && closing && !hiding) {
				hiding = true;
				mind.spot = null;
				nextSearch = ticks;
			} else if (!sneaking && hiding) {
				hiding = false;
				mind.spot = null;
				nextSearch = ticks;
			}
			lastDistance = distance;
		}

		// Too close: back up (owner C5: dashing at it makes it back off).
		if (distance < CreeperTuning.COMFORT_DISTANCE - CreeperTuning.BACKOFF_MARGIN) {
			mind.spot = null;
			if (creeper.getNavigation().isDone() || ticks % 10 == 0) {
				Vec3 away = DefaultRandomPos.getPosAway(creeper, 6, 3, target.position());
				if (away != null) {
					creeper.getNavigation().moveTo(away.x, away.y, away.z, CreeperTuning.BACKOFF_SPEED);
				}
			}
			return;
		}

		CoverFinder.PeekSpot spot = mind.spot;
		if (spot != null) {
			tickSpot(mind, spot, target, distance);
			return;
		}
		if (ticks >= nextSearch) {
			nextSearch = ticks + CreeperTuning.SPOT_SEARCH_INTERVAL;
			CoverFinder.PeekSpot found = search(target, distance);
			if (found != null && creeper.getNavigation().moveTo(found.cell().getX() + 0.5, found.cell().getY(), found.cell().getZ() + 0.5, 0,
					CreeperTuning.WATCH_SPEED)) {
				mind.setSpot(found, false);
				return;
			}
		}
		// No cover in reach.
		if (hiding) {
			if (creeper.getNavigation().isDone()) {
				Vec3 away = DefaultRandomPos.getPosAway(creeper, 8, 3, target.position());
				if (away != null) {
					creeper.getNavigation().moveTo(away.x, away.y, away.z, CreeperTuning.WATCH_SPEED);
				}
			}
		} else if (ticks - lastSeen > CreeperTuning.LOST_VIEW_TICKS || distance > CreeperTuning.COMFORT_DISTANCE + 1.5) {
			// Out of sight (reposition) or far: creep closer, up to the uncomfortable distance.
			if (creeper.getNavigation().isDone() || ticks % 20 == 0) {
				creeper.getNavigation().moveTo(target, CreeperTuning.WATCH_SPEED);
			}
		} else if (distance <= CreeperTuning.COMFORT_DISTANCE + 0.5) {
			creeper.getNavigation().stop();
		}
	}

	private CoverFinder.@Nullable PeekSpot search(LivingEntity target, double distance) {
		Vec3 eye = target.getEyePosition();
		if (hiding) {
			return CoverFinder.find(creeper.level(), creeper.getType().getDimensions(), creeper.blockPosition(), eye,
					distance + 1.5, distance + 8.0, distance + 4.0);
		}
		return CoverFinder.find(creeper.level(), creeper.getType().getDimensions(), creeper.blockPosition(), eye,
				CreeperTuning.COMFORT_DISTANCE, CreeperTuning.PEEK_MAX_DISTANCE, CreeperTuning.COMFORT_DISTANCE + 1.0);
	}

	private void tickSpot(CreeperMind mind, CoverFinder.PeekSpot spot, LivingEntity target, double distance) {
		if (!mind.spotReached) {
			Vec3 centre = Vec3.atBottomCenterOf(spot.cell());
			double dx = creeper.getX() - centre.x;
			double dz = creeper.getZ() - centre.z;
			if (dx * dx + dz * dz < 0.8 * 0.8 && Math.abs(creeper.getY() - centre.y) < 1.0) {
				mind.spotReached = true;
				creeper.getNavigation().stop();
			} else if (creeper.getNavigation().isDone()) {
				mind.spot = null; // couldn't get there; search again
				return;
			} else {
				return;
			}
		}
		// The target moves: re-check the spot still half-hides us from it.
		if (ticks % CreeperTuning.SPOT_RECHECK_INTERVAL == 0) {
			boolean inRange = hiding || (distance >= CreeperTuning.COMFORT_DISTANCE - CreeperTuning.BACKOFF_MARGIN
					&& distance <= CreeperTuning.PEEK_MAX_DISTANCE + 2.0);
			CoverFinder.PeekSpot fresh = inRange ? CoverFinder.evaluate(creeper.level(), creeper.getType().getDimensions(), spot.cell(), target.getEyePosition()) : null;
			if (fresh == null) {
				mind.spot = null;
				nextSearch = ticks;
				return;
			}
			mind.spot = fresh;
			spot = fresh;
		}
		Vec3 wanted = hiding || lookedAtBy(target) ? spot.hidden() : spot.half();
		Vec3 offset = new Vec3(wanted.x - creeper.getX(), 0.0, wanted.z - creeper.getZ());
		double length = offset.length();
		if (length > 0.01) {
			creeper.move(MoverType.SELF, offset.scale(Math.min(1.0, CreeperTuning.SLIDE_SPEED / length)));
		}
	}

	/** Peekaboo: the watched player is looking toward the creeper (within the look cone). */
	private boolean lookedAtBy(LivingEntity target) {
		if (!(target instanceof Player)) {
			return false;
		}
		Vec3 to = creeper.getEyePosition().subtract(target.getEyePosition());
		double length = to.length();
		if (length < 1.0E-3) {
			return true;
		}
		double cos = target.getViewVector(1.0F).dot(to) / length;
		return cos >= Math.cos(Math.toRadians(CreeperTuning.PLAYER_LOOK_HALF_ANGLE));
	}
}

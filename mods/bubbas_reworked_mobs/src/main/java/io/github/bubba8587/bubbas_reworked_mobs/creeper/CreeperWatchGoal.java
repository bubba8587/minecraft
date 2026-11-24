// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import java.util.EnumSet;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.sense.Gaze;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The nervous watcher (specs/creepers.md): holds a spot half behind cover where it can see what it watches and
 * stares; peekaboo; creeps to closer cover while not looked at; runs to farther cover when the player comes at it;
 * backs behind cover from a sneaking player; by day a watcher heads out of sight to leave; open ground only with no
 * cover at all. Replaces vanilla's MeleeAttackGoal slot (priority 4) for uncharged creepers; vanilla's strolling and
 * looking around run when it has nothing to watch.
 */
public final class CreeperWatchGoal extends Goal {
	/** What it is doing with its spot. */
	private enum Plan {
		WATCH, APPROACH, FLEE, HIDE, LEAVE
	}

	private final Creeper creeper;
	private int ticks;
	private int nextSearch;
	private int lastSeen;
	private int lastLooked;
	private int nextApproach;
	private int failedSearches;
	private double fleeFrom;
	private Plan plan = Plan.WATCH;
	private CoverFinder.@Nullable PeekSpot previous;
	private @Nullable Vec3 targetBefore;
	private @Nullable Vec3 targetNow;
	private @Nullable LivingEntity spotFor;

	public CreeperWatchGoal(Creeper creeper) {
		this.creeper = creeper;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		creeper.getNavigation().setRequiredPathLength(CreeperTuning.WATCH_PATH_LENGTH);
	}

	private CreeperMind mind() {
		return CreeperAccess.mind(creeper);
	}

	private EntityDimensions body() {
		return creeper.getType().getDimensions();
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
		lastLooked = 0;
		nextApproach = CreeperTuning.APPROACH_INTERVAL;
		failedSearches = 0;
		plan = Plan.WATCH;
		previous = null;
		targetBefore = null;
		targetNow = null;
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
			plan = Plan.WATCH;
			previous = null;
			targetBefore = null;
			targetNow = null;
			failedSearches = 0;
			mind.setOpenGround(false);
		}
		creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);
		double distance = Math.sqrt(creeper.distanceToSqr(target));
		boolean sees = creeper.getSensing().hasLineOfSight(target);
		if (sees) {
			lastSeen = ticks;
		}
		boolean looked = lookedAtBy(target);
		if (looked) {
			lastLooked = ticks;
			mind.lookedAt(creeper.level().getGameTime());
		}
		if (mind.leaving() && plan != Plan.LEAVE && plan != Plan.FLEE) {
			startLeaving(mind, target);
		}
		if (ticks % (CreeperTuning.APPROACH_WINDOW / 2) == 0) {
			noticeApproach(mind, target, distance);
		}

		CoverFinder.PeekSpot spot = mind.spot;
		if (spot != null) {
			tickSpot(mind, spot, target, distance, looked);
			return;
		}
		if (ticks >= nextSearch) {
			nextSearch = ticks + CreeperTuning.SPOT_SEARCH_INTERVAL;
			CoverFinder.PeekSpot found = search(target, distance);
			if (found != null && creeper.getNavigation().moveTo(found.cell().getX() + 0.5, found.cell().getY(), found.cell().getZ() + 0.5, 0, speed())) {
				mind.setSpot(found, false);
				failedSearches = 0;
				mind.setOpenGround(false);
				return;
			}
			failedSearches++;
			if (plan == Plan.APPROACH) {
				plan = Plan.WATCH;
			}
			if (plan == Plan.WATCH && failedSearches >= CreeperTuning.OPEN_GROUND_AFTER_FAILED_SEARCHES) {
				mind.setOpenGround(true);
			}
		}
		// No spot.
		switch (plan) {
			case FLEE -> {
				if (distance >= fleeFrom + CreeperTuning.FLEE_MIN_GAIN) {
					plan = Plan.WATCH;
				} else if (creeper.getNavigation().isDone() && !walkAway(target, 10, CreeperTuning.FLEE_SPEED)) {
					plan = Plan.WATCH;
				}
			}
			case HIDE -> {
				if (creeper.getNavigation().isDone()) {
					walkAway(target, 8, CreeperTuning.WATCH_SPEED);
				}
			}
			case LEAVE -> {
				// No hidden spot: walk away until out of sight (the mind takes it once no one sees it).
				if (creeper.getNavigation().isDone()) {
					walkAway(target, 16, CreeperTuning.WATCH_SPEED);
				}
			}
			default -> {
				if (mind.openGround()) {
					tickOpenGround(target, distance, sees, looked);
				}
			}
		}
	}

	/** Open ground with no cover at all (owner C5): holds the uncomfortable distance, backs off when you come closer. */
	private void tickOpenGround(LivingEntity target, double distance, boolean sees, boolean looked) {
		if (distance < CreeperTuning.COMFORT_DISTANCE - CreeperTuning.BACKOFF_MARGIN) {
			if (creeper.getNavigation().isDone() || ticks % 10 == 0) {
				walkAway(target, 6, CreeperTuning.BACKOFF_SPEED);
			}
		} else if (sees && distance < CreeperTuning.COMFORT_DISTANCE + CreeperTuning.OPEN_LOOKED_AT_EXTRA && looked) {
			if (creeper.getNavigation().isDone()) {
				walkAway(target, 5, CreeperTuning.WATCH_SPEED);
			}
		} else if (ticks - lastSeen > CreeperTuning.LOST_VIEW_TICKS || distance > CreeperTuning.COMFORT_DISTANCE + 1.5) {
			if (creeper.getNavigation().isDone() || ticks % 20 == 0) {
				creeper.getNavigation().moveTo(target, CreeperTuning.WATCH_SPEED);
			}
		} else if (distance <= CreeperTuning.COMFORT_DISTANCE + 0.5) {
			creeper.getNavigation().stop();
		}
	}

	private boolean walkAway(LivingEntity target, int range, double speed) {
		Vec3 away = DefaultRandomPos.getPosAway(creeper, range, Math.max(2, range / 2), target.position());
		return away != null && creeper.getNavigation().moveTo(away.x, away.y, away.z, speed);
	}

	private double speed() {
		return plan == Plan.FLEE ? CreeperTuning.FLEE_SPEED : CreeperTuning.WATCH_SPEED;
	}

	/** The player's own movement toward it over the last APPROACH_WINDOW ticks (its own movement doesn't count). */
	private void noticeApproach(CreeperMind mind, LivingEntity target, double distance) {
		Vec3 now = target.position();
		Vec3 before = targetBefore;
		targetBefore = targetNow;
		targetNow = now;
		if (before == null || !(target instanceof Player)) {
			return;
		}
		Vec3 here = creeper.position();
		double closing = before.subtract(here).horizontalDistance() - now.subtract(here).horizontalDistance();
		if (target.isShiftKeyDown()) {
			if (closing > CreeperTuning.SNEAK_CLOSING && distance < CreeperTuning.SNEAK_NOTICE_RANGE && plan != Plan.HIDE && plan != Plan.FLEE && plan != Plan.LEAVE) {
				plan = Plan.HIDE;
				mind.spot = null;
				nextSearch = ticks;
			}
			return;
		}
		if (plan == Plan.HIDE) {
			plan = Plan.WATCH;
		}
		boolean running = plan == Plan.FLEE && (mind.spot == null || !mind.spotReached);
		if (closing >= CreeperTuning.FLEE_CLOSING && distance < CreeperTuning.FLEE_NOTICE_RANGE && !running) {
			plan = Plan.FLEE;
			fleeFrom = distance;
			previous = null;
			mind.spot = null;
			nextSearch = ticks;
			failedSearches = 0;
		}
	}

	private void startLeaving(CreeperMind mind, LivingEntity target) {
		plan = Plan.LEAVE;
		previous = null;
		CoverFinder.PeekSpot spot = mind.spot;
		if (spot == null || !mind.spotReached || !CoverFinder.hiddenOutOfSight(creeper.level(), body(), spot, target.getEyePosition())) {
			mind.spot = null;
			nextSearch = ticks;
		}
	}

	private CoverFinder.@Nullable PeekSpot search(LivingEntity target, double distance) {
		Vec3 eye = target.getEyePosition();
		double far = CreeperTuning.WATCH_RANGE - 1.0;
		return switch (plan) {
			case HIDE -> find(eye, distance + 1.5, distance + 8.0, distance + 4.0);
			case FLEE -> {
				double max = Math.min(far, Math.max(CreeperTuning.PEEK_MAX_DISTANCE, distance + CreeperTuning.FLEE_MAX_GAIN));
				yield find(eye, distance + CreeperTuning.FLEE_MIN_GAIN, max, Math.min(max, distance + CreeperTuning.FLEE_PREFERRED_GAIN));
			}
			case LEAVE -> {
				for (CoverFinder.PeekSpot spot : CoverFinder.findAll(creeper.level(), body(), creeper.blockPosition(), eye,
						CreeperTuning.APPROACH_MIN_DISTANCE, far, Math.min(far, distance + CreeperTuning.FLEE_PREFERRED_GAIN), CreeperTuning.APPROACH_CANDIDATES)) {
					if (CoverFinder.hiddenOutOfSight(creeper.level(), body(), spot, eye)) {
						yield spot;
					}
				}
				yield null;
			}
			default -> find(eye, CreeperTuning.APPROACH_MIN_DISTANCE, CreeperTuning.PEEK_MAX_DISTANCE,
					Math.clamp(distance, CreeperTuning.APPROACH_MIN_DISTANCE + 1.0, CreeperTuning.PEEK_PREFERRED_DISTANCE));
		};
	}

	private CoverFinder.@Nullable PeekSpot find(Vec3 eye, double min, double max, double preferred) {
		return CoverFinder.find(creeper.level(), body(), creeper.blockPosition(), eye, min, max, preferred);
	}

	private void tickSpot(CreeperMind mind, CoverFinder.PeekSpot spot, LivingEntity target, double distance, boolean looked) {
		if (!mind.spotReached) {
			Vec3 centre = Vec3.atBottomCenterOf(spot.cell());
			double dx = creeper.getX() - centre.x;
			double dz = creeper.getZ() - centre.z;
			if (dx * dx + dz * dz < 0.8 * 0.8 && Math.abs(creeper.getY() - centre.y) < 1.0) {
				mind.spotReached = true;
				creeper.getNavigation().stop();
				arrived(mind);
			} else if (creeper.getNavigation().isDone()) {
				mind.spot = null;
				if (plan == Plan.APPROACH) {
					plan = Plan.WATCH;
				}
				return;
			} else {
				if (plan == Plan.APPROACH && looked && CoverFinder.clear(creeper.level(), target.getEyePosition(), creeper.getEyePosition())) {
					abortApproach(mind);
				}
				return;
			}
		}
		// The target moves: re-check the spot still half-hides us from it.
		if (ticks % CreeperTuning.SPOT_RECHECK_INTERVAL == 0) {
			boolean inRange = plan == Plan.WATCH
					? distance >= CreeperTuning.APPROACH_MIN_DISTANCE - 1.0 && distance <= CreeperTuning.PEEK_MAX_DISTANCE + 2.0
					: distance <= CreeperTuning.WATCH_RANGE;
			Vec3 eye = target.getEyePosition();
			CoverFinder.PeekSpot fresh = inRange ? CoverFinder.evaluate(creeper.level(), body(), spot.cell(), eye) : null;
			if (fresh != null && plan == Plan.LEAVE && !CoverFinder.hiddenOutOfSight(creeper.level(), body(), fresh, eye)) {
				fresh = null;
			}
			if (fresh == null) {
				mind.spot = null;
				nextSearch = ticks;
				return;
			}
			mind.spot = fresh;
			spot = fresh;
		}
		Vec3 wanted = plan == Plan.HIDE || plan == Plan.LEAVE || looked ? spot.hidden() : spot.half();
		Vec3 offset = new Vec3(wanted.x - creeper.getX(), 0.0, wanted.z - creeper.getZ());
		double length = offset.length();
		if (length > 0.01) {
			creeper.move(MoverType.SELF, offset.scale(Math.min(1.0, CreeperTuning.SLIDE_SPEED / length)));
		}
		if (plan == Plan.WATCH && ticks >= nextApproach && ticks - lastLooked >= CreeperTuning.APPROACH_UNSEEN_TICKS
				&& distance > CreeperTuning.APPROACH_MIN_DISTANCE + CreeperTuning.APPROACH_STEP) {
			tryApproach(mind, spot, target, distance);
		}
	}

	private void arrived(CreeperMind mind) {
		if (plan == Plan.FLEE) {
			plan = mind.leaving() ? Plan.LEAVE : Plan.WATCH;
			nextApproach = ticks + CreeperTuning.APPROACH_AFTER_FLEE;
		} else if (plan == Plan.APPROACH) {
			plan = Plan.WATCH;
			previous = null;
		}
	}

	/** Caught walking up in the open: back to the spot it came from. */
	private void abortApproach(CreeperMind mind) {
		plan = Plan.WATCH;
		nextApproach = ticks + CreeperTuning.APPROACH_INTERVAL;
		CoverFinder.PeekSpot back = previous;
		previous = null;
		if (back != null && creeper.getNavigation().moveTo(back.cell().getX() + 0.5, back.cell().getY(), back.cell().getZ() + 0.5, 0, CreeperTuning.WATCH_SPEED)) {
			mind.setSpot(back, false);
		} else {
			creeper.getNavigation().stop();
			mind.spot = null;
		}
	}

	/** Creeping closer: a peek spot nearer the target, the one whose route and hidden place are least in its view. */
	private void tryApproach(CreeperMind mind, CoverFinder.PeekSpot current, LivingEntity target, double distance) {
		nextApproach = ticks + CreeperTuning.APPROACH_INTERVAL + creeper.getRandom().nextInt(CreeperTuning.APPROACH_JITTER + 1);
		Vec3 eye = target.getEyePosition();
		List<CoverFinder.PeekSpot> spots = CoverFinder.findAll(creeper.level(), body(), creeper.blockPosition(), eye,
				CreeperTuning.APPROACH_MIN_DISTANCE, distance - CreeperTuning.APPROACH_STEP,
				Math.max(CreeperTuning.APPROACH_MIN_DISTANCE, distance - CreeperTuning.APPROACH_STRIDE), CreeperTuning.APPROACH_CANDIDATES);
		CoverFinder.PeekSpot best = null;
		Path bestPath = null;
		double bestScore = Double.MAX_VALUE;
		for (CoverFinder.PeekSpot spot : spots) {
			Path path = creeper.getNavigation().createPath(spot.cell(), 0);
			if (path == null || !path.canReach()) {
				continue;
			}
			double score = routeInView(path, eye) + (CoverFinder.hiddenOutOfSight(creeper.level(), body(), spot, eye) ? 0.0 : 1.0);
			if (score < bestScore) {
				best = spot;
				bestPath = path;
				bestScore = score;
				if (score == 0.0) {
					break;
				}
			}
		}
		if (best != null && creeper.getNavigation().moveTo(bestPath, CreeperTuning.WATCH_SPEED)) {
			previous = current;
			mind.setSpot(best, false);
			plan = Plan.APPROACH;
		}
	}

	/** The share of points along a route its target could see the creeper at. */
	private double routeInView(Path path, Vec3 eye) {
		int nodes = path.getNodeCount();
		if (nodes == 0) {
			return 0.0;
		}
		int samples = Math.min(CreeperTuning.ROUTE_SAMPLES, nodes);
		int visible = 0;
		double eyeHeight = body().eyeHeight();
		for (int i = 0; i < samples; i++) {
			Node node = path.getNode((int) ((i + 0.5) * nodes / samples));
			if (CoverFinder.clear(creeper.level(), eye, new Vec3(node.x + 0.5, node.y + eyeHeight, node.z + 0.5))) {
				visible++;
			}
		}
		return visible / (double) samples;
	}

	/**
	 * The watched player is looking toward the creeper: within the look cone of any point of its body, or of where it
	 * peeks from (Gaze). No sight check.
	 */
	private boolean lookedAtBy(LivingEntity target) {
		if (!(target instanceof Player)) {
			return false;
		}
		Vec3 from = target.getEyePosition();
		float width = creeper.getBbWidth();
		float height = creeper.getBbHeight();
		double eye = creeper.getEyeHeight();
		if (Gaze.looksAt(creeper.level(), target, CreeperTuning.PLAYER_LOOK_HALF_ANGLE, false, Gaze.bodyPoints(creeper.position(), width, height, eye, from))) {
			return true;
		}
		CoverFinder.PeekSpot spot = mind().spot();
		return spot != null && Gaze.looksAt(creeper.level(), target, CreeperTuning.PLAYER_LOOK_HALF_ANGLE, false, Gaze.bodyPoints(spot.half(), width, height, eye, from));
	}
}

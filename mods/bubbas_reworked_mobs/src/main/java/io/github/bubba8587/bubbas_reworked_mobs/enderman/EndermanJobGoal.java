// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The caretaker's work loop (specs/endermen.md, "Two jobs"), replacing vanilla's random take-block and leave-block
 * goals: carrying a block, it takes it to a spot that needs it (a cairn's missing block, or a hole whose original
 * terrain is its family); empty-handed, it takes a block that doesn't belong, or jumps on farmland that should be dirt.
 * It walks, and teleports when far or stuck. If no spot needs what it carries it keeps it (and the block vanishes
 * when it despawns); a builder whose block can't go in yet sets it down near its cairn. With nothing to do at a complete cairn
 * it stands vigil in a ring around it, staring at it. Off while angry, and off when mob griefing is off (vanilla's rule for enderman blocks).
 */
public final class EndermanJobGoal extends Goal {
	private final Enderman enderman;
	private final EndermanMind mind;
	private int nextSearch;
	private int nextPath;
	private double bestDistance;
	private int stuckTicks;
	/** What a vigil stares at: the middle of its cairn. */
	private BlockPos vigilCentre = BlockPos.ZERO;
	/** Where a vigil's gaze rests now (drifts over the cairn). */
	private Vec3 vigilGaze = Vec3.ZERO;
	private int nextGaze;

	public EndermanJobGoal(Enderman enderman, EndermanMind mind) {
		this.enderman = enderman;
		this.mind = mind;
		this.nextSearch = (enderman.getId() & 15);
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
	}

	private boolean allowed(ServerLevel level) {
		return enderman.getTarget() == null && level.getGameRules().get(GameRules.MOB_GRIEFING) && !enderman.isPassenger();
	}

	@Override
	public boolean canUse() {
		if (!(enderman.level() instanceof ServerLevel level) || !allowed(level)) {
			return false;
		}
		if (mind.job() != null) {
			return true;
		}
		if (enderman.tickCount < nextSearch) {
			return false;
		}
		nextSearch = enderman.tickCount + EndermanTuning.JOB_SCAN_INTERVAL + enderman.getRandom().nextInt(10);
		return plan(level);
	}

	/** Looks for a job; true when it found one. */
	boolean plan(ServerLevel level) {
		long now = level.getGameTime();
		BlockPos here = enderman.blockPosition();
		BlockState carried = enderman.getCarriedBlock();
		if (carried != null) {
			BlockPos spot = EndermanWork.destination(level, here, carried, enderman.getRandom());
			if (spot != null) {
				mind.set(EndermanMind.Job.DELIVER, spot, now);
				return true;
			}
			// set it down near its cairn for a later enderman to bring in
			CairnSite waitingFor = siteWanting(level, here, carried);
			if (waitingFor == null) {
				// a block no cairn wants any more (its cairn was finished without it): set it down by the nearest one
				waitingFor = CairnSites.nearest(level, here, EndermanTuning.SITE_WORK_RADIUS);
			}
			if (waitingFor != null) {
				BlockPos ground = setDownSpot(level, waitingFor, carried);
				if (ground != null) {
					mind.set(EndermanMind.Job.SET_DOWN, ground, now);
					return true;
				}
			}
		} else {
			EndermanWork.Found found = EndermanWork.findWork(level, here, enderman.getRandom(), EndermanWork.wanted(level, here));
			if (found != null) {
				mind.set(found.kind() == EndermanWork.Kind.TRAMPLE ? EndermanMind.Job.TRAMPLE : EndermanMind.Job.TAKE, found.pos(), now);
				return true;
			}
			// Now and then it goes farther afield for something to do (owner, 2026-11-16: "should endermen occasionally
			// pick up a new task? I think yes"): a look round a random spot ERRAND_MIN-ERRAND_MAX blocks off.
			if (enderman.getRandom().nextFloat() < EndermanTuning.ERRAND_CHANCE && errand(level, here, now)) {
				return true;
			}
		}
		// Nothing to do near it: at a complete cairn (its home first) it stands vigil in the ring around it.
		CairnSite site = homeSite(level);
		if (site == null || !site.complete(level)) {
			site = CairnSites.nearest(level, here, EndermanTuning.SITE_RADIUS);
		}
		if (site != null && site.complete(level)) {
			BlockPos place = vigilPlace(level, site);
			if (place != null) {
				mind.set(EndermanMind.Job.VIGIL, place, now);
				return true;
			}
		}
		return false;
	}

	/** Its home site, if it has one and it is loaded. */
	private @Nullable CairnSite homeSite(ServerLevel level) {
		BlockPos home = enderman instanceof EndermanAccess access ? access.brm$home() : null;
		return home == null || !level.isLoaded(home) ? null : CairnSites.covering(level, home);
	}

	/** Looks for a block to take or farmland to trample round a random spot a way off; true when it found one. */
	private boolean errand(ServerLevel level, BlockPos here, long now) {
		double angle = enderman.getRandom().nextDouble() * Math.PI * 2.0;
		double distance = Mth.lerp(enderman.getRandom().nextDouble(), EndermanTuning.ERRAND_MIN, EndermanTuning.ERRAND_MAX);
		int x = Mth.floor(here.getX() + Math.cos(angle) * distance);
		int z = Mth.floor(here.getZ() + Math.sin(angle) * distance);
		if (!level.hasChunk(x >> 4, z >> 4)) {
			return false;
		}
		BlockPos there = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		EndermanWork.Found found = EndermanWork.findWork(level, there, enderman.getRandom(), EndermanWork.wanted(level, there));
		if (found == null) {
			return false;
		}
		mind.set(found.kind() == EndermanWork.Kind.TRAMPLE ? EndermanMind.Job.TRAMPLE : EndermanMind.Job.TAKE, found.pos(), now);
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return mind.job() != null && enderman.level() instanceof ServerLevel level && allowed(level)
				&& level.getGameTime() - mind.started() < switch (mind.job()) {
					case VIGIL -> EndermanTuning.VIGIL_TICKS;
					default -> EndermanTuning.JOB_TIMEOUT;
				};
	}

	@Override
	public void start() {
		mind.setBusy(true);
		bestDistance = Double.MAX_VALUE;
		stuckTicks = 0;
		nextPath = 0;
	}

	@Override
	public void stop() {
		mind.setBusy(false);
		mind.clear();
		enderman.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (!(enderman.level() instanceof ServerLevel level) || mind.job() == null || mind.target() == null) {
			return;
		}
		BlockPos target = mind.target();
		switch (mind.job()) {
			case DELIVER, TAKE, SET_DOWN -> {
				Vec3 centre = Vec3.atCenterOf(target);
				if (enderman.getEyePosition().distanceTo(centre) <= EndermanTuning.REACH) {
					enderman.getNavigation().stop();
					enderman.getLookControl().setLookAt(centre);
					if (mind.job() == EndermanMind.Job.DELIVER) {
						deliver(level, target);
					} else if (mind.job() == EndermanMind.Job.SET_DOWN) {
						setDown(level, target);
					} else {
						take(level, target);
					}
				} else {
					approach(level, target, Vec3.atBottomCenterOf(target));
				}
			}
			case TRAMPLE -> {
				if (!level.getBlockState(target).is(Blocks.FARMLAND)) {
					mind.clear();
					return;
				}
				Vec3 on = Vec3.atBottomCenterOf(target.above());
				if (enderman.position().distanceToSqr(on) < 0.3) {
					enderman.getNavigation().stop();
					if (enderman.onGround()) {
						enderman.getJumpControl().jump();
					}
				} else {
					approach(level, target.above(), on);
				}
			}
			case VIGIL -> {
				Vec3 at = Vec3.atBottomCenterOf(target);
				CairnSite site = CairnSites.covering(level, vigilCentre);
				if (site == null || ((level.getGameTime() - mind.started()) % 20 == 0 && !site.complete(level))) {
					// cairn damaged or gone: back to work
					mind.clear();
					return;
				}
				if (enderman.position().distanceToSqr(at) > 1.0) {
					approach(level, target, at);
				} else {
					enderman.getNavigation().stop();
				}
				if (enderman.tickCount >= nextGaze || vigilGaze == Vec3.ZERO) {
					var random = enderman.getRandom();
					double half = site.box().getYSpan() / 2.0;
					vigilGaze = Vec3.atCenterOf(vigilCentre).add((random.nextDouble() - 0.5) * EndermanTuning.VIGIL_GAZE_SPREAD, (random.nextDouble() - 0.5) * half * 2.0,
							(random.nextDouble() - 0.5) * EndermanTuning.VIGIL_GAZE_SPREAD);
					nextGaze = enderman.tickCount + Mth.nextInt(random, EndermanTuning.VIGIL_GAZE_MIN_TICKS, EndermanTuning.VIGIL_GAZE_MAX_TICKS);
				}
				enderman.getLookControl().setLookAt(vigilGaze.x, vigilGaze.y, vigilGaze.z, 3.0F, 3.0F);
			}
		}
	}

	/** Walks toward {@code goal}; teleports next to the job when it is far away or stuck. */
	private void approach(ServerLevel level, BlockPos job, Vec3 goal) {
		double distance = enderman.position().distanceTo(goal);
		if (distance < bestDistance - 0.5) {
			bestDistance = distance;
			stuckTicks = 0;
		} else {
			stuckTicks++;
		}
		if (distance > EndermanTuning.TELEPORT_DISTANCE || stuckTicks > EndermanTuning.STUCK_TICKS) {
			teleportNear(level, job);
			bestDistance = Double.MAX_VALUE;
			stuckTicks = 0;
			nextPath = 0;
			return;
		}
		if (--nextPath <= 0) {
			nextPath = 10;
			enderman.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0);
		}
	}

	/** The nearest unfinished cairn within SITE_WORK_RADIUS that is missing {@code carried}'s block, or null. */
	private static @Nullable CairnSite siteWanting(ServerLevel level, BlockPos here, BlockState carried) {
		CairnSite best = null;
		for (CairnSite site : CairnSites.near(level, here, EndermanTuning.SITE_WORK_RADIUS)) {
			if (site.wants(level, carried.getBlock()) && (best == null || CairnSites.horizontalDistSqr(site.origin(), here) < CairnSites.horizontalDistSqr(best.origin(), here))) {
				best = site;
			}
		}
		return best;
	}

	/** Teleports to a free standing spot within reach of {@code job} (on the cairn itself for high blocks). */
	private void teleportNear(ServerLevel level, BlockPos job) {
		teleportNear(level, enderman, job);
	}

	private static void teleportNear(ServerLevel level, Enderman enderman, BlockPos job) {
		List<BlockPos> spots = new ArrayList<>();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		Vec3 centre = Vec3.atCenterOf(job);
		double eye = enderman.getEyeHeight();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 1; dy >= -6; dy--) {
					at.set(job.getX() + dx, job.getY() + dy, job.getZ() + dz);
					if (at.equals(job) || (dx == 0 && dz == 0 && dy <= 0 && dy >= -3)) {
						continue;
					}
					Vec3 feet = Vec3.atBottomCenterOf(at);
					if (feet.add(0.0, eye, 0.0).distanceTo(centre) > EndermanTuning.REACH) {
						continue;
					}
					if (standable(level, enderman, at) && !AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(job)).intersects(enderman.getType().getDimensions().makeBoundingBox(feet))) {
						spots.add(at.immutable());
					}
				}
			}
		}
		if (!spots.isEmpty()) {
			BlockPos spot = spots.get(enderman.getRandom().nextInt(spots.size()));
			if (enderman.teleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5)) {
				return;
			}
		}
		enderman.teleport(job.getX() + 0.5 + enderman.getRandom().nextInt(5) - 2, job.getY(), job.getZ() + 0.5 + enderman.getRandom().nextInt(5) - 2);
	}

	/** A sturdy floor, room for an enderman's 2.9-block height, and no water. */
	private boolean standable(ServerLevel level, BlockPos feet) {
		return standable(level, enderman, feet);
	}

	private static boolean standable(ServerLevel level, Enderman enderman, BlockPos feet) {
		BlockPos below = feet.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(feet).isEmpty()) {
			return false;
		}
		AABB box = enderman.getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(feet));
		return level.noCollision(enderman, box) && !level.containsAnyLiquid(box);
	}

	private void deliver(ServerLevel level, BlockPos pos) {
		BlockState carried = enderman.getCarriedBlock();
		if (carried == null) {
			mind.clear();
			return;
		}
		BlockState placed = EndermanWork.placement(level, pos, carried);
		List<net.minecraft.world.entity.Entity> inTheWay = placed == null ? List.of() : level.getEntities(enderman, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos)));
		if (placed != null && !inTheWay.isEmpty() && inTheWay.stream().allMatch(e -> e instanceof Enderman other && EndermanMind.of(other) != null)) {
			// caretakers standing where the block goes step aside
			inTheWay.forEach(e -> teleportNear(level, (Enderman) e, pos));
			inTheWay = level.getEntities(enderman, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos)));
		}
		if (placed == null || !inTheWay.isEmpty()) {
			// Someone got there first, or the spot changed: look again next time.
			mind.clear();
			nextSearch = enderman.tickCount + 5;
			return;
		}
		placed = Block.updateFromNeighbourShapes(placed, level, pos);
		level.setBlockAndUpdate(pos, placed);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(enderman, placed));
		if (EndermanNexus.isInside(level, pos)) {
			Ripples.revertEffect(level, pos);
		}
		enderman.setCarriedBlock(null);
		mind.clear();
	}

	private void take(ServerLevel level, BlockPos pos) {
		BlockState real = level.getBlockState(pos);
		BlockState target = TargetWorld.target(level, pos);
		if (enderman.getCarriedBlock() != null || real.isAir() || target == null || TerrainKinds.matches(real, target)
				|| !(real.is(Blocks.FARMLAND) || TerrainKinds.clearable(level, pos, real) || EndermanWork.readyCairnBlock(level, pos, real))
				|| BeaconBases.inWorkingBase(level, pos, real)) {
			mind.clear();
			return;
		}
		level.removeBlock(pos, false);
		level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(enderman, real));
		if (EndermanNexus.isInside(level, pos)) {
			Ripples.revertEffect(level, pos);
		}
		enderman.setCarriedBlock(real);
		mind.clear();
		// Find where it goes straight away.
		nextSearch = enderman.tickCount;
	}

	/** Sets its carried block down at {@code pos} as it is. */
	private void setDown(ServerLevel level, BlockPos pos) {
		BlockState carried = enderman.getCarriedBlock();
		if (carried == null || !groundFor(level, pos, carried)
				|| !level.getEntities(enderman, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))).isEmpty()) {
			mind.clear();
			nextSearch = enderman.tickCount + 5;
			return;
		}
		BlockState placed = Block.updateFromNeighbourShapes(carried, level, pos);
		level.setBlockAndUpdate(pos, placed);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(enderman, placed));
		enderman.setCarriedBlock(null);
		mind.clear();
		nextSearch = enderman.tickCount;
	}

	/** A spot on the ground SET_DOWN_GAP_MIN-MAX outside {@code site}'s footprint where {@code carried} can lie, or null. */
	private @Nullable BlockPos setDownSpot(ServerLevel level, CairnSite site, BlockState carried) {
		for (int i = 0; i < 12; i++) {
			double angle = enderman.getRandom().nextDouble() * Math.PI * 2.0;
			double radius = site.radius() + 0.5 + Mth.nextInt(enderman.getRandom(), EndermanTuning.SET_DOWN_GAP_MIN, EndermanTuning.SET_DOWN_GAP_MAX);
			int x = Mth.floor(site.origin().getX() + 0.5 + Math.cos(angle) * radius);
			int z = Mth.floor(site.origin().getZ() + 0.5 + Math.sin(angle) * radius);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			BlockPos pad = site.tradeSpot();
			if (Math.abs(pos.getY() - site.origin().getY()) <= 4 && (pad == null || pad.distSqr(pos) > 9.0) && groundFor(level, pos, carried)) {
				return pos;
			}
		}
		return null;
	}

	/** Open ground outside every cairn where {@code carried} can lie and later counts as out of place. */
	private static boolean groundFor(ServerLevel level, BlockPos pos, BlockState carried) {
		BlockPos below = pos.below();
		BlockState target = TargetWorld.target(level, pos);
		return level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
				&& carried.canSurvive(level, pos) && target != null && target.isAir() && EndermanWork.offCairn(level, pos);
	}

	/**
	 * Its place in the ring around a complete cairn: VIGIL_GAP outside the footprint, at an angle fixed by its id (so
	 * the ring spreads out and each keeps its place), on the ground there. Also sets what it stares at (the cairn's
	 * middle). Null when no standable spot is found near that angle.
	 */
	private @Nullable BlockPos vigilPlace(ServerLevel level, CairnSite site) {
		double radius = site.radius() + EndermanTuning.VIGIL_GAP + 0.5;
		double base = enderman.getId() * 2.399963; // golden angle: neighbours' ids land far apart on the ring
		for (int i = 0; i < 6; i++) {
			double angle = base + i * 0.35;
			int x = Mth.floor(site.origin().getX() + 0.5 + Math.cos(angle) * radius);
			int z = Mth.floor(site.origin().getZ() + 0.5 + Math.sin(angle) * radius);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (Math.abs(feet.getY() - site.origin().getY()) <= 4 && standable(level, feet)) {
				int top = site.box().maxY();
				vigilCentre = new BlockPos(site.origin().getX(), (site.origin().getY() + top) / 2, site.origin().getZ()); vigilGaze = Vec3.ZERO;
				return feet;
			}
		}
		return null;
	}

}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The caretaker's work loop (specs/endermen.md, "Two jobs"), replacing vanilla's random take-block and leave-block
 * goals: carrying a block, it takes it to a spot that needs it (a cairn's missing block, or a hole whose original
 * terrain is its family); empty-handed, it takes a block that doesn't belong, or jumps on farmland that should be dirt.
 * It walks, and teleports when far or stuck. If no spot needs what it carries it keeps it (and the block vanishes
 * when it despawns). Off while angry, and off when mob griefing is off (vanilla's rule for enderman blocks).
 */
public final class EndermanJobGoal extends Goal {
	private final Enderman enderman;
	private final EndermanMind mind;
	private int nextSearch;
	private int nextPath;
	private double bestDistance;
	private int stuckTicks;

	public EndermanJobGoal(Enderman enderman, EndermanMind mind) {
		this.enderman = enderman;
		this.mind = mind;
		this.nextSearch = (enderman.getId() & 15);
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
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
		} else {
			EndermanWork.Found found = EndermanWork.findWork(level, here, enderman.getRandom(), EndermanWork.wanted(level, here));
			if (found != null) {
				mind.set(found.kind() == EndermanWork.Kind.TRAMPLE ? EndermanMind.Job.TRAMPLE : EndermanMind.Job.TAKE, found.pos(), now);
				return true;
			}
		}
		if (enderman.getRandom().nextFloat() < EndermanTuning.PERCH_CHANCE) {
			CairnSite site = CairnSites.nearest(level, here, EndermanTuning.SITE_RADIUS);
			if (site != null && site.complete(level)) {
				BlockPos perch = perchOn(level, site);
				if (perch != null) {
					mind.set(EndermanMind.Job.PERCH, perch, now);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return mind.job() != null && enderman.level() instanceof ServerLevel level && allowed(level)
				&& level.getGameTime() - mind.started() < (mind.job() == EndermanMind.Job.PERCH ? EndermanTuning.PERCH_TICKS : EndermanTuning.JOB_TIMEOUT);
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
			case DELIVER, TAKE -> {
				Vec3 centre = Vec3.atCenterOf(target);
				if (enderman.getEyePosition().distanceTo(centre) <= EndermanTuning.REACH) {
					enderman.getNavigation().stop();
					enderman.getLookControl().setLookAt(centre);
					if (mind.job() == EndermanMind.Job.DELIVER) {
						deliver(level, target);
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
			case PERCH -> {
				Vec3 on = Vec3.atBottomCenterOf(target);
				if (enderman.position().distanceToSqr(on) > 1.0) {
					enderman.teleport(on.x, on.y, on.z);
					if (enderman.position().distanceToSqr(on) > 1.0) {
						mind.clear();
					}
				}
				enderman.getNavigation().stop();
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

	/** Teleports to a free standing spot within reach of {@code job} (on the cairn itself for high blocks). */
	private void teleportNear(ServerLevel level, BlockPos job) {
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
					if (standable(level, at)) {
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
		if (placed == null || !level.getEntities(enderman, AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))).isEmpty()) {
			// Someone got there first, or the spot changed: look again next time.
			mind.clear();
			nextSearch = enderman.tickCount + 5;
			return;
		}
		placed = Block.updateFromNeighbourShapes(placed, level, pos);
		level.setBlockAndUpdate(pos, placed);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(enderman, placed));
		enderman.setCarriedBlock(null);
		mind.clear();
	}

	private void take(ServerLevel level, BlockPos pos) {
		BlockState real = level.getBlockState(pos);
		BlockState target = TargetWorld.target(level, pos);
		if (enderman.getCarriedBlock() != null || real.isAir() || target == null || TerrainKinds.matches(real, target)
				|| !(real.is(Blocks.FARMLAND) || TerrainKinds.liftable(level, pos, real))) {
			mind.clear();
			return;
		}
		level.removeBlock(pos, false);
		level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(enderman, real));
		enderman.setCarriedBlock(real);
		mind.clear();
		// Find where it goes straight away.
		nextSearch = enderman.tickCount;
	}

	/** A free spot on top of the cairn (its highest standable blocks), or null. */
	private @Nullable BlockPos perchOn(ServerLevel level, CairnSite site) {
		List<BlockPos> order = site.order();
		for (int i = order.size() - 1; i >= 0; i--) {
			BlockPos above = order.get(i).above();
			if (site.at(above) == null && standable(level, above)) {
				return above;
			}
		}
		return null;
	}

	/** For tests: plan right now, ignoring the search interval. */
	public boolean planNow() {
		return enderman.level() instanceof ServerLevel level && allowed(level) && plan(level);
	}
}

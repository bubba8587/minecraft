// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.sense.Gaze;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Pillar creeper (specs/creepers.md, "Making its own cover"): with no cover anywhere and mobGriefing on, a watcher
 * stacks a 2-high pillar of surface blocks (moved as they are) and peeks from behind it. One job per creeper, run by
 * {@link CreeperWatchGoal}.
 */
public final class CreeperPillar {
	/** Natural terrain a creeper may stack (data: tags/block/creeper_pillar). */
	public static final TagKey<Block> PILLAR_BLOCKS = TagKey.create(Registries.BLOCK, BubbasReworkedMobs.id("creeper_pillar"));

	/** The block a creeper carries (shown over its head). */
	public static final AttachmentType<BlockState> CARRIED = AttachmentRegistry.create(BubbasReworkedMobs.id("creeper_carried"),
			builder -> builder.persistent(BlockState.CODEC).syncWith(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), AttachmentSyncPredicate.all()));

	/** Job stages. */
	private enum Stage {
		TO_SOURCE, TAKING, TO_SITE, PLACING
	}

	/** A planned pillar: base cell, the cells to fill (bottom first), where it stands to place, and where blocks come from. */
	record Plan(BlockPos base, List<BlockPos> fill, BlockPos stand, List<BlockPos> sources) {
	}

	private final Creeper creeper;
	private @Nullable Plan plan;
	private Stage stage = Stage.TO_SOURCE;
	private int placed;
	private int sourceIndex;
	private int timer;
	private int budget;
	private int walks;
	private @Nullable BlockPos takenFrom;

	CreeperPillar(Creeper creeper) {
		this.creeper = creeper;
	}

	boolean active() {
		return plan != null;
	}

	/** Whether this creeper may move blocks: vanilla's mobGriefing rule, as endermen. */
	static boolean griefing(Level level) {
		return level instanceof ServerLevel server && server.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	/** Plans and starts a pillar against {@code target} if allowed and a site exists; false otherwise. */
	boolean tryStart(CreeperMind mind, LivingEntity target, double distance) {
		long now = creeper.level().getGameTime();
		if (!griefing(creeper.level()) || mind.leaving() || now < mind.nextPillarTime || creeper.hasAttached(CARRIED)) {
			return false;
		}
		Plan found = planFor(creeper.level(), creeper, target, distance);
		if (found == null) {
			mind.nextPillarTime = now + CreeperTuning.PILLAR_RETRY_TICKS;
			return false;
		}
		mind.nextPillarTime = now + CreeperTuning.PILLAR_COOLDOWN_TICKS;
		plan = found;
		stage = Stage.TO_SOURCE;
		placed = 0;
		sourceIndex = 0;
		timer = 0;
		budget = CreeperTuning.PILLAR_MAX_TICKS;
		walks = 0;
		walkToSource();
		return true;
	}

	/** One tick of the job; returns true once the pillar stands or the job is given up. */
	boolean tick(CreeperMind mind, LivingEntity target, boolean looked) {
		Plan p = plan;
		if (p == null) {
			return true;
		}
		Level level = creeper.level();
		if (!griefing(level) || --budget < 0) {
			abort();
			return true;
		}
		// Caught in the open: freezes and stares until looked away.
		if (looked && CoverFinder.clear(level, target.getEyePosition(), creeper.getEyePosition())) {
			creeper.getNavigation().stop();
			budget++;
			return false;
		}
		switch (stage) {
			case TO_SOURCE -> {
				BlockPos source = p.sources().get(sourceIndex);
				boolean done = creeper.getNavigation().isDone();
				if (horizontal(source) < CreeperTuning.PILLAR_REACH || done && horizontal(source) < CreeperTuning.PILLAR_FAR_REACH) {
					creeper.getNavigation().stop();
					stage = Stage.TAKING;
					timer = CreeperTuning.PILLAR_TAKE_TICKS;
				} else if (done && !walkToSource()) {
					return nextSource();
				}
			}
			case TAKING -> {
				BlockPos source = p.sources().get(sourceIndex);
				creeper.getLookControl().setLookAt(Vec3.atCenterOf(source));
				if (--timer > 0) {
					return false;
				}
				BlockState state = level.getBlockState(source);
				if (!takeable(level, source, state)) {
					return nextSource();
				}
				level.removeBlock(source, false);
				level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, source, Block.getId(state));
				level.gameEvent(GameEvent.BLOCK_DESTROY, source, GameEvent.Context.of(creeper, state));
				creeper.setAttached(CARRIED, state);
				takenFrom = source;
				stage = Stage.TO_SITE;
				walks = 0;
				walkToSite();
			}
			case TO_SITE -> {
				Vec3 stand = Vec3.atBottomCenterOf(p.stand());
				double dx = creeper.getX() - stand.x;
				double dz = creeper.getZ() - stand.z;
				BlockPos at = p.fill().get(placed);
				boolean inTheWay = creeper.getBoundingBox().intersects(new AABB(at));
				if (!inTheWay && (dx * dx + dz * dz < 0.6 * 0.6 || creeper.getNavigation().isDone() && horizontal(at) < CreeperTuning.PILLAR_FAR_REACH)) {
					creeper.getNavigation().stop();
					stage = Stage.PLACING;
					timer = CreeperTuning.PILLAR_PLACE_TICKS;
				} else if (inTheWay && creeper.getNavigation().isDone()) {
					creeper.getMoveControl().setWantedPosition(stand.x, stand.y, stand.z, CreeperTuning.WATCH_SPEED);
				} else if (creeper.getNavigation().isDone() && !walkToSite()) {
					abort();
					return true;
				}
			}
			case PLACING -> {
				BlockPos at = p.fill().get(placed);
				creeper.getLookControl().setLookAt(Vec3.atCenterOf(at));
				BlockState carried = creeper.getAttached(CARRIED);
				if (carried == null) {
					abort();
					return true;
				}
				if (--timer > 0) {
					return false;
				}
				if (!level.getBlockState(at).isAir()) {
					abort();
					return true;
				}
				if (!level.isUnobstructed(carried, at, CollisionContext.empty())) {
					// Something stands in the way: step back, or wait for it.
					stage = Stage.TO_SITE;
					return false;
				}
				place(level, at, carried);
				creeper.removeAttached(CARRIED);
				takenFrom = null;
				placed++;
				if (placed >= p.fill().size()) {
					mind.pillar = p.base();
					plan = null;
					return true;
				}
				sourceIndex++;
				if (sourceIndex >= p.sources().size()) {
					plan = null;
					return true;
				}
				stage = Stage.TO_SOURCE;
				walks = 0;
				walkToSource();
			}
		}
		return false;
	}

	/** Gives up: whatever it carries goes back where it came from (or close by). */
	void abort() {
		plan = null;
		putDown(creeper, takenFrom);
		takenFrom = null;
	}

	private boolean nextSource() {
		Plan p = plan;
		if (p == null) {
			return true;
		}
		sourceIndex++;
		if (sourceIndex >= p.sources().size()) {
			abort();
			return true;
		}
		stage = Stage.TO_SOURCE;
		walks = 0;
		if (!walkToSource()) {
			return nextSource();
		}
		return false;
	}

	private boolean walkToSource() {
		Plan p = plan;
		if (p == null || ++walks > CreeperTuning.PILLAR_MAX_WALKS) {
			return false;
		}
		BlockPos source = p.sources().get(sourceIndex);
		return horizontal(source) < CreeperTuning.PILLAR_REACH
				|| creeper.getNavigation().moveTo(source.getX() + 0.5, source.getY() + 1, source.getZ() + 0.5, 1, CreeperTuning.WATCH_SPEED);
	}

	private boolean walkToSite() {
		Plan p = plan;
		if (p == null || ++walks > CreeperTuning.PILLAR_MAX_WALKS) {
			return false;
		}
		BlockPos stand = p.stand();
		return creeper.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, CreeperTuning.WATCH_SPEED);
	}

	private double horizontal(BlockPos pos) {
		double dx = creeper.getX() - (pos.getX() + 0.5);
		double dz = creeper.getZ() - (pos.getZ() + 0.5);
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static void place(Level level, BlockPos at, BlockState state) {
		level.setBlock(at, state, Block.UPDATE_ALL);
		SoundType sound = state.getSoundType();
		level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
		level.gameEvent(GameEvent.BLOCK_PLACE, at, GameEvent.Context.of(state));
	}

	/** Sets down a carried block: back in its dent if that is still open, else next to the creeper, else as an item. */
	public static void putDown(Creeper creeper, @Nullable BlockPos dent) {
		BlockState carried = creeper.getAttached(CARRIED);
		if (carried == null) {
			return;
		}
		creeper.removeAttached(CARRIED);
		Level level = creeper.level();
		List<BlockPos> spots = new ArrayList<>();
		if (dent != null) {
			spots.add(dent);
		}
		BlockPos feet = creeper.blockPosition();
		for (Direction d : Direction.Plane.HORIZONTAL) {
			spots.add(feet.relative(d));
		}
		for (BlockPos spot : spots) {
			BlockPos below = spot.below();
			if (level.getBlockState(spot).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
					&& level.isUnobstructed(carried, spot, CollisionContext.empty())) {
				place(level, spot, carried);
				return;
			}
		}
		Block.popResource(level, feet, new ItemStack(carried.getBlock()));
	}

	/** A surface block it may take: natural terrain (tag), a full cube, no block entity, breakable, open above, solid below. */
	static boolean takeable(Level level, BlockPos pos, BlockState state) {
		if (!state.is(PILLAR_BLOCKS) || state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, pos) < 0.0F
				|| state.is(BlockTags.WITHER_IMMUNE) || !state.isCollisionShapeFullBlock(level, pos)) {
			return false;
		}
		BlockPos above = pos.above();
		BlockPos below = pos.below();
		return level.getBlockState(above).getCollisionShape(level, above).isEmpty() && level.getFluidState(above).isEmpty()
				&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	/**
	 * A pillar site for watching {@code target}: within PILLAR_SITE_RADIUS of the creeper, at a watching distance, with
	 * room to stand behind it and enough surface blocks around to dig. A lone natural block sticking up is used as the
	 * bottom block. Prefers sites the target isn't looking at. Block lookups only.
	 */
	static @Nullable Plan planFor(Level level, Creeper creeper, LivingEntity target, double distance) {
		Vec3 eye = target.getEyePosition();
		double preferred = Math.clamp(distance, CreeperTuning.PILLAR_MIN_DISTANCE + 2.0, CreeperTuning.PILLAR_PREFERRED_DISTANCE);
		BlockPos around = creeper.blockPosition();
		int r = CreeperTuning.PILLAR_SITE_RADIUS;
		Plan best = null;
		double bestScore = Double.MAX_VALUE;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int x = around.getX() + dx;
				int z = around.getZ() + dz;
				double vx = x + 0.5 - eye.x;
				double vz = z + 0.5 - eye.z;
				double toViewer = Math.sqrt(vx * vx + vz * vz);
				if (toViewer < CreeperTuning.PILLAR_MIN_DISTANCE || toViewer > CreeperTuning.PILLAR_MAX_DISTANCE || !level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				double score = Math.abs(toViewer - preferred) + 0.3 * Math.sqrt(dx * dx + dz * dz);
				if (score - CreeperTuning.PILLAR_BUMP_BONUS >= bestScore) {
					continue;
				}
				Plan plan = site(level, x, around.getY(), z, eye, vx / toViewer, vz / toViewer);
				if (plan == null) {
					continue;
				}
				if (plan.fill().size() == 1) {
					score -= CreeperTuning.PILLAR_BUMP_BONUS;
				}
				if (target instanceof Player && Gaze.looksAt(level, target, CreeperTuning.PLAYER_LOOK_HALF_ANGLE, false, Vec3.atCenterOf(plan.base()))) {
					score += CreeperTuning.PILLAR_SEEN_PENALTY;
				}
				if (score < bestScore) {
					best = plan;
					bestScore = score;
				}
			}
		}
		return best;
	}

	/** The pillar at column x, z (away: unit vector from the viewer), or null when it doesn't suit. */
	private static @Nullable Plan site(Level level, int x, int y, int z, Vec3 eye, double ax, double az) {
		BlockPos top = CoverFinder.standableIn(level, x, y, z, 2, 2);
		if (top == null) {
			return null;
		}
		BlockPos ground = top.below();
		BlockState groundState = level.getBlockState(ground);
		BlockPos base;
		List<BlockPos> fill;
		if (bump(level, ground, groundState)) {
			base = ground;
			fill = List.of(top);
		} else if (groundState.isFaceSturdy(level, ground, Direction.UP)) {
			base = top;
			fill = List.of(top, top.above());
		} else {
			return null;
		}
		int floor = base.getY();
		// Where it stands behind the pillar, and the cells either side (its peek spots).
		Vec3 behind = Vec3.atBottomCenterOf(base).add(ax * 1.4, 0.0, az * 1.4);
		BlockPos stand = BlockPos.containing(behind.x, floor, behind.z);
		BlockPos left = BlockPos.containing(behind.x - az, floor, behind.z + ax);
		BlockPos right = BlockPos.containing(behind.x + az, floor, behind.z - ax);
		List<BlockPos> keep = List.of(base, stand, left, right);
		for (BlockPos cell : List.of(stand, left, right)) {
			if (cell.equals(base) || !CoverFinder.standable(level, cell)) {
				return null;
			}
		}
		// The view to where it will hide is open now (else there is cover already).
		if (!CoverFinder.clear(level, eye, behind.add(0.0, 1.4, 0.0))) {
			return null;
		}
		List<BlockPos> sources = sources(level, base, floor, keep, fill.size() + CreeperTuning.PILLAR_SPARE_SOURCES);
		if (sources.size() < fill.size()) {
			return null;
		}
		return new Plan(base, fill, stand, sources);
	}

	/** A lone natural block sticking up out of level ground (three or four sides open). */
	private static boolean bump(Level level, BlockPos pos, BlockState state) {
		if (!state.is(PILLAR_BLOCKS) || !state.isCollisionShapeFullBlock(level, pos)) {
			return false;
		}
		BlockPos below = pos.below();
		if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
			return false;
		}
		int open = 0;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos side = pos.relative(d);
			if (level.getBlockState(side).getCollisionShape(level, side).isEmpty()) {
				open++;
			}
		}
		return open >= 3;
	}

	/**
	 * Surface blocks to dig near the pillar, nearest first, each the top of its own column on level ground (no side
	 * higher than it), never two side by side, so every dent is 1 deep.
	 */
	private static List<BlockPos> sources(Level level, BlockPos base, int floor, List<BlockPos> keep, int limit) {
		List<BlockPos> found = new ArrayList<>();
		int r = CreeperTuning.PILLAR_DIG_RADIUS;
		List<int[]> offsets = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx != 0 || dz != 0) {
					offsets.add(new int[] {dx, dz});
				}
			}
		}
		offsets.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
		for (int[] o : offsets) {
			if (found.size() >= limit) {
				break;
			}
			int x = base.getX() + o[0];
			int z = base.getZ() + o[1];
			if (kept(keep, x, z) || nextTo(found, x, z)) {
				continue;
			}
			BlockPos top = CoverFinder.standableIn(level, x, floor, z, 1, 2);
			if (top == null) {
				continue;
			}
			BlockPos source = top.below();
			if (!takeable(level, source, level.getBlockState(source))) {
				continue;
			}
			boolean level0 = true;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos side = source.above().relative(d);
				if (!level.getBlockState(side).getCollisionShape(level, side).isEmpty()) {
					level0 = false;
					break;
				}
			}
			if (level0) {
				found.add(source);
			}
		}
		return found;
	}

	private static boolean kept(List<BlockPos> keep, int x, int z) {
		for (BlockPos pos : keep) {
			if (pos.getX() == x && pos.getZ() == z) {
				return true;
			}
		}
		return false;
	}

	private static boolean nextTo(List<BlockPos> found, int x, int z) {
		for (BlockPos pos : found) {
			if (Math.abs(pos.getX() - x) <= 1 && Math.abs(pos.getZ() - z) <= 1) {
				return true;
			}
		}
		return false;
	}
}

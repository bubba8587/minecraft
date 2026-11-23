// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.bogged;

import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.sense.Gaze;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bogged are not sleepers (owner, 2026-11-22, specs/undead.md "Undead variants"): they lurk like the reworked drowned
 * (specs/drowned.md), crouched low in swamp water and mud among the reeds, moving when nobody is looking, and ambush.
 *
 * <ul>
 * <li><b>Find a hiding spot</b> near a player within {@link BoggedTuning#NOTICE_RANGE}: 1-2 deep water over a floor
 * (2 deep hides it best), or mud beside reeds (sugar cane, tall grass, ferns), out of the open.</li>
 * <li><b>Lurk:</b> crouched (vanilla's synced crouch pose), still, holding its bow down: it ignores targets while it
 * hides (its bow needs a second of drawing, so clearing its target every tick keeps it from shooting).</li>
 * <li><b>Moves unseen:</b> with nobody looking within 70 degrees of it, now and then (about every 30 s, often when its
 * player has moved off) it slips to a new spot. You notice it has moved when you look back.</li>
 * <li><b>Ambush:</b> a player within {@link BoggedTuning#AMBUSH_RANGE} with their back turned (the bogged more than 100
 * degrees from where they look), or anyone who comes within {@link BoggedTuning#TOO_CLOSE}: it stands and its normal AI
 * takes over (poison arrows). After the fight it goes back to hiding.</li>
 * </ul>
 * One mind per bogged, server only, not saved (it finds a spot again after a reload). Spawner bogged (trial chambers)
 * stay vanilla, as the owner decided for drowned and spawners.
 */
public final class BoggedLurk {
	enum State { IDLE, TRAVEL, LURK, HUNT }

	private static final class Mind {
		State state = State.IDLE;
		@Nullable BlockPos spot;
		long since;
		long lastTarget;
	}

	private static final Map<Bogged, Mind> MINDS = new WeakHashMap<>();

	private BoggedLurk() {
	}

	/** Whether this bogged is lurking right now (tests). */
	public static boolean isLurking(Bogged bogged) {
		Mind mind = MINDS.get(bogged);
		return mind != null && mind.state == State.LURK;
	}

	/** Puts it straight into hiding at {@code spot} (tests, previews). */
	public static void lurkAt(Bogged bogged, BlockPos spot) {
		Mind mind = MINDS.computeIfAbsent(bogged, b -> new Mind());
		mind.state = State.LURK;
		mind.spot = spot.immutable();
		mind.since = bogged.level().getGameTime();
		bogged.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, bogged.getYRot(), 0.0F);
	}

	public static void tick(ServerLevel level, Bogged bogged) {
		if (bogged.isNoAi() || bogged.isPassenger() || bogged.entityTags().contains(BoggedTuning.VANILLA_TAG)) {
			return;
		}
		Mind mind = MINDS.computeIfAbsent(bogged, b -> new Mind());
		long now = level.getGameTime();
		RandomSource random = bogged.getRandom();
		switch (mind.state) {
			case IDLE -> {
				posture(bogged, Pose.STANDING);
				if ((now + bogged.getId()) % 20 != 0 || bogged.getTarget() != null) {
					return;
				}
				Player player = level.getNearestPlayer(bogged, BoggedTuning.NOTICE_RANGE);
				if (player == null || player.isSpectator()) {
					return;
				}
				BlockPos spot = findSpot(level, bogged, player, null, random);
				if (spot != null) {
					mind.spot = spot;
					mind.state = State.TRAVEL;
					mind.since = now;
				}
			}
			case TRAVEL -> {
				posture(bogged, Pose.STANDING);
				bogged.setTarget(null);
				BlockPos spot = mind.spot;
				if (spot == null || now - mind.since > BoggedTuning.TRAVEL_TIMEOUT) {
					mind.state = State.IDLE;
					return;
				}
				Vec3 centre = Vec3.atBottomCenterOf(spot);
				if (bogged.position().distanceToSqr(centre) < 0.8) {
					bogged.getNavigation().stop();
					mind.state = State.LURK;
					mind.since = now;
				} else if (bogged.getNavigation().isDone() || now % 20 == 0) {
					bogged.getNavigation().moveTo(centre.x, centre.y, centre.z, 1.0);
				}
			}
			case LURK -> lurk(level, bogged, mind, now, random);
			case HUNT -> {
				posture(bogged, Pose.STANDING);
				if (bogged.getTarget() != null) {
					mind.lastTarget = now;
				} else if (now - mind.lastTarget > BoggedTuning.HUNT_LINGER) {
					mind.state = State.IDLE; // back to hiding
				}
			}
		}
	}

	private static void lurk(ServerLevel level, Bogged bogged, Mind mind, long now, RandomSource random) {
		BlockPos spot = mind.spot;
		if (spot == null) {
			mind.state = State.IDLE;
			return;
		}
		bogged.getNavigation().stop();
		bogged.setTarget(null);
		posture(bogged, Pose.CROUCHING);
		// Held at its spot (a gentle spring, so pushes and water don't carry it off).
		Vec3 centre = Vec3.atBottomCenterOf(spot);
		Vec3 pull = centre.subtract(bogged.position()).multiply(0.15, 0.0, 0.15);
		bogged.setDeltaMovement(bogged.getDeltaMovement().multiply(0.5, 1.0, 0.5).add(pull));
		Player nearest = level.getNearestPlayer(bogged, BoggedTuning.NOTICE_RANGE);
		if (nearest == null) {
			return;
		}
		if (bogged.hurtTime > 0 && bogged.getLastHurtByMob() != null) {
			ambush(bogged, mind, bogged.getLastHurtByMob(), now); // found out: fight
			return;
		}
		Vec3[] points = Gaze.bodyPoints(bogged.position(), bogged.getBbWidth(), bogged.getBbHeight(), bogged.getEyeHeight(), nearest.getEyePosition());
		boolean seen = false;
		for (Player player : level.players()) {
			if (player.isSpectator() || player.distanceToSqr(bogged) > BoggedTuning.NOTICE_RANGE * BoggedTuning.NOTICE_RANGE) {
				continue;
			}
			double d = Math.sqrt(player.distanceToSqr(bogged));
			boolean faces = Gaze.looksAt(level, player, BoggedTuning.BACK_TURNED_HALF_ANGLE, false, points);
			if (!player.isCreative() && (d < BoggedTuning.TOO_CLOSE || (d < BoggedTuning.AMBUSH_RANGE && !faces && bogged.hasLineOfSight(player)))) {
				ambush(bogged, mind, player, now);
				return;
			}
			seen |= Gaze.looksAt(level, player, BoggedTuning.UNSEEN_HALF_ANGLE, true, points);
		}
		// Unseen, it slips away to another spot now and then (often once its player has moved off).
		double fromSpot = Math.sqrt(nearest.distanceToSqr(Vec3.atBottomCenterOf(spot)));
		int oneIn = fromSpot > BoggedTuning.FOLLOW_DISTANCE ? BoggedTuning.MOVE_ONE_IN_FOLLOWING : BoggedTuning.MOVE_ONE_IN;
		if (!seen && random.nextInt(oneIn) == 0) {
			BlockPos next = findSpot(level, bogged, nearest, spot, random);
			if (next != null) {
				mind.spot = next;
				mind.state = State.TRAVEL;
				mind.since = now;
			}
		}
	}

	private static void ambush(Bogged bogged, Mind mind, net.minecraft.world.entity.LivingEntity target, long now) {
		posture(bogged, Pose.STANDING);
		mind.state = State.HUNT;
		mind.lastTarget = now;
		bogged.setTarget(target);
		bogged.playAmbientSound();
	}

	private static void posture(Bogged bogged, Pose pose) {
		if (bogged.getPose() != pose) {
			bogged.setPose(pose);
		}
	}

	/**
	 * A hiding spot near {@code player}: random columns {@link BoggedTuning#SPOT_MIN}-{@link BoggedTuning#SPOT_MAX}
	 * blocks from the player and within {@link BoggedTuning#SPOT_TRAVEL} of the bogged. Water 1-2 deep over a solid
	 * floor (2 deep best), or mud with reeds beside it. Returns the block its feet go in, or null.
	 */
	public static @Nullable BlockPos findSpot(Level level, Bogged bogged, Player player, @Nullable BlockPos avoid, RandomSource random) {
		BlockPos best = null;
		int bestScore = Integer.MAX_VALUE;
		for (int i = 0; i < BoggedTuning.SPOT_COLUMNS; i++) {
			double a = random.nextDouble() * Math.PI * 2.0;
			double r = BoggedTuning.SPOT_MIN + random.nextDouble() * (BoggedTuning.SPOT_MAX - BoggedTuning.SPOT_MIN);
			int x = (int) Math.floor(player.getX() + Math.cos(a) * r);
			int z = (int) Math.floor(player.getZ() + Math.sin(a) * r);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos feet = hideAt(level, x, z, player.getBlockY());
			if (feet == null || (avoid != null && feet.distManhattan(avoid) < 4)
					|| feet.distToCenterSqr(bogged.position()) > BoggedTuning.SPOT_TRAVEL * BoggedTuning.SPOT_TRAVEL) {
				continue;
			}
			int score = (level.getBlockState(feet.above()).getFluidState().isEmpty() ? 2 : 0) + (int) Math.abs(r - BoggedTuning.SPOT_BEST);
			if (score < bestScore) {
				bestScore = score;
				best = feet;
			}
		}
		return best;
	}

	/**
	 * The feet block of a hiding spot in this column near height {@code aroundY}, or null: 1-2 water over a floor, or mud
	 * among reeds, open to the air above. Scans down from a few blocks over {@code aroundY} rather than trusting the
	 * heightmap, which mangrove leaves and roots would stop at; anything solid over the spot (a canopy) is passed by.
	 */
	public static @Nullable BlockPos hideAt(Level level, int x, int z, int aroundY) {
		int top = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, aroundY + BoggedTuning.SPOT_HEIGHT);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, top, z);
		for (int y = top; y >= aroundY - BoggedTuning.SPOT_HEIGHT; y--) {
			at.setY(y);
			BlockState state = level.getBlockState(at);
			BlockState above = level.getBlockState(at.above());
			if (!above.getFluidState().isEmpty() || !above.getCollisionShape(level, at.above()).isEmpty()) {
				continue; // not a surface open to the air
			}
			BlockPos surface = at.immutable();
			if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
				// Water: the floor 1 or 2 below the surface.
				for (int depth = 1; depth <= 2; depth++) {
					BlockPos floor = surface.below(depth);
					BlockState f = level.getBlockState(floor);
					if (!f.getFluidState().isEmpty()) {
						continue;
					}
					return f.isFaceSturdy(level, floor, net.minecraft.core.Direction.UP) || f.is(Blocks.MUD) ? floor.above() : null;
				}
				return null;
			}
			if (state.is(Blocks.MUD) && level.getBlockState(surface.above(2)).getCollisionShape(level, surface.above(2)).isEmpty()) {
				for (BlockPos n : new BlockPos[] { surface.above().north(), surface.above().south(), surface.above().east(), surface.above().west() }) {
					BlockState plant = level.getBlockState(n);
					if (plant.is(Blocks.SUGAR_CANE) || plant.is(Blocks.TALL_GRASS) || plant.is(Blocks.LARGE_FERN)
							|| plant.is(BlockTags.REPLACEABLE) && !plant.isAir() && plant.getFluidState().isEmpty()) {
						return surface.above();
					}
				}
				return null;
			}
		}
		return null;
	}
}

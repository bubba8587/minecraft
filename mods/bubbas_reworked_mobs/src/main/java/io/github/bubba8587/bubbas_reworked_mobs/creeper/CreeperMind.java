// dte:C1,B18,B19
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One creeper's senses and nerves (specs/creepers.md): the startle meter, what it watches, and the hiss state. Server
 * side only, not saved (the meter drains within seconds; a reloaded creeper starts calm and notices you again).
 * Ticked from {@code CreeperMixin}; its goals ({@link CreeperFuseGoal}, {@link CreeperWatchGoal}) read it.
 *
 * <p>Cost per creeper: one entity box query of WATCH_RANGE every MOTION_SCAN_INTERVAL ticks (phase spread by entity
 * id) and a small projectile query every PROJECTILE_SCAN_INTERVAL; line-of-sight raycasts only for things that are
 * moving and in range (vanilla's per-tick cached Sensing).
 */
public final class CreeperMind {
	private float meter;
	private boolean fusing;
	private boolean hitDuringFuse;
	private @Nullable LivingEntity watched;
	private long watchedSince;
	private @Nullable LivingEntity startler;
	/** Positions at the last motion scan, by entity id (speeds are measured across one scan). */
	private Int2ObjectMap<Vec3> lastPositions = new Int2ObjectOpenHashMap<>();
	/** Projectiles already counted as near-misses, by id, with the game time they were counted. */
	private final Int2LongMap countedProjectiles = new Int2LongOpenHashMap();
	/** The watch spot, kept here so a spawner can hand a fresh creeper its spot. */
	CoverFinder.@Nullable PeekSpot spot;
	boolean spotReached;
	private boolean openGround;
	private long lastNoticedApproach = -1L;
	/** Game time its watched player last looked toward it (never: Long.MIN_VALUE). */
	private long lastLookedAt = Long.MIN_VALUE;
	/** Leaving by day (watchers only): ticks until it starts leaving (-1: not counting), and whether it is leaving. */
	private int leaveCountdown = -1;
	private boolean leaving;

	/** Whether this creeper behaves as a watcher at all: charged creepers stay vanilla, blinded ones can't see motion. */
	public static boolean active(Creeper creeper) {
		return !creeper.isPowered() && !creeper.hasEffect(MobEffects.BLINDNESS);
	}

	public float meter() {
		return meter;
	}

	public boolean isFusing() {
		return fusing;
	}

	public boolean wasHitDuringFuse() {
		return hitDuringFuse;
	}

	public @Nullable LivingEntity watched() {
		return watched;
	}

	/** Who set it off (or startled it last): the one whose sneaking or leaving defuses the hiss. */
	public @Nullable LivingEntity startler() {
		return startler != null ? startler : watched;
	}

	public void watch(LivingEntity entity, long gameTime) {
		if (watched != entity) {
			watched = entity;
			watchedSince = gameTime;
			spot = null;
			spotReached = false;
		}
	}

	/** Its current watch spot, if any. */
	public CoverFinder.@Nullable PeekSpot spot() {
		return spot;
	}

	/** Whether it has walked to its spot (and now only slides between half-hidden and hidden). */
	public boolean spotReached() {
		return spot != null && spotReached;
	}

	public void setSpot(CoverFinder.PeekSpot spot, boolean reached) {
		this.spot = spot;
		this.spotReached = reached;
	}

	/** Whether it found no cover at all and holds the open-ground stance (C5). */
	public boolean openGround() {
		return openGround;
	}

	void setOpenGround(boolean openGround) {
		this.openGround = openGround;
	}

	/** Game time it last noticed a player coming at it (ran off or backed behind cover); -1 if never. */
	public long lastNoticedApproach() {
		return lastNoticedApproach;
	}

	void noticedApproach(long gameTime) {
		lastNoticedApproach = gameTime;
	}

	/** Its watched player looked toward it now. */
	void lookedAt(long gameTime) {
		lastLookedAt = gameTime;
	}

	/** Whether it is leaving (a mod-spawned watcher by day): it heads out of sight and is gone once no one can see it. */
	public boolean leaving() {
		return leaving;
	}

	/** Starts the leaving countdown now with {@code ticks} to go (by day it otherwise picks a random moment itself). */
	public void leaveIn(int ticks) {
		leaveCountdown = Math.max(0, ticks);
	}

	/** Adds startle points from {@code source} (null for unknown); a full meter lights the hiss. */
	public void startle(float points, @Nullable Entity source) {
		if (source instanceof LivingEntity living) {
			startler = living;
		}
		meter = Math.min(CreeperTuning.METER_FULL, meter + points);
		if (meter >= CreeperTuning.METER_FULL) {
			fusing = true;
		}
	}

	/** Something with an attacker behind it hurt the creeper. */
	public void attacked(@Nullable Entity attacker, long gameTime) {
		if (fusing) {
			hitDuringFuse = true;
		}
		startle(CreeperTuning.ATTACK_POINTS, attacker);
		if (attacker instanceof LivingEntity living && watchable(living)) {
			watch(living, gameTime);
		}
	}

	/** The hiss is called off: the meter drops to DEFUSED_METER and vanilla's swell runs back down. */
	public void defuse(Creeper creeper) {
		fusing = false;
		hitDuringFuse = false;
		meter = Math.min(meter, CreeperTuning.DEFUSED_METER);
		creeper.setSwellDir(-1);
	}

	public void tick(Creeper creeper) {
		long time = creeper.level().getGameTime();
		if (!active(creeper)) {
			if (fusing) {
				defuse(creeper);
			}
			meter = Math.max(0.0F, meter - CreeperTuning.METER_DECAY_PER_TICK);
			watched = null;
			startler = null;
			spot = null;
			lastPositions.clear();
			return;
		}
		if (!fusing) {
			meter = Math.max(0.0F, meter - CreeperTuning.METER_DECAY_PER_TICK);
		}
		if (startler != null && (!startler.isAlive() || startler.distanceToSqr(creeper) > CreeperTuning.WATCH_RANGE * CreeperTuning.WATCH_RANGE)) {
			startler = null;
		}
		if (watched != null && !stillWatching(creeper, watched, time)) {
			watched = null;
			spot = null;
		}
		int phase = creeper.getId() + (int) time;
		if (Math.floorMod(phase, CreeperTuning.MOTION_SCAN_INTERVAL) == 0) {
			scanMotion(creeper, time);
		}
		if (Math.floorMod(phase, CreeperTuning.PROJECTILE_SCAN_INTERVAL) == 0) {
			scanProjectiles(creeper, time);
		}
		if (Math.floorMod(phase, 20) == 0) {
			tickLeaving(creeper, time);
		}
	}

	/** Leaving by day (mod-spawned watchers only), once a second: the countdown, then the despawn roll while unseen. */
	private void tickLeaving(Creeper creeper, long time) {
		if (!creeper.hasAttached(WatcherSpawning.WATCHER_FOR)) {
			return;
		}
		if (!creeper.level().isBrightOutside()) {
			leaveCountdown = -1;
			leaving = false;
			return;
		}
		long unseen = lastLookedAt == Long.MIN_VALUE ? Long.MAX_VALUE / 2 : time - lastLookedAt;
		if (!leaving) {
			if (leaveCountdown < 0) {
				leaveCountdown = Mth.nextInt(creeper.getRandom(), CreeperTuning.LEAVE_DELAY_MIN, CreeperTuning.LEAVE_DELAY_MAX);
			}
			leaveCountdown -= unseen >= CreeperTuning.LEAVE_UNSEEN_TICKS ? 40 : 20;
			if (leaveCountdown > 0) {
				return;
			}
			leaving = true;
		}
		if (fusing || seenByAnyPlayer(creeper)) {
			return;
		}
		double chance = CreeperTuning.LEAVE_CHANCE + CreeperTuning.LEAVE_CHANCE_PER_UNSEEN_SECOND * Math.min(unseen / 20.0, 1000.0);
		if (creeper.getRandom().nextDouble() < chance) {
			creeper.discard();
		}
	}

	/** Whether any player (not spectators) within LEAVE_SEEN_RANGE has a clear view of its eyes, middle or top. */
	public static boolean seenByAnyPlayer(Creeper creeper) {
		double rangeSqr = CreeperTuning.LEAVE_SEEN_RANGE * CreeperTuning.LEAVE_SEEN_RANGE;
		Vec3 feet = creeper.position();
		Vec3[] points = {feet.add(0.0, creeper.getEyeHeight(), 0.0), feet.add(0.0, creeper.getBbHeight() * 0.5, 0.0), feet.add(0.0, creeper.getBbHeight(), 0.0)};
		for (Player player : creeper.level().players()) {
			if (player.isSpectator() || player.distanceToSqr(creeper) > rangeSqr) {
				continue;
			}
			Vec3 eye = player.getEyePosition();
			for (Vec3 point : points) {
				if (CoverFinder.clear(creeper.level(), eye, point)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean stillWatching(Creeper creeper, LivingEntity entity, long time) {
		if (!entity.isAlive() || entity.isRemoved() || entity.level() != creeper.level() || !watchable(entity)
				|| entity.distanceToSqr(creeper) > CreeperTuning.WATCH_RANGE * CreeperTuning.WATCH_RANGE) {
			return false;
		}
		// Players are watched for as long as they're in range (owner C2); anything else for a while.
		return entity instanceof Player || time - watchedSince < CreeperTuning.NON_PLAYER_WATCH_TICKS;
	}

	/** What a creeper pays attention to: living things other than creepers; not spectators (nor creative players, if so tuned) or the invisible. */
	public static boolean watchable(LivingEntity entity) {
		if (entity instanceof Creeper || entity.isSpectator() || entity.isInvisible()) {
			return false;
		}
		return !(CreeperTuning.IGNORE_CREATIVE_PLAYERS && entity instanceof Player player && player.isCreative());
	}

	private void scanMotion(Creeper creeper, long time) {
		AABB box = creeper.getBoundingBox().inflate(CreeperTuning.WATCH_RANGE);
		List<LivingEntity> nearby = creeper.level().getEntitiesOfClass(LivingEntity.class, box,
				e -> e != creeper && e.isAlive() && watchable(e));
		Int2ObjectMap<Vec3> positions = new Int2ObjectOpenHashMap<>(nearby.size());
		Vec3 eye = creeper.getEyePosition();
		Vec3 look = creeper.getViewVector(1.0F);
		double cosFov = Math.cos(Math.toRadians(CreeperTuning.FOV_HALF_ANGLE));
		double fastSqr = CreeperTuning.FAST_RANGE * CreeperTuning.FAST_RANGE;
		double watchSqr = CreeperTuning.WATCH_RANGE * CreeperTuning.WATCH_RANGE;
		LivingEntity bestPlayer = null;
		LivingEntity bestOther = null;
		double bestPlayerDistance = Double.MAX_VALUE;
		double bestOtherDistance = Double.MAX_VALUE;
		for (LivingEntity entity : nearby) {
			Vec3 now = entity.position();
			positions.put(entity.getId(), now);
			Vec3 before = lastPositions.get(entity.getId());
			if (before == null) {
				continue;
			}
			// A rider moves with its mount; count the mount (B18: the same rule for everyone, counted once).
			if (entity.getVehicle() instanceof LivingEntity) {
				continue;
			}
			Vec3 moved = now.subtract(before);
			double speed = Math.sqrt(moved.x * moved.x + moved.z * moved.z
					+ Math.pow(moved.y * CreeperTuning.VERTICAL_SPEED_WEIGHT, 2)) / CreeperTuning.MOTION_SCAN_INTERVAL;
			if (speed < CreeperTuning.NOTICE_SPEED) {
				continue;
			}
			double distanceSqr = entity.distanceToSqr(creeper);
			if (distanceSqr > watchSqr) {
				continue;
			}
			if (speed > CreeperTuning.FAST_SPEED && distanceSqr <= fastSqr && creeper.getSensing().hasLineOfSight(entity)) {
				startle(CreeperTuning.FAST_MOVE_POINTS, entity);
			}
			// Motion in its field of view catches its eye (players first).
			Vec3 to = entity.getEyePosition().subtract(eye);
			double toLength = to.horizontalDistance();
			boolean inView = toLength < 1.0E-3 || (look.x * to.x + look.z * to.z) / (look.horizontalDistance() * toLength + 1.0E-9) >= cosFov;
			if (!inView) {
				continue;
			}
			if (entity instanceof Player) {
				if (distanceSqr < bestPlayerDistance && creeper.getSensing().hasLineOfSight(entity)) {
					bestPlayer = entity;
					bestPlayerDistance = distanceSqr;
				}
			} else if (distanceSqr < bestOtherDistance && creeper.getSensing().hasLineOfSight(entity)) {
				bestOther = entity;
				bestOtherDistance = distanceSqr;
			}
		}
		lastPositions = positions;
		if (watched instanceof Player) {
			return;
		}
		if (bestPlayer != null) {
			watch(bestPlayer, time);
		} else if (watched == null && bestOther != null) {
			watch(bestOther, time);
		}
	}

	private void scanProjectiles(Creeper creeper, long time) {
		countedProjectiles.int2LongEntrySet().removeIf(e -> time - e.getLongValue() > 60);
		AABB box = creeper.getBoundingBox().inflate(CreeperTuning.PROJECTILE_SCAN_RANGE);
		List<Projectile> projectiles = creeper.level().getEntitiesOfClass(Projectile.class, box,
				p -> p.isAlive() && p.getDeltaMovement().lengthSqr() > 0.04);
		if (projectiles.isEmpty()) {
			return;
		}
		Vec3 middle = creeper.position().add(0.0, creeper.getBbHeight() / 2.0, 0.0);
		double radiusSqr = CreeperTuning.NEAR_MISS_RADIUS * CreeperTuning.NEAR_MISS_RADIUS;
		for (Projectile projectile : projectiles) {
			if (countedProjectiles.containsKey(projectile.getId())) {
				continue;
			}
			// The stretch it flew since the last scan (straight-line estimate from its current velocity).
			Vec3 end = projectile.position();
			Vec3 start = end.subtract(projectile.getDeltaMovement().scale(CreeperTuning.PROJECTILE_SCAN_INTERVAL));
			if (distanceToSegmentSqr(middle, start, end) <= radiusSqr) {
				countedProjectiles.put(projectile.getId(), time);
				startle(CreeperTuning.NEAR_MISS_POINTS, projectile.getOwner() != null ? projectile.getOwner() : projectile);
			}
		}
	}

	static double distanceToSegmentSqr(Vec3 point, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double lengthSqr = ab.lengthSqr();
		double t = lengthSqr < 1.0E-9 ? 0.0 : Math.clamp(point.subtract(a).dot(ab) / lengthSqr, 0.0, 1.0);
		return point.distanceToSqr(a.add(ab.scale(t)));
	}
}

// dte:C1,B18,B19
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.meter.Meters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One per vanilla drowned (server only, not saved): the lurker's state and senses (specs/drowned.md). Ticked from
 * {@code Drowned#tick}; it steers the drowned directly (velocity and facing) while {@link DrownedLurkGoal} keeps
 * vanilla's movement and look goals out. With no player within ACTIVATE_RANGE it is IDLE and vanilla's wandering runs.
 */
public final class DrownedMind {
	public enum State {
		/** Vanilla: wandering (and vanilla's daytime "go back to water"). */
		IDLE,
		/** Swimming to its lurk spot. */
		TRAVEL,
		/** At its spot, upright, head above water (or sunk, while stared at). */
		LURK,
		/** Lunging at a victim at the edge. */
		SURGE,
		/** Holding a victim, moving backwards toward the deepest water. */
		DRAG,
		/** Swimming over to grab the other leg of a victim another drowned drags. */
		JOIN,
		/** Holding a victim's other leg beside the one that drags it (not ridden: it follows the victim). */
		HELP
	}

	private static final Identifier CRAWL_MODIFIER = BubbasReworkedMobs.id("drowned_land_crawl");

	private State state = State.IDLE;
	private LurkSpots.@Nullable Spot spot;
	private @Nullable BlockPos avoid;
	private @Nullable LivingEntity victim;
	private @Nullable Vec3 deepTarget;
	private boolean stared;
	private boolean sunk;
	private float grip;
	private long lastSearch = Long.MIN_VALUE;
	/** Keeps its spot with no player near (client test screenshots, a test world station). */
	private boolean pinned;
	/** Dragging: the drowned that joined in (JOIN or HELP). */
	private final List<Drowned> helpers = new ArrayList<>();
	/** Joining or helping: the drowned that drags the victim, and the angle round from its grip that this one holds from. */
	private @Nullable Drowned leader;
	private float sideAngle;
	/** The grip value last sent to clients (show_meters). */
	private int sentGrip;

	public State state() {
		return state;
	}

	public LurkSpots.@Nullable Spot spot() {
		return spot;
	}

	public boolean isStared() {
		return stared;
	}

	/** Sunk below the surface (being stared at). */
	public boolean isSunk() {
		return sunk;
	}

	public float grip() {
		return grip;
	}

	public @Nullable LivingEntity victim() {
		return state == State.DRAG || state == State.JOIN || state == State.HELP ? victim : null;
	}

	/** Dragging: how dangerous its hits are, from the grip: 0 at GRIP_START or less, 1 at GRIP_FULL. */
	public float escalation() {
		return Mth.clamp((grip - DrownedTuning.GRIP_START) / (DrownedTuning.GRIP_FULL - DrownedTuning.GRIP_START), 0.0F, 1.0F);
	}

	/** For tests and the client test: set the grip meter (dragging only). */
	public void setGrip(float value) {
		grip = Mth.clamp(value, 0.0F, DrownedTuning.GRIP_FULL);
	}

	/** Dragging: the extra drowned holding on now (HELP, not still swimming over). */
	public int holdingHelpers() {
		int n = 0;
		for (Drowned helper : helpers) {
			if (DrownedAccess.mind(helper).state == State.HELP) {
				n++;
			}
		}
		return n;
	}

	public List<Drowned> helpers() {
		return List.copyOf(helpers);
	}

	public @Nullable Drowned leader() {
		return state == State.JOIN || state == State.HELP ? leader : null;
	}

	/** Whether our goal holds the drowned's movement (anything but IDLE). */
	public boolean steering() {
		return state != State.IDLE;
	}

	/** Vanilla's swim control should swim (not walk the floor) while it travels to its spot. */
	public boolean wantsToSwim() {
		return state == State.TRAVEL || state == State.JOIN;
	}

	/** Lying down (swimming, crawling on land) or upright (lurking; dragging in water). Owner, 2026-11-16. */
	public boolean lying(Drowned drowned) {
		if (state == State.LURK) {
			return false;
		}
		if ((state == State.DRAG || state == State.HELP) && drowned.isInWater()) {
			return false;
		}
		return true;
	}

	// --- who counts ------------------------------------------------------------------------------

	/** Players (not spectators) and passive mobs (animals, villagers) that can drown: the same rules for both (B18). */
	public static boolean grabbable(LivingEntity entity) {
		if (!entity.isAlive() || entity.isInvisible() || entity.isPassenger() || entity.isVehicle() || entity.canBreatheUnderwater()) {
			return false;
		}
		if (entity instanceof Player player) {
			return !player.isSpectator() && (DrownedTuning.GRAB_CREATIVE_PLAYERS || !player.isCreative());
		}
		return entity instanceof Animal || entity instanceof AbstractVillager;
	}

	/** Anything whose stare makes it sink: what it could grab, plus creative players (so the owner can try it). */
	public static boolean watcher(LivingEntity entity) {
		if (!entity.isAlive() || entity.isInvisible()) {
			return false;
		}
		if (entity instanceof Player player) {
			return !player.isSpectator();
		}
		return (entity instanceof Animal || entity instanceof AbstractVillager) && !entity.canBreatheUnderwater();
	}

	// --- the tick --------------------------------------------------------------------------------

	public void tick(Drowned drowned) {
		ServerLevel level = (ServerLevel) drowned.level();
		long time = level.getGameTime();
		updateCrawlSpeed(drowned);
		// Visible meter (game rule show_meters): its grip, blue above its head and on the held player's screen.
		sentGrip = Meters.sync(drowned, Meters.GRIP, state == State.DRAG ? grip : 0.0F, sentGrip, DrownedTuning.GRIP_FULL);
		if (state != State.DRAG && drowned.isVehicle()) {
			// A passenger we don't hold (loaded from a save): let go.
			drowned.ejectPassengers();
		}
		if (drowned.isNoAi()) {
			return;
		}
		switch (state) {
			case IDLE, TRAVEL -> tickSeeking(drowned, level, time);
			case LURK -> tickLurk(drowned, level, time);
			case SURGE -> tickSurge(drowned, level);
			case DRAG -> tickDrag(drowned, level);
			case JOIN, HELP -> tickHelper(drowned, level);
		}
	}

	private void updateCrawlSpeed(Drowned drowned) {
		AttributeInstance speed = drowned.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		boolean crawling = !drowned.isInWater();
		if (crawling != speed.hasModifier(CRAWL_MODIFIER)) {
			if (crawling) {
				speed.addTransientModifier(new AttributeModifier(CRAWL_MODIFIER, DrownedTuning.LAND_CRAWL_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			} else {
				speed.removeModifier(CRAWL_MODIFIER);
			}
		}
	}

	private void tickSeeking(Drowned drowned, ServerLevel level, long time) {
		Player player = level.getNearestPlayer(drowned.getX(), drowned.getY(), drowned.getZ(), DrownedTuning.ACTIVATE_RANGE,
				e -> !e.isSpectator());
		if (player == null && !(pinned && spot != null)) {
			if (state != State.IDLE) {
				enterIdle(drowned);
			}
			return;
		}
		if (player != null && !pinned && spot != null && spot.horizontalDistanceSqr(player) > DrownedTuning.SPOT_RELEASE_DISTANCE * DrownedTuning.SPOT_RELEASE_DISTANCE) {
			spot = null;
		}
		if (spot == null && player != null && (time + drowned.getId()) % DrownedTuning.SPOT_SEARCH_INTERVAL == 0 && time != lastSearch) {
			lastSearch = time;
			spot = LurkSpots.find(level, drowned, player, avoid, drowned.getRandom());
		}
		if (spot == null) {
			if (state != State.IDLE) {
				enterIdle(drowned);
			}
			return;
		}
		state = State.TRAVEL;
		if (spot.horizontalDistanceSqr(drowned) <= DrownedTuning.ARRIVE_DISTANCE * DrownedTuning.ARRIVE_DISTANCE
				&& Math.abs(drowned.getY() - spot.floorTop()) < 2.0) {
			drowned.getNavigation().stop();
			state = State.LURK;
			drowned.setNoGravity(true);
			return;
		}
		if (drowned.getNavigation().isDone() || time % 20 == drowned.getId() % 20) {
			Vec3 c = spot.centre();
			drowned.getNavigation().moveTo(c.x, c.y, c.z, DrownedTuning.TRAVEL_SPEED);
		}
	}

	private static void posture(Drowned drowned, Pose pose) {
		if (drowned.getPose() != pose) {
			drowned.setPose(pose);
		}
	}

	private void enterIdle(Drowned drowned) {
		leader = null;
		posture(drowned, Pose.STANDING);
		state = State.IDLE;
		spot = null;
		sunk = false;
		stared = false;
		drowned.setNoGravity(false);
	}

	private void tickLurk(Drowned drowned, ServerLevel level, long time) {
		LurkSpots.Spot s = spot;
		if (s == null || !LurkSpots.isOpenWater(level, s.floor().above())) {
			enterIdle(drowned);
			return;
		}
		LivingEntity surgeAt = null;
		if ((time + drowned.getId()) % DrownedTuning.SCAN_INTERVAL == 0) {
			surgeAt = scan(drowned, level, s);
			if (spot == null) {
				// No player near any more.
				return;
			}
		}
		sunk = stared;
		// Sunk: it also crouches (the synced vanilla pose; the model drops its head about a quarter block), since in
		// 2-deep water the floor stops a standing drowned with the top of its head level with the surface.
		posture(drowned, sunk ? Pose.CROUCHING : Pose.STANDING);
		if (surgeAt != null) {
			startSurge(drowned, surgeAt);
			return;
		}
		// Hold the spot: a spring toward the centre and the lurk (or sunk) height.
		double targetFeet = sunk
				? Math.max(s.floorTop(), s.surfaceY() - drowned.getBbHeight() - DrownedTuning.SUNK_HEAD_BELOW_SURFACE)
				: Math.max(s.floorTop(), s.surfaceY() - drowned.getEyeHeight() + DrownedTuning.LURK_EYES_ABOVE_SURFACE);
		Vec3 c = s.centre();
		double vx = Mth.clamp((c.x - drowned.getX()) * 0.2, -0.05, 0.05);
		double vz = Mth.clamp((c.z - drowned.getZ()) * 0.2, -0.05, 0.05);
		double vy = Mth.clamp((targetFeet - drowned.getY()) * 0.25, -DrownedTuning.BOB_SPEED, DrownedTuning.BOB_SPEED);
		drowned.setDeltaMovement(vx, vy, vz);
		face(drowned, s.shoreYaw());
		Player nearest = level.getNearestPlayer(drowned, DrownedTuning.SENSE_RANGE);
		if (nearest != null) {
			drowned.getLookControl().setLookAt(nearest, 30.0F, 30.0F);
		}
	}

	/** One senses scan while lurking: is it stared at, and is someone at the edge with their back turned? */
	private @Nullable LivingEntity scan(Drowned drowned, ServerLevel level, LurkSpots.Spot s) {
		if (!pinned && level.getNearestPlayer(drowned.getX(), drowned.getY(), drowned.getZ(), DrownedTuning.ACTIVATE_RANGE, e -> !e.isSpectator()) == null) {
			enterIdle(drowned);
			return null;
		}
		AABB box = drowned.getBoundingBox().inflate(DrownedTuning.SENSE_RANGE);
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != drowned && watcher(e));
		// Where its head shows when lurking: looking there is staring at it, sunk or not (so it doesn't bob in and
		// out of a watcher's sight at the edge of the bank).
		Vec3 head = lurkPoint(s);
		double cone = Math.cos(Math.toRadians(DrownedTuning.STARE_HALF_ANGLE));
		boolean anyStare = false;
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		double surgeSqr = DrownedTuning.SURGE_RANGE * DrownedTuning.SURGE_RANGE;
		for (LivingEntity e : nearby) {
			Vec3 eye = e.getEyePosition();
			Vec3 to = head.subtract(eye);
			double length = to.length();
			if (length < 1.0E-4) {
				continue;
			}
			if (!anyStare && e.getViewVector(1.0F).dot(to) / length >= cone && clearSight(level, e, eye, head)) {
				anyStare = true;
			}
			if (!grabbable(e)) {
				continue;
			}
			double dx = e.getX() - drowned.getX();
			double dz = e.getZ() - drowned.getZ();
			double horizontal = dx * dx + dz * dz;
			if (horizontal > surgeSqr || horizontal >= bestDistance) {
				continue;
			}
			if (e.getY() < s.surfaceY() - DrownedTuning.SURGE_BELOW_SURFACE || e.getY() > s.surfaceY() + DrownedTuning.SURGE_ABOVE_SURFACE || e.isUnderWater()) {
				continue;
			}
			if (!backTurned(e, drowned) || !drowned.getSensing().hasLineOfSight(e)) {
				continue;
			}
			best = e;
			bestDistance = horizontal;
		}
		stared = anyStare;
		if (anyStare && DrownedTuning.WATCHED_BLOCKS_SURGE) {
			return null;
		}
		return best;
	}

	/** The point a watcher has to look at: the middle of the spot, just above the water's surface. */
	public static Vec3 lurkPoint(LurkSpots.Spot s) {
		Vec3 c = s.centre();
		return new Vec3(c.x, s.surfaceY() + 0.1, c.z);
	}

	private static boolean clearSight(ServerLevel level, Entity watcher, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, watcher)).getType() == HitResult.Type.MISS;
	}

	/** The drowned is more than BACK_TURNED_ANGLE degrees (horizontally) from where {@code e} faces. */
	public static boolean backTurned(LivingEntity e, Entity drowned) {
		double dx = drowned.getX() - e.getX();
		double dz = drowned.getZ() - e.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			return false;
		}
		float yaw = e.getYHeadRot() * Mth.DEG_TO_RAD;
		double fx = -Mth.sin(yaw);
		double fz = Mth.cos(yaw);
		double cos = (fx * dx + fz * dz) / length;
		return cos < Math.cos(Math.toRadians(DrownedTuning.BACK_TURNED_ANGLE));
	}

	private void startSurge(Drowned drowned, LivingEntity target) {
		state = State.SURGE;
		victim = target;
		sunk = false;
		posture(drowned, Pose.STANDING);
		drowned.setNoGravity(false);
		drowned.playSound(SoundEvents.DROWNED_SWIM, 1.0F, 0.8F);
	}

	private void tickSurge(Drowned drowned, ServerLevel level) {
		LivingEntity target = victim;
		LurkSpots.Spot s = spot;
		if (target == null || s == null || !grabbable(target)) {
			endSurge(drowned);
			return;
		}
		double dx = target.getX() - drowned.getX();
		double dz = target.getZ() - drowned.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal > DrownedTuning.SURGE_GIVE_UP || Math.sqrt(s.horizontalDistanceSqr(drowned)) > DrownedTuning.LAND_REACH + 1.0) {
			endSurge(drowned);
			return;
		}
		if (drowned.getBoundingBox().inflate(DrownedTuning.GRAB_REACH).intersects(target.getBoundingBox())) {
			grab(drowned, level, target);
			return;
		}
		double speed = DrownedTuning.SURGE_SPEED;
		double vx = horizontal > 1.0E-4 ? dx / horizontal * speed : 0.0;
		double vz = horizontal > 1.0E-4 ? dz / horizontal * speed : 0.0;
		double vy = drowned.getDeltaMovement().y;
		boolean higher = target.getY() > drowned.getY() + 0.3;
		if (drowned.isInWater() && higher) {
			vy = 0.2;
		} else if (drowned.horizontalCollision && (drowned.onGround() || drowned.isInWater())) {
			vy = DrownedTuning.SURGE_CLIMB;
		}
		drowned.setDeltaMovement(vx, vy, vz);
		face(drowned, (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
	}

	private void endSurge(Drowned drowned) {
		victim = null;
		state = spot != null ? State.TRAVEL : State.IDLE;
	}

	// --- the grab --------------------------------------------------------------------------------

	/** Grabs {@code target}: it rides the drowned (held in front, can't swim or sneak off) until the grip breaks. */
	public void grab(Drowned drowned, ServerLevel level, LivingEntity target) {
		if (!target.startRiding(drowned, true, true)) {
			endSurge(drowned);
			return;
		}
		state = State.DRAG;
		victim = target;
		grip = DrownedTuning.GRIP_START;
		helpers.clear();
		sunk = false;
		posture(drowned, Pose.STANDING);
		stared = false;
		drowned.setNoGravity(false);
		drowned.getNavigation().stop();
		deepTarget = null;
		drowned.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, drowned.getX(), drowned.getY(), drowned.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, drowned.getSoundSource(), 1.0F, 0.9F);
	}

	private void tickDrag(Drowned drowned, ServerLevel level) {
		LivingEntity held = victim;
		if (held == null || !held.isAlive() || held.getVehicle() != drowned) {
			// Gone (died, ender pearl, a command): back to lurking somewhere.
			victim = null;
			releaseHelpers(drowned, false);
			enterIdle(drowned);
			return;
		}
		// The grip tightens while it drags (faster with others holding on), and with it its hits.
		grip = Math.min(DrownedTuning.GRIP_FULL,
				grip + DrownedTuning.GRIP_RISE_PER_TICK * (1.0F + DrownedTuning.GRIP_RISE_PER_HELPER * holdingHelpers()));
		float tight = escalation();
		helpers.removeIf(h -> !h.isAlive() || DrownedAccess.mind(h).leader != drowned);
		if (helpers.size() < DrownedTuning.MAX_EXTRA_HOLDERS && (level.getGameTime() + drowned.getId()) % DrownedTuning.JOIN_SCAN_INTERVAL == 0) {
			recruit(drowned, level, held);
		}
		// Toward the deepest water nearby, backwards (facing away from it, toward the victim and the shore).
		if (deepTarget == null || horizontalDistance(drowned.position(), deepTarget) < DrownedTuning.DRAG_ARRIVE) {
			Vec3 next = findDeeper(level, drowned);
			if (next != null) {
				deepTarget = next;
			} else if (deepTarget == null && !drowned.isInWater() && spot != null) {
				// Up the bank with no water column found from here: back to its spot first.
				deepTarget = spot.centre();
			}
		}
		Vec3 v = drowned.getDeltaMovement();
		double vx = 0.0;
		double vz = 0.0;
		double vy = v.y;
		if (deepTarget != null) {
			double dx = deepTarget.x - drowned.getX();
			double dz = deepTarget.z - drowned.getZ();
			double h = Math.sqrt(dx * dx + dz * dz);
			if (h > 0.2) {
				double speed = (drowned.isInWater() ? DrownedTuning.DRAG_SPEED : DrownedTuning.DRAG_LAND_SPEED)
						* (1.0 + DrownedTuning.DRAG_SPEED_PER_HELPER * holdingHelpers());
				vx = dx / h * Math.min(speed, h * 0.5);
				vz = dz / h * Math.min(speed, h * 0.5);
				face(drowned, (float) (Mth.atan2(-dz, -dx) * Mth.RAD_TO_DEG) - 90.0F);
			}
			if (drowned.isInWater()) {
				vy = Math.min(vy, -0.03);
			}
			if (drowned.horizontalCollision && (drowned.onGround() || drowned.isInWater())) {
				vy = 0.3;
			}
		}
		drowned.setDeltaMovement(vx, vy, vz);
		squeeze(drowned, level, held, tight);
		// The tightening shows: more bubbles stream off the victim the longer it holds them.
		if (held.isUnderWater() && level.getGameTime() % 2 == 0) {
			level.sendParticles(ParticleTypes.BUBBLE, held.getX(), held.getY() + 0.3, held.getZ(),
					1 + Math.round(tight * DrownedTuning.ESCALATION_BUBBLES), 0.25, 0.3, 0.25, 0.05);
		}
	}

	/** Its hits (a chance each tick, no fixed rhythm: B19), more often and harder as the grip tightens. */
	private static void squeeze(Drowned drowned, ServerLevel level, LivingEntity held, float tight) {
		if (drowned.getRandom().nextFloat() < Mth.lerp(tight, DrownedTuning.HIT_CHANCE_PER_TICK, DrownedTuning.HIT_CHANCE_FULL)) {
			hit(drowned, level, held, tight);
		}
	}

	/** Drowned already near (not dragging, not vanilla) swim over to grab the other leg; no new ones spawn. */
	private void recruit(Drowned drowned, ServerLevel level, LivingEntity held) {
		AABB box = drowned.getBoundingBox().inflate(DrownedTuning.JOIN_RANGE);
		List<Drowned> near = level.getEntitiesOfClass(Drowned.class, box, d -> d != drowned && d.isAlive() && !d.isNoAi() && !d.isVehicle()
				&& !((DrownedAccess) d).brm$isVanilla() && available(DrownedAccess.mind(d).state));
		near.sort(java.util.Comparator.comparingDouble(d -> d.distanceToSqr(held)));
		for (Drowned d : near) {
			if (helpers.size() >= DrownedTuning.MAX_EXTRA_HOLDERS) {
				break;
			}
			float side = helpers.isEmpty() ? DrownedTuning.JOIN_SIDE_ANGLE : -DrownedTuning.JOIN_SIDE_ANGLE;
			for (Drowned h : helpers) {
				if (DrownedAccess.mind(h).sideAngle == side) {
					side = -side;
				}
			}
			DrownedAccess.mind(d).join(d, drowned, held, side);
			helpers.add(d);
		}
	}

	private static boolean available(State state) {
		return state == State.IDLE || state == State.TRAVEL || state == State.LURK;
	}

	/** Called on a drowned that will swim over and grab the other leg of {@code held}, which {@code lead} drags. */
	public void join(Drowned drowned, Drowned lead, LivingEntity held, float side) {
		enterIdle(drowned);
		state = State.JOIN;
		leader = lead;
		victim = held;
		sideAngle = side;
		drowned.getNavigation().stop();
	}

	/** Its grip point: at the victim's feet, from the side (SIDE degrees round from the dragging drowned's grip), level with it. */
	private Vec3 gripPoint(Drowned lead, LivingEntity held) {
		float yaw = (lead.getYRot() + 180.0F + sideAngle) * Mth.DEG_TO_RAD;
		double fx = -Mth.sin(yaw);
		double fz = Mth.cos(yaw);
		return new Vec3(held.getX() + fx * DrownedTuning.HOLD_FORWARD, lead.getY(), held.getZ() + fz * DrownedTuning.HOLD_FORWARD);
	}

	private void tickHelper(Drowned drowned, ServerLevel level) {
		Drowned lead = leader;
		LivingEntity held = victim;
		if (lead == null || held == null || !lead.isAlive() || !held.isAlive() || DrownedAccess.mind(lead).state != State.DRAG
				|| DrownedAccess.mind(lead).victim != held) {
			letGo(drowned, false);
			return;
		}
		Vec3 target = gripPoint(lead, held);
		Vec3 to = target.subtract(drowned.position());
		double distance = to.length();
		if (state == State.JOIN && distance > DrownedTuning.JOIN_GIVE_UP) {
			letGo(drowned, false);
			return;
		}
		drowned.setNoGravity(drowned.isInWater());
		if (state == State.JOIN) {
			if (distance <= DrownedTuning.JOIN_REACH) {
				state = State.HELP;
				drowned.swingForAttack(InteractionHand.MAIN_HAND);
				level.playSound(null, drowned.getX(), drowned.getY(), drowned.getZ(), SoundEvents.PLAYER_SPLASH, drowned.getSoundSource(), 0.8F, 0.8F);
			} else {
				Vec3 v = to.scale(DrownedTuning.JOIN_SPEED / distance);
				drowned.setDeltaMovement(v.x, drowned.isInWater() ? v.y : drowned.getDeltaMovement().y, v.z);
				if (drowned.horizontalCollision && (drowned.onGround() || drowned.isInWater())) {
					drowned.setDeltaMovement(v.x, 0.3, v.z);
				}
				face(drowned, (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F);
				return;
			}
		}
		if (distance > DrownedTuning.JOIN_REACH * 3.0) {
			// Shaken loose (pulled round a corner): swim back to it.
			state = State.JOIN;
			return;
		}
		// Holding on: a spring onto its grip point, facing the victim's feet.
		double k = 0.5;
		drowned.setDeltaMovement(Mth.clamp(to.x * k, -0.3, 0.3), drowned.isInWater() ? Mth.clamp(to.y * k, -0.3, 0.3) : drowned.getDeltaMovement().y,
				Mth.clamp(to.z * k, -0.3, 0.3));
		double dx = held.getX() - drowned.getX();
		double dz = held.getZ() - drowned.getZ();
		face(drowned, (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
		squeeze(drowned, level, held, DrownedAccess.mind(lead).escalation());
	}

	/** A helper lets go (the dragging one let go, or the victim is gone); with {@code recoil} it is pushed back too. */
	private void letGo(Drowned drowned, boolean recoil) {
		LivingEntity held = victim;
		leader = null;
		victim = null;
		avoid = drowned.blockPosition();
		enterIdle(drowned);
		if (recoil && held != null) {
			Vec3 away = drowned.position().subtract(held.position()).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize().scale(DrownedTuning.RECOIL_SPEED);
			drowned.setDeltaMovement(away.x, 0.1, away.z);
			drowned.needsSync = true;
		}
	}

	private void releaseHelpers(Drowned drowned, boolean recoil) {
		for (Drowned helper : helpers) {
			DrownedMind mind = DrownedAccess.mind(helper);
			if (mind.leader == drowned) {
				mind.letGo(helper, recoil);
			}
		}
		helpers.clear();
	}

	private static double horizontalDistance(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static @Nullable Vec3 findDeeper(ServerLevel level, Drowned drowned) {
		BlockPos at = drowned.blockPosition();
		// The water's top block above it (or, on land, the spot's level): scan up a few blocks.
		int surface = at.getY();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(at.getX(), at.getY(), at.getZ());
		for (int i = 0; i < DrownedTuning.DEEP_MAX_DEPTH && LurkSpots.isOpenWater(level, pos.set(at.getX(), surface + 1, at.getZ())); i++) {
			surface++;
		}
		if (!LurkSpots.isOpenWater(level, pos.set(at.getX(), surface, at.getZ()))) {
			// On land: the water is a block below its feet at most (it only goes a few blocks up the bank).
			surface = at.getY() - 1;
		}
		int floor = at.getY();
		while (floor > at.getY() - DrownedTuning.DEEP_MAX_DEPTH && LurkSpots.isOpenWater(level, pos.set(at.getX(), floor - 1, at.getZ()))) {
			floor--;
		}
		if (!LurkSpots.isOpenWater(level, pos.set(at.getX(), at.getY(), at.getZ()))) {
			floor = Integer.MAX_VALUE;
		}
		return LurkSpots.deepest(level, at, surface, floor);
	}

	/** One small hit on the held victim: damage, and air knocked out unless it can't lose air (vanilla's Water Breathing, Conduit Power, creative). */
	public static void hit(Drowned drowned, ServerLevel level, LivingEntity held) {
		hit(drowned, level, held, 0.0F);
	}

	/** One hit with the grip {@code tight} (0 just grabbed, 1 at its tightest): harder, more air, a louder, lower squeeze. */
	public static void hit(Drowned drowned, ServerLevel level, LivingEntity held, float tight) {
		if (!held.hurtServer(level, level.damageSources().mobAttack(drowned), Mth.lerp(tight, DrownedTuning.HIT_DAMAGE, DrownedTuning.HIT_DAMAGE_FULL))) {
			return;
		}
		drowned.swingForAttack(InteractionHand.MAIN_HAND);
		level.playSound(null, drowned.getX(), drowned.getY(), drowned.getZ(), SoundEvents.DROWNED_AMBIENT_WATER, drowned.getSoundSource(),
				0.6F + 0.6F * tight, 1.1F - 0.4F * tight);
		if (held.canBreatheUnderwater() || MobEffectUtil.hasWaterBreathing(held)
				|| held instanceof Player player && player.getAbilities().invulnerable) {
			return;
		}
		double oxygen = held.getAttributeValue(Attributes.OXYGEN_BONUS);
		int loss = (int) Math.round(Mth.lerp(tight, DrownedTuning.HIT_AIR_LOSS, DrownedTuning.HIT_AIR_LOSS_FULL) / (1.0 + Math.max(0.0, oxygen)));
		held.setAirSupply(Math.max(0, held.getAirSupply() - loss));
	}

	// --- the grip meter -------------------------------------------------------------------------

	/**
	 * A hit on the drowned while it holds someone. Anyone's hit counts (the victim's, a friend's, an arrow: B18); fast
	 * hits beat the tightening, and axes, Impaling and Knockback count extra. At 0 it lets go.
	 */
	public void hurt(Drowned drowned, ServerLevel level, Entity attacker, @Nullable Entity direct, @Nullable ItemStack weapon, float damage) {
		if ((state == State.HELP || state == State.JOIN) && leader != null && leader.isAlive()) {
			// A hit on a helper counts toward the grip it is part of.
			DrownedAccess.mind(leader).addGrip(leader, gripPoints(level, weapon, damage));
			return;
		}
		if (state != State.DRAG) {
			if (state == State.LURK || state == State.TRAVEL) {
				// Hit from the shore: find another spot (no free hits on a sitting target).
				avoid = spot != null ? spot.floor() : drowned.blockPosition();
				enterIdle(drowned);
			}
			return;
		}
		addGrip(drowned, gripPoints(level, weapon, damage));
	}

	/** A hit loosens the grip by its points, split among the holders (each extra one: GRIP_EXTRA_HOLDER_DIVISOR more). At 0 all let go. */
	public void addGrip(Drowned drowned, float points) {
		if (state != State.DRAG) {
			return;
		}
		grip -= points / (1.0F + DrownedTuning.GRIP_EXTRA_HOLDER_DIVISOR * holdingHelpers());
		if (grip <= 0.0F) {
			release(drowned, true);
		}
	}

	/** The meter points for one hit of {@code damage} (before armor) with {@code weapon}. */
	public static float gripPoints(ServerLevel level, @Nullable ItemStack weapon, float damage) {
		float points = DrownedTuning.GRIP_HIT_POINTS + DrownedTuning.GRIP_DAMAGE_POINTS * damage;
		if (weapon != null && !weapon.isEmpty()) {
			if (weapon.is(ItemTags.AXES)) {
				points *= DrownedTuning.GRIP_AXE_MULTIPLIER;
			}
			var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			Holder<Enchantment> impaling = enchantments.getOrThrow(Enchantments.IMPALING);
			Holder<Enchantment> knockback = enchantments.getOrThrow(Enchantments.KNOCKBACK);
			points += DrownedTuning.GRIP_IMPALING_POINTS_PER_LEVEL * EnchantmentHelper.getItemEnchantmentLevel(impaling, weapon);
			points += DrownedTuning.GRIP_KNOCKBACK_POINTS_PER_LEVEL * EnchantmentHelper.getItemEnchantmentLevel(knockback, weapon);
		}
		return points;
	}

	/** Lets go of its victim; with {@code recoil} it is pushed back and the victim nudged up. It then finds a new spot. */
	public void release(Drowned drowned, boolean recoil) {
		if (state == State.JOIN || state == State.HELP) {
			letGo(drowned, recoil);
			return;
		}
		LivingEntity held = victim;
		victim = null;
		grip = 0.0F;
		deepTarget = null;
		releaseHelpers(drowned, recoil);
		avoid = spot != null ? spot.floor() : drowned.blockPosition();
		enterIdle(drowned);
		if (held == null) {
			return;
		}
		if (held.getVehicle() == drowned) {
			held.stopRiding();
		}
		if (recoil) {
			Vec3 away = drowned.position().subtract(held.position()).multiply(1.0, 0.0, 1.0);
			away = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize().scale(DrownedTuning.RECOIL_SPEED);
			drowned.setDeltaMovement(away.x, 0.1, away.z);
			drowned.needsSync = true;
			held.setDeltaMovement(held.getDeltaMovement().add(0.0, DrownedTuning.RELEASE_LIFT, 0.0));
			held.needsSync = true;
			drowned.playSound(SoundEvents.DROWNED_HURT_WATER, 1.0F, 0.7F);
		}
	}

	private static void face(Drowned drowned, float yaw) {
		drowned.setYRot(yaw);
		drowned.setYBodyRot(yaw);
		drowned.setYHeadRot(yaw);
	}

	/** For tests and the client test: put it straight into lurking at {@code spot}. */
	public void lurkAt(Drowned drowned, LurkSpots.Spot spot) {
		this.spot = spot;
		state = State.LURK;
		drowned.getNavigation().stop();
		drowned.setNoGravity(true);
	}

	/** For the client test and a test world station: swim to {@code spot} and lurk there whether or not a player is near. */
	public void pinTo(Drowned drowned, LurkSpots.Spot spot) {
		this.spot = spot;
		pinned = true;
		state = State.TRAVEL;
	}
}

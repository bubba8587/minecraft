// dte:C1,B17,B18,B19
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedMind;
import io.github.bubba8587.bubbas_reworked_mobs.grip.Gripper;
import io.github.bubba8587.bubbas_reworked_mobs.meter.Meters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A giant spider of the old-growth taiga (specs/spiders.md "Giant spiders"). Vanilla's spider model and texture at
 * vanilla's {@code scale} attribute ({@link SpiderTuning#GIANT_SCALE}), so size and hitbox follow vanilla rules (B17).
 * <ul>
 * <li><b>Roams</b> the forest (vanilla stroll) and <b>spins</b> cobweb spans between nearby trunks as it goes, capped
 * by how many cobwebs are already around (so a forest fills in but isn't buried).</li>
 * <li><b>Snatches livestock</b>: a passive animal it reaches is grabbed (it rides the spider, the drowned's riding
 * hold) and hauled up into a web (one of its own spans, or a fresh one by a tree trunk), where it's stuck and the spider
 * feeds. Hits on it while it hauls loosen its grip like the drowned's (B18, B19); at 0 it drops the catch.</li>
 * <li><b>Livestock first</b>: it goes for an animal before a player, and a hunting giant turns from a player to an
 * animal nearby, so it can be baited or lured (unless that player just hurt it).</li>
 * <li><b>Little spiders</b>: hurt in a fight, it calls small, weak vanilla spiders, a few at a time, capped.</li>
 * </ul>
 */
public class GiantSpider extends Spider implements Gripper {
	public enum Task {
		ROAM, HAUL, CLIMB, FEED,
		/** Its catch is cocooned; it has backed off on its perch and waits. */
		WAIT
	}

	/** Marks the little spiders it calls, to count them. */
	public static final String SPIDERLING_TAG = "bubbas_reworked_mobs.spiderling";

	/** Its task and larder, synced so the client can pose a haul and draw its thread. */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_TASK =
			net.minecraft.network.syncher.SynchedEntityData.defineId(GiantSpider.class, net.minecraft.network.syncher.EntityDataSerializers.BYTE);
	private static final net.minecraft.network.syncher.EntityDataAccessor<java.util.Optional<BlockPos>> DATA_LARDER =
			net.minecraft.network.syncher.SynchedEntityData.defineId(GiantSpider.class, net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_BLOCK_POS);

	private Task task = Task.ROAM;
	private @Nullable LivingEntity catchHeld;
	private @Nullable BlockPos larder;
	/** The perch is under something (a branch, the canopy): it hangs upside down there. Else sideways on the trunk. */
	private boolean perchUnder;
	/** From the perch toward the trunk it clings to. */
	private Direction trunkSide = Direction.NORTH;
	private @Nullable SilkCocoon cocoon;
	private Vec3 backOff = Vec3.ZERO;
	private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_UNDER =
			net.minecraft.network.syncher.SynchedEntityData.defineId(GiantSpider.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
	private float grip;
	private int sentGrip = -1;
	private int taskTicks;
	private int spinCooldown;
	private int spiderlingCooldown;

	public GiantSpider(EntityType<? extends GiantSpider> type, Level level) {
		super(type, level);
		spinCooldown = SpiderTuning.SPIN_INTERVAL_MIN;
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_TASK, (byte) Task.ROAM.ordinal());
		builder.define(DATA_LARDER, java.util.Optional.empty());
		builder.define(DATA_UNDER, false);
	}

	/** The task as both sides see it. */
	public Task syncedTask() {
		return Task.values()[entityData.get(DATA_TASK)];
	}

	/** Climbing a trunk with a catch: posed rear first, facing down at it, thread from its abdomen to the web (owner). */
	public boolean haulingUp() {
		return syncedTask() == Task.CLIMB;
	}

	/** Up its tree with a catch (climbing, waiting by the cocoon, feeding): posed by us, not by Stormie's Spiders. */
	public boolean onPerch() {
		Task t = syncedTask();
		return t == Task.CLIMB || t == Task.WAIT || t == Task.FEED;
	}

	/** Its perch is an underside (it hangs upside down there), as both sides see it. */
	public boolean perchedUnder() {
		return entityData.get(DATA_UNDER);
	}

	public @Nullable SilkCocoon cocoon() {
		return cocoon;
	}

	/** For tests: skip the rest of the wait. */
	public void endWait() {
		if (task == Task.WAIT) {
			taskTicks = SpiderTuning.COCOON_WAIT_TICKS;
		}
	}

	public java.util.Optional<BlockPos> syncedLarder() {
		return entityData.get(DATA_LARDER);
	}

	private void setTask(Task t) {
		task = t;
		entityData.set(DATA_TASK, (byte) t.ordinal());
		entityData.set(DATA_LARDER, t == Task.ROAM ? java.util.Optional.empty() : java.util.Optional.ofNullable(larder));
		entityData.set(DATA_UNDER, t != Task.ROAM && perchUnder);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, SpiderTuning.GIANT_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, SpiderTuning.GIANT_ATTACK)
				.add(Attributes.MOVEMENT_SPEED, SpiderTuning.GIANT_SPEED)
				.add(Attributes.SCALE, SpiderTuning.GIANT_SCALE)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new BusyGoal());
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		// Livestock first (owner), then players in the dark like a vanilla spider.
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Animal.class, true, (t, l) -> snatchable(t)));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true,
				(t, l) -> getLightLevelDependentMagicValue() < 0.5F));
	}

	/** Holds the body still (no stroll, no melee steering) while it hauls, climbs or feeds. */
	private class BusyGoal extends Goal {
		BusyGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return task != Task.ROAM;
		}
	}

	@Override
	public String grabbedHintKey() {
		return "bubbas_reworked_mobs.spider.grabbed";
	}

	public Task task() {
		return task;
	}

	/** What it snatches: passive animals on the ground, not tamed, not riding or ridden. */
	public static boolean snatchable(LivingEntity e) {
		if (!(e instanceof Animal animal) || !e.isAlive() || e.isPassenger() || e.isVehicle() || e.isInvisible()) {
			return false;
		}
		return !(animal instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame());
	}

	// --- tick -------------------------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (spiderlingCooldown > 0) {
			spiderlingCooldown--;
		}
		switch (task) {
			case ROAM -> tickRoam(level);
			case HAUL -> tickHaul(level);
			case CLIMB -> tickClimb(level);
			case FEED, WAIT -> tickPerch(level);
		}
		sentGrip = Meters.sync(this, Meters.GRIP, catchHeld != null ? grip : 0.0F, sentGrip, SpiderTuning.GRIP_FULL);
	}

	private void tickRoam(ServerLevel level) {
		if (catchHeld == null && isVehicle()) {
			ejectPassengers(); // a catch left riding it by a reload
		}
		if ((tickCount + getId()) % 20 == 0 && getTarget() instanceof Player player) {
			lure(level, player);
		}
		if (getTarget() == null && --spinCooldown <= 0) {
			spinCooldown = SpiderTuning.SPIN_INTERVAL_MIN + random.nextInt(SpiderTuning.SPIN_INTERVAL_MAX - SpiderTuning.SPIN_INTERVAL_MIN + 1);
			spin(level);
		}
	}

	/** Livestock is a distraction: an animal near it beats the player it's after (unless that player just hurt it). */
	private void lure(ServerLevel level, Player player) {
		if (getLastHurtByMob() == player && tickCount - getLastHurtByMobTimestamp() < SpiderTuning.LURE_IGNORED_AFTER_HURT_TICKS) {
			return;
		}
		double playerDist = distanceToSqr(player);
		Animal best = null;
		double bestDist = SpiderTuning.LURE_RADIUS * SpiderTuning.LURE_RADIUS;
		for (Animal animal : level.getEntitiesOfClass(Animal.class, getBoundingBox().inflate(SpiderTuning.LURE_RADIUS), GiantSpider::snatchable)) {
			double d = distanceToSqr(animal);
			if (d < bestDist && d < playerDist) {
				bestDist = d;
				best = animal;
			}
		}
		if (best != null) {
			setTarget(best);
		}
	}

	// --- the snatch ---------------------------------------------------------------------------------------

	/** Reaching an animal it's after, it grabs instead of biting. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (task == Task.ROAM && target instanceof LivingEntity living && snatchable(living)) {
			snatch(level, living);
			return true;
		}
		return super.doHurtTarget(level, target);
	}

	/** Grabs {@code prey} and sets off for a larder web. Public for tests. */
	public boolean snatch(ServerLevel level, LivingEntity prey) {
		BlockPos site = findLarder(level);
		if (site == null || !prey.startRiding(this, true, true)) {
			return false;
		}
		catchHeld = prey;
		larder = site;
		grip = SpiderTuning.GRIP_START;
		setTask(Task.HAUL);
		taskTicks = 0;
		setTarget(null);
		swingForAttack(InteractionHand.MAIN_HAND);
		playSound(SoundEvents.SPIDER_AMBIENT, 1.2F, 0.6F);
		return true;
	}

	private void tickHaul(ServerLevel level) {
		if (!holding()) {
			return;
		}
		taskTicks++;
		Vec3 below = Vec3.atBottomCenterOf(larder);
		double dx = below.x - getX();
		double dz = below.z - getZ();
		if (dx * dx + dz * dz < Mth.square(getBbWidth() * 0.5 + 1.2)) {
			getNavigation().stop();
			setTask(Task.CLIMB);
			setNoGravity(true);
			return;
		}
		if (taskTicks % 20 == 1) {
			getNavigation().moveTo(larder.getX() + 0.5, getY(), larder.getZ() + 0.5, 1.0);
		}
		if (taskTicks > SpiderTuning.HAUL_GIVE_UP_TICKS) {
			letGo(false);
		}
	}

	/** Where its body ends up on the perch: under the overhang, top against it; on the trunk, a little below the spot. */
	private Vec3 perchPos() {
		Vec3 site = Vec3.atBottomCenterOf(larder);
		Vec3 toTrunk = new Vec3(trunkSide.getStepX(), 0.0, trunkSide.getStepZ());
		// Hug the trunk: its middle a little off the bark.
		Vec3 hug = site.add(toTrunk.scale(Math.max(0.0, 0.5 - getBbWidth() * 0.5 + 0.05)));
		return perchUnder ? new Vec3(hug.x, site.y + 1.0 - getBbHeight(), hug.z) : new Vec3(hug.x, site.y - 0.4, hug.z);
	}

	/** Faces the trunk (its belly to the bark when posed head down on the trunk). */
	private void faceTrunk() {
		float yaw = trunkSide.toYRot();
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	private void tickClimb(ServerLevel level) {
		if (!holding()) {
			setNoGravity(false);
			return;
		}
		Vec3 target = perchPos();
		double nx = getX() + Mth.clamp(target.x - getX(), -0.08, 0.08);
		double nz = getZ() + Mth.clamp(target.z - getZ(), -0.08, 0.08);
		setDeltaMovement(Vec3.ZERO);
		faceTrunk();
		resetFallDistance();
		if (getY() < target.y) {
			setPos(nx, Math.min(target.y, getY() + SpiderTuning.CLIMB_SPEED), nz);
			setClimbing(true);
			return;
		}
		setPos(target.x, target.y, target.z);
		// Up: it wraps its catch in silk where it hangs below it (owner: cocooned, not bitten to death), then backs off.
		LivingEntity prey = catchHeld;
		catchHeld = null;
		prey.stopRiding();
		double below = prey.getBbHeight() + SpiderTuning.COCOON_DROP_BELOW;
		prey.teleportTo(target.x, target.y - below, target.z);
		cocoon = SilkCocoon.wrap(level, prey, new Vec3(target.x, target.y, target.z));
		if (cocoon == null) {
			endTask();
			return;
		}
		backOff = backOffSpot(level, target);
		setTask(Task.WAIT);
		taskTicks = 0;
		playSound(SoundEvents.SPIDER_AMBIENT, 1.2F, 0.5F);
	}

	/** Where it waits: along the overhang away from the trunk, or up the trunk; where it is if there's no room. */
	private Vec3 backOffSpot(ServerLevel level, Vec3 at) {
		if (perchUnder) {
			BlockPos out = larder.relative(trunkSide.getOpposite());
			if (level.getBlockState(out).isAir() && !level.getBlockState(out.above()).isAir()) {
				return at.add(-trunkSide.getStepX() * 1.0, 0.0, -trunkSide.getStepZ() * 1.0);
			}
			return at;
		}
		BlockPos up = larder.above();
		if (level.getBlockState(up).isAir() && level.getBlockState(up.above()).isAir()) {
			return at.add(0.0, 1.5, 0.0);
		}
		return at;
	}

	/** On its perch by the cocoon: backed off while it waits, back at the cocoon to feed. */
	private void tickPerch(ServerLevel level) {
		taskTicks++;
		setDeltaMovement(Vec3.ZERO);
		setClimbing(true);
		resetFallDistance();
		faceTrunk();
		SilkCocoon c = cocoon;
		LivingEntity wrapped = c == null || c.isRemoved() ? null : c.victim();
		if (wrapped == null || larder == null) {
			// Cut free, or eaten: it comes down and roams again.
			endTask();
			return;
		}
		Vec3 home = perchPos();
		Vec3 goal = task == Task.WAIT ? backOff : home;
		Vec3 step = goal.subtract(position());
		double len = step.length();
		if (len > 1.0E-3) {
			Vec3 move = len > 0.08 ? step.scale(0.08 / len) : step;
			setPos(getX() + move.x, getY() + move.y, getZ() + move.z);
		}
		if (task == Task.WAIT) {
			if (taskTicks >= SpiderTuning.COCOON_WAIT_TICKS) {
				setTask(Task.FEED);
				taskTicks = 0;
				playSound(SoundEvents.SPIDER_AMBIENT, 1.2F, 0.7F);
			}
			return;
		}
		if (len < 0.2 && taskTicks % SpiderTuning.COCOON_FEED_BITE_TICKS == 0
				&& wrapped.hurtServer(level, damageSources().mobAttack(this), SpiderTuning.COCOON_FEED_BITE_DAMAGE)) {
			swingForAttack(InteractionHand.MAIN_HAND);
			playSound(SoundEvents.SPIDER_HURT, 0.8F, 0.5F);
		}
	}

	private boolean holding() {
		LivingEntity prey = catchHeld;
		if (prey == null || !prey.isAlive() || prey.getVehicle() != this || larder == null) {
			letGo(false);
			return false;
		}
		return true;
	}

	/** Drops whatever it carries; with {@code recoil}, the catch is pushed off. */
	public void letGo(boolean recoil) {
		LivingEntity prey = catchHeld;
		catchHeld = null;
		if (prey != null && prey.getVehicle() == this) {
			prey.stopRiding();
			if (recoil) {
				prey.setDeltaMovement(prey.getDeltaMovement().add(0.0, 0.3, 0.0));
			}
		}
		endTask();
	}

	private void endTask() {
		cocoon = null;
		perchUnder = false;
		larder = null;
		setTask(Task.ROAM);
		catchHeld = null;
		larder = null;
		taskTicks = 0;
		grip = 0.0F;
		setNoGravity(false);
	}

	// --- larders and spinning ------------------------------------------------------------------------------

	/**
	 * Where it takes a catch: a perch beside the nearest trunk. It prefers an underside (owner): the highest spot by
	 * the trunk with leaves or wood right above, where it hangs upside down with the cocoon below. Without one, a spot on
	 * the trunk 4-7 up, where it clings sideways. Sets {@link #perchUnder} and {@link #trunkSide}.
	 */
	private @Nullable BlockPos findLarder(ServerLevel level) {
		BlockPos trunk = nearestTrunk(level, (int) SpiderTuning.LARDER_SEARCH);
		if (trunk == null) {
			return null;
		}
		int height = 1;
		while (height < 40 && level.getBlockState(trunk.above(height)).is(BlockTags.LOGS)) {
			height++;
		}
		// Undersides, highest first, with room below for the cocoon.
		for (int h = height - 1; h >= SpiderTuning.LARDER_HEIGHT_MIN; h--) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos spot = trunk.above(h).relative(d);
				if (level.getBlockState(spot).isAir() && !level.getBlockState(spot.above()).isAir()
						&& level.getBlockState(spot.below()).isAir() && level.getBlockState(spot.below(2)).isAir()) {
					perchUnder = true;
					trunkSide = d.getOpposite();
					return spot;
				}
			}
		}
		int pick = SpiderTuning.LARDER_HEIGHT_MIN + random.nextInt(SpiderTuning.LARDER_HEIGHT_MAX - SpiderTuning.LARDER_HEIGHT_MIN + 1);
		for (int h = Math.min(pick, height - 1); h >= SpiderTuning.LARDER_HEIGHT_MIN - 1; h--) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos spot = trunk.above(h).relative(d);
				if (level.getBlockState(spot).isAir()) {
					perchUnder = false;
					trunkSide = d.getOpposite();
					return spot;
				}
			}
		}
		return null;
	}

	/** The base of the nearest tree trunk at least LARDER_HEIGHT_MIN tall within {@code range}. */
	private @Nullable BlockPos nearestTrunk(ServerLevel level, int range) {
		return level.findBlocksInBoxByManhattanDistance(blockPosition(), range, 3).filterState(state -> state.is(BlockTags.LOGS))
				.findFirst((pos, state) -> trunkBase(level, pos, SpiderTuning.LARDER_HEIGHT_MIN)).orElse(null);
	}

	/** Whether {@code pos} is the bottom log of a log column at least {@code minHeight} tall. */
	private static boolean trunkBase(ServerLevel level, BlockPos pos, int minHeight) {
		if (level.getBlockState(pos.below()).is(BlockTags.LOGS)) {
			return false;
		}
		int h = 1;
		while (h < minHeight && level.getBlockState(pos.above(h)).is(BlockTags.LOGS)) {
			h++;
		}
		return h >= minHeight;
	}

	/** Spins a web between two nearby trunks at canopy height. Returns its strand count, 0 if none (tests). */
	public int spin(ServerLevel level) {
		if (websAround(level) >= SpiderTuning.AREA_WEB_CAP) {
			return 0;
		}
		List<BlockPos> trunks = trunks(level);
		for (int i = 0; i < trunks.size(); i++) {
			for (int j = i + 1; j < trunks.size(); j++) {
				BlockPos a = trunks.get(i);
				BlockPos b = trunks.get(j);
				double dist = Math.sqrt(a.distSqr(new BlockPos(b.getX(), a.getY(), b.getZ())));
				if (dist < SpiderTuning.SPAN_MIN || dist > SpiderTuning.SPAN_MAX) {
					continue;
				}
				int h = SpiderTuning.LARDER_HEIGHT_MIN + random.nextInt(SpiderTuning.LARDER_HEIGHT_MAX - SpiderTuning.LARDER_HEIGHT_MIN + 1);
				int y = Math.max(a.getY(), b.getY()) + h;
				if (!level.getBlockState(new BlockPos(a.getX(), y, a.getZ())).is(BlockTags.LOGS)
						|| !level.getBlockState(new BlockPos(b.getX(), y, b.getZ())).is(BlockTags.LOGS)) {
					continue;
				}
				// A tangle between the two trunks: thick strands from points on both trunks, the canopy and the ground
				// criss-crossing through the gap (owner), seeded from the site.
				Vec3 ca = new Vec3(a.getX() + 0.5, y + 0.5, a.getZ() + 0.5);
				Vec3 cb = new Vec3(b.getX() + 0.5, y + 0.5, b.getZ() + 0.5);
				Vec3 along = cb.subtract(ca).normalize();
				Vec3 hub = ca.lerp(cb, 0.5);
				List<Vec3> extra = new ArrayList<>();
				for (int k = -2; k <= 3; k++) {
					extra.add(ca.add(along.scale(0.5)).add(0, k * 1.1 + random.nextDouble() * 0.4, 0));
					extra.add(cb.subtract(along.scale(0.5)).add(0, k * 1.1 + random.nextDouble() * 0.4, 0));
				}
				SpunWeb web = SpunWeb.spin(level, hub, BlockPos.containing(hub).asLong(), SpiderTuning.TREE_WEB_RAYS, 0.2,
						SpiderTuning.TREE_WEB_REACH, extra, SpiderTuning.TREE_WEB_STRANDS, 0.0F, SpiderTuning.TREE_WEB_TIE_OFFS);
				if (web != null) {
					playSound(SoundEvents.SPIDER_STEP, 0.6F, 0.5F);
					return web.strands().size();
				}
			}
		}
		return 0;
	}

	/** Bases of tree trunks tall enough to string a web between, nearest first. */
	private List<BlockPos> trunks(ServerLevel level) {
		List<BlockPos> out = new ArrayList<>();
		level.findBlocksInBoxByManhattanDistance(blockPosition(), SpiderTuning.SPIN_SEARCH, 3).filterState(state -> state.is(BlockTags.LOGS))
				.forEach((pos, state) -> {
					if (trunkBase(level, pos, SpiderTuning.LARDER_HEIGHT_MAX + 1)) {
						out.add(pos.immutable());
					}
				});
		return out;
	}

	/** Webs already round here (spun webs near it): the cap that keeps a forest from being buried. */
	private int websAround(ServerLevel level) {
		double r = SpiderTuning.AREA_RADIUS;
		return level.getEntitiesOfClass(SpunWeb.class, getBoundingBox().inflate(r, 12.0, r)).size();
	}

	// --- hurt: grip and little spiders --------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (!hurt || !isAlive()) {
			return hurt;
		}
		if ((task == Task.HAUL || task == Task.CLIMB) && source.getEntity() != null) {
			grip -= DrownedMind.gripPoints(level, source.getWeaponItem(), damage) * SpiderTuning.GRIP_HIT_FACTOR;
			if (grip <= 0.0F) {
				letGo(true);
			}
		}
		if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			callSpiderlings(level, attacker);
		}
		return hurt;
	}

	/** Calls little spiders on {@code attacker}: a few at a time, capped, then a cooldown. Returns how many came. */
	public int callSpiderlings(ServerLevel level, LivingEntity attacker) {
		if (spiderlingCooldown > 0) {
			return 0;
		}
		int alive = level.getEntitiesOfClass(Spider.class, getBoundingBox().inflate(16.0), s -> s.entityTags().contains(SPIDERLING_TAG)).size();
		int count = Math.min(SpiderTuning.SPIDERLINGS_PER_CALL, SpiderTuning.SPIDERLINGS_CAP - alive);
		int made = 0;
		for (int i = 0; i < count; i++) {
			Spider small = EntityTypes.SPIDER.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (small == null) {
				break;
			}
			small.getAttribute(Attributes.SCALE).setBaseValue(SpiderTuning.SPIDERLING_SCALE);
			small.getAttribute(Attributes.MAX_HEALTH).setBaseValue(SpiderTuning.SPIDERLING_HEALTH);
			small.setHealth((float) SpiderTuning.SPIDERLING_HEALTH);
			small.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(SpiderTuning.SPIDERLING_ATTACK);
			small.addTag(SPIDERLING_TAG);
			double a = random.nextDouble() * Math.PI * 2.0;
			small.snapTo(getX() + Math.cos(a) * 1.5, getY(), getZ() + Math.sin(a) * 1.5, random.nextFloat() * 360.0F, 0.0F);
			small.setTarget(attacker);
			level.addFreshEntity(small);
			made++;
		}
		if (made > 0) {
			spiderlingCooldown = SpiderTuning.SPIDERLING_COOLDOWN_TICKS;
			playSound(SoundEvents.SPIDER_AMBIENT, 1.5F, 0.5F);
		}
		return made;
	}

	/** Never despawns while it has a catch, a climb, or a cocoon to feed on (it would leave a cocoon behind). */
	@Override
	public boolean requiresCustomPersistence() {
		return super.requiresCustomPersistence() || task != Task.ROAM;
	}

	@Override
	public void die(DamageSource source) {
		letGo(false);
		super.die(source);
	}

	/** It comes down out of the trees on silk: no fall damage. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	/** The catch rides its back on the ground; while it climbs rear first it hangs below its head. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		if (haulingUp()) {
			return new Vec3(0.0, dimensions.height() * 0.5 - dimensions.width() * 0.5 - passenger.getBbHeight(), 0.0);
		}
		return new Vec3(0.0, dimensions.height() * 0.15, 0.0);
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return passenger.position();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		// tasks aren't saved: it reloads roaming, on the ground
		setNoGravity(false);
	}

	/** Spawn rule: monster rules, and no other giant within GIANT_SPACING (how many roam a forest). */
	public static boolean checkGiantSpawnRules(EntityType<GiantSpider> type, net.minecraft.world.level.ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		double r = SpiderTuning.GIANT_SPACING;
		return level.getEntitiesOfClass(GiantSpider.class, new AABB(pos).inflate(r, 64.0, r)).isEmpty();
	}
}

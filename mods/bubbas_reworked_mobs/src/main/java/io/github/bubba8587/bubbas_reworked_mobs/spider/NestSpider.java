// dte:C1,B18,B19
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedMind;
import io.github.bubba8587.bubbas_reworked_mobs.grip.Gripper;
import io.github.bubba8587.bubbas_reworked_mobs.meter.Meters;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import win.demistorm.stormiespiders.common.entity.mob.Orientation;

/**
 * A spider of a cave nest (specs/spiders.md "Cave nests"). It sleeps hanging upside down in the cobwebs, like the
 * sleeping undead: no AI, no gravity, persistent, so it costs next to nothing and never counts against the mob cap.
 * Its {@link SpiderNest} drops it on a player walking underneath: it falls head first, straight down, on a thread.
 * <ul>
 * <li>A miss (it reaches the ground, or the target moved): the thread breaks and it turns into a vanilla spider.</li>
 * <li>A hit: it bites and latches on, and reels its victim up the thread into the nest, where it keeps biting. The
 * hold is the drowned grab's riding hold, pulling up instead of down: the victim rides it, hanging below. Its grip
 * works like the drowned's (B19, visible with the show_meters game rule): starts at 50, tightens while it holds,
 * hits loosen it by {@link DrownedMind#gripPoints} (anyone's hit counts, B18); at 0 it lets go and becomes a
 * vanilla spider. Let go mid-reel and the victim falls; in the nest the cobwebs catch them.</li>
 * </ul>
 */
public class NestSpider extends Spider implements Gripper {
	/** A new sleeper hung from {@code anchor} facing {@code yaw}, not yet added to the level. */
	public static @Nullable NestSpider hanging(Level level, Vec3 anchor, float yaw) {
		NestSpider spider = Spiders.NEST_SPIDER.create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
		if (spider != null) {
			spider.setYRot(yaw);
			spider.setYBodyRot(yaw);
			spider.setYHeadRot(yaw);
			spider.hangFrom(anchor);
		}
		return spider;
	}

	public enum Phase {
		SLEEPING, DROPPING, REELING, DANGLING,
		/** Its victim is cocooned below; it has backed off into the nest and waits. */
		WAITING,
		/** Back down at the cocoon, feeding. */
		FEEDING
	}

	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(NestSpider.class, EntityDataSerializers.BYTE);
	/** Where its thread is tied: the nest ceiling right above where it slept. */
	private static final EntityDataAccessor<Vector3fc> DATA_ANCHOR = SynchedEntityData.defineId(NestSpider.class, EntityDataSerializers.VECTOR3);

	private @Nullable LivingEntity target;
	private @Nullable LivingEntity victim;
	private float grip;
	private int dangle;
	private @Nullable SilkCocoon cocoon;
	private int waitTicks;
	private int sentGrip = -1;

	public NestSpider(EntityType<? extends NestSpider> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PHASE, (byte) Phase.SLEEPING.ordinal());
		builder.define(DATA_ANCHOR, new Vector3f());
	}

	public Phase phase() {
		return Phase.values()[entityData.get(DATA_PHASE)];
	}

	private void setPhase(Phase phase) {
		entityData.set(DATA_PHASE, (byte) phase.ordinal());
	}

	public Vec3 anchor() {
		Vector3fc a = entityData.get(DATA_ANCHOR);
		return new Vec3(a.x(), a.y(), a.z());
	}

	public @Nullable LivingEntity victim() {
		return victim;
	}

	public float grip() {
		return grip;
	}

	/** For tests: set the grip while it holds. */
	public void setGrip(float value) {
		grip = Math.clamp(value, 0.0F, SpiderTuning.GRIP_FULL);
	}

	/** Puts it to sleep hanging from the ceiling at {@code anchor} (the ceiling block's underside). */
	public void hangFrom(Vec3 anchor) {
		entityData.set(DATA_ANCHOR, new Vector3f((float) anchor.x, (float) anchor.y, (float) anchor.z));
		setPos(anchor.x, anchor.y - getBbHeight(), anchor.z);
		sleep();
	}

	private void sleep() {
		setPhase(Phase.SLEEPING);
		setNoAi(true);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
	}

	public boolean sleeping() {
		return phase() == Phase.SLEEPING;
	}

	@Override
	public String grabbedHintKey() {
		return "bubbas_reworked_mobs.spider.grabbed";
	}

	/** What it can latch onto: players (not spectators or creative) and passive animals, not riding or ridden (B18). */
	public static boolean grabbable(LivingEntity entity) {
		if (!entity.isAlive() || entity.isInvisible() || entity.isPassenger() || entity.isVehicle()) {
			return false;
		}
		if (entity instanceof Player player) {
			return !player.isSpectator() && !player.isCreative();
		}
		return entity instanceof Animal;
	}

	// --- the drop -----------------------------------------------------------------------------------

	/** Lets go of the ceiling and falls on {@code who} (straight down from where it hangs: it has to actually hit). */
	public void drop(@Nullable LivingEntity who) {
		if (!sleeping()) {
			return;
		}
		target = who;
		setPhase(Phase.DROPPING);
		setNoAi(true);
		setNoGravity(true);
		playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 1.4F);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (phase() != Phase.SLEEPING) {
			// On its thread it isn't falling: nothing may carry over as fall damage when the thread snaps.
			resetFallDistance();
		}
		switch (phase()) {
			case SLEEPING -> {
				setDeltaMovement(Vec3.ZERO);
				if (isVehicle()) {
					ejectPassengers(); // a victim left riding it by a reload
				}
			}
			case DROPPING -> tickDrop(level);
			case REELING -> tickHold(level);
			case WAITING, FEEDING -> tickCocoon(level);
			case DANGLING -> {
				setDeltaMovement(Vec3.ZERO);
				if (--dangle <= 0) {
					breakThread(level);
				}
			}
		}
		sentGrip = Meters.sync(this, Meters.GRIP, victim != null ? grip : 0.0F, sentGrip, SpiderTuning.GRIP_FULL);
	}

	private void tickDrop(ServerLevel level) {
		// No AI means no physics of its own: it is moved here, with collision.
		setDeltaMovement(Vec3.ZERO);
		AABB box = getBoundingBox().expandTowards(0.0, -SpiderTuning.DROP_SPEED, 0.0);
		LivingEntity hit = null;
		if (target != null && target.isAlive() && target.getBoundingBox().intersects(box)) {
			hit = target;
		} else {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && grabbable(e))) {
				hit = e;
				break;
			}
		}
		if (hit != null) {
			land(level, hit);
			return;
		}
		// The bottom of the drop: just above the floor, or level with whoever it missed. A miss hangs there on its
		// thread for a moment before it snaps.
		boolean floorClose = !level.noCollision(this, getBoundingBox().move(0.0, -SpiderTuning.DROP_SPEED - SpiderTuning.DANGLE_ABOVE_FLOOR, 0.0));
		if (floorClose || (target != null && getY() < target.getY()) || getY() < level.getMinY()) {
			setPhase(Phase.DANGLING);
			dangle = SpiderTuning.MISS_DANGLE_TICKS;
			return;
		}
		move(MoverType.SELF, new Vec3(0.0, -SpiderTuning.DROP_SPEED, 0.0));
	}

	/** It landed on {@code who}: a bite, and it latches on if it can. */
	private void land(ServerLevel level, LivingEntity who) {
		who.hurtServer(level, damageSources().mobAttack(this), SpiderTuning.DROP_DAMAGE);
		swingForAttack(InteractionHand.MAIN_HAND);
		if (!grabbable(who) || !who.startRiding(this, true, true)) {
			breakThread(level);
			return;
		}
		victim = who;
		grip = SpiderTuning.GRIP_START;
		setPhase(Phase.REELING);
		noPhysics = true;
		playSound(SoundEvents.SPIDER_HURT, 1.0F, 0.6F);
	}

	/** A miss, a let-go or a lost victim: the thread snaps and it's an ordinary spider from now on. */
	private void breakThread(ServerLevel level) {
		LivingEntity held = victim;
		victim = null;
		if (held != null && held.getVehicle() == this) {
			held.stopRiding();
		}
		noPhysics = false;
		resetFallDistance();
		Meters.sync(this, Meters.GRIP, 0.0F, sentGrip, SpiderTuning.GRIP_FULL);
		playSound(SoundEvents.SPIDER_STEP, 1.0F, 1.6F);
		convertTo(EntityTypes.SPIDER, ConversionParams.single(this, true, true), spider -> {
			spider.setNoAi(false);
			spider.setNoGravity(false);
			spider.setPersistenceRequired();
			spider.resetFallDistance(); // only a real fall after release hurts
			if (held instanceof Player player && grabbable(held)) {
				spider.setTarget(player);
			}
		});
	}

	// --- the hold -------------------------------------------------------------------------------------

	private void tickHold(ServerLevel level) {
		LivingEntity held = victim;
		if (held == null || !held.isAlive() || held.getVehicle() != this) {
			breakThread(level);
			return;
		}
		grip = Math.min(SpiderTuning.GRIP_FULL, grip + SpiderTuning.GRIP_RISE_PER_TICK);
		float tight = Math.clamp((grip - SpiderTuning.GRIP_START) / (SpiderTuning.GRIP_FULL - SpiderTuning.GRIP_START), 0.0F, 1.0F);
		if (phase() == Phase.REELING) {
			Vec3 anchor = anchor();
			double top = anchor.y - getBbHeight();
			if (getY() < top) {
				setDeltaMovement(0.0, 0.0, 0.0);
				setPos(anchor.x, Math.min(top, getY() + SpiderTuning.REEL_SPEED), anchor.z);
			} else {
				wrap(level, held);
				return;
			}
		}
		if (random.nextFloat() < Mth.lerp(tight, SpiderTuning.BITE_CHANCE_PER_TICK, SpiderTuning.BITE_CHANCE_PER_TICK_FULL)) {
			if (held.hurtServer(level, damageSources().mobAttack(this), Mth.lerp(tight, SpiderTuning.BITE_DAMAGE, SpiderTuning.BITE_DAMAGE_FULL))) {
				swingForAttack(InteractionHand.MAIN_HAND);
				playSound(SoundEvents.SPIDER_AMBIENT, 0.8F, 0.7F);
			}
		}
	}

	// --- the cocoon -----------------------------------------------------------------------

	public @Nullable SilkCocoon cocoon() {
		return cocoon;
	}

	/** Up in the nest: it wraps its victim where it hangs, then backs off and waits. */
	private void wrap(ServerLevel level, LivingEntity held) {
		victim = null;
		held.stopRiding();
		cocoon = SilkCocoon.wrap(level, held, anchor());
		noPhysics = false;
		if (cocoon == null) {
			breakThread(level);
			return;
		}
		waitTicks = SpiderTuning.COCOON_WAIT_TICKS;
		setPhase(Phase.WAITING);
		playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.5F);
	}

	private void tickCocoon(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		Vec3 anchor = anchor();
		setPos(anchor.x, anchor.y - getBbHeight(), anchor.z);
		SilkCocoon c = cocoon;
		LivingEntity wrapped = c == null || c.isRemoved() ? null : c.victim();
		if (wrapped == null) {
			cocoon = null;
			// Its meal got away (cut free) or is gone: an ordinary spider again if anyone's near, else back to sleep.
			Player near = level.getNearestPlayer(getX(), getY(), getZ(), 8.0, e -> e instanceof LivingEntity living && grabbable(living));
			if (near != null) {
				breakThread(level);
			} else {
				sleep();
			}
			return;
		}
		if (phase() == Phase.WAITING) {
			if (--waitTicks <= 0) {
				setPhase(Phase.FEEDING);
				waitTicks = 0;
				playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.7F);
			}
			return;
		}
		if (++waitTicks % SpiderTuning.COCOON_FEED_BITE_TICKS == 0
				&& wrapped.hurtServer(level, damageSources().mobAttack(this), SpiderTuning.COCOON_FEED_BITE_DAMAGE)) {
			swingForAttack(InteractionHand.MAIN_HAND);
			playSound(SoundEvents.SPIDER_HURT, 0.6F, 0.6F);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (!hurt || !isAlive()) {
			return hurt;
		}
		if (phase() == Phase.WAITING || phase() == Phase.FEEDING) {
			// Disturbed at its cocoon: it comes down and fights.
			cocoon = null;
			breakThread(level);
			return hurt;
		}
		if (sleeping()) {
			// Woken by a hit (an arrow from below): it drops on whoever shot it.
			drop(source.getEntity() instanceof LivingEntity attacker ? attacker : null);
		} else if (victim != null && source.getEntity() != null) {
			grip -= DrownedMind.gripPoints(level, source.getWeaponItem(), damage) * SpiderTuning.GRIP_HIT_FACTOR;
			if (grip <= 0.0F) {
				LivingEntity held = victim;
				breakThread(level);
				if (held != null) {
					Vec3 away = position().subtract(held.position()).multiply(1.0, 0.0, 1.0);
					held.setDeltaMovement(away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize().scale(-SpiderTuning.RECOIL_SPEED));
				}
			}
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		LivingEntity held = victim;
		victim = null;
		if (held != null && held.getVehicle() == this) {
			held.stopRiding();
		}
		super.die(source);
	}

	/** The victim hangs below it, head just under its mouth. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0.0, -passenger.getBbHeight() - 0.05, 0.0);
	}

	/** Let go: the victim stays where it hangs (and falls from there, or sticks in the nest's cobwebs). */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return passenger.position();
	}

	// --- Stormie's Spiders (required: spiders walk on walls and ceilings) ----------------------------------
	// Its climbing is mixed into vanilla's Spider, so this class gets it too. While it's a nest spider (asleep, dropping,
	// dangling, reeling, holding) it poses itself, so it reports upright with no attachment: Stormie's neither moves it
	// onto a surface nor rotates its model. Once it misses or lets go it becomes a vanilla spider and climbs like any.
	// These override methods Stormie's adds to Spider at runtime (same signatures), hence no @Override.

	private static final Orientation UPRIGHT = Orientation.fromNormal(new Vec3(0.0, 1.0, 0.0));

	public Orientation calculateOrientation(float partialTicks) {
		return UPRIGHT;
	}

	public Orientation getOrientation() {
		return UPRIGHT;
	}

	public float getAttachmentOffset(net.minecraft.core.Direction.Axis axis, float partialTicks) {
		return 0.0F;
	}

	public float getVerticalOffset(float partialTicks) {
		return 0.0F;
	}

	public boolean canAttachToSide(net.minecraft.core.Direction side) {
		return false;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false; // it comes down on a thread
	}

	@Override
	public boolean isPushable() {
		return !sleeping() && super.isPushable();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		Vector3fc a = entityData.get(DATA_ANCHOR);
		output.putFloat("NestAnchorX", a.x());
		output.putFloat("NestAnchorY", a.y());
		output.putFloat("NestAnchorZ", a.z());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_ANCHOR, new Vector3f(input.getFloatOr("NestAnchorX", (float) getX()),
				input.getFloatOr("NestAnchorY", (float) getY() + getBbHeight()), input.getFloatOr("NestAnchorZ", (float) getZ())));
		// A save mid-drop or mid-hold (the victim isn't kept): it wakes as an ordinary spider would, but stays a sleeper
		// otherwise. Simplest honest state: back to sleep where its thread is tied.
		sleep();
		setPos(anchor().x, anchor().y - getBbHeight(), anchor().z);
	}
}

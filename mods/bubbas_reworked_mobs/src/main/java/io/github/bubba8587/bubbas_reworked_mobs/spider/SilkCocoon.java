// dte:C1,B18
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import io.github.bubba8587.bubbas_reworked_mobs.grip.Gripper;
import io.github.bubba8587.bubbas_reworked_mobs.mixin.MobAmbientSoundInvoker;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A silk cocoon (specs/spiders.md "Cocoons", owner 2026-11-22): a spider's victim, wrapped and hanging from a thread
 * under the nest or the giant's perch. The victim rides it, so it can't move (B18: players and livestock alike).
 * <ul>
 * <li>Wrapping poisons it; vanilla Poison never takes the last half heart, so the cocoon alone never kills. The spider
 * that wrapped it comes back later to feed (its bites can kill).</li>
 * <li>Breaking free: every hit on the cocoon weakens the silk, from inside (the victim's own swings) or from outside
 * (anyone's). Swords and shears cut far faster than a fist ({@link SpiderTuning#COCOON_CUT_HAND} and friends); using
 * shears on it from outside cuts a big strip at once. At 0 the victim drops out alive.</li>
 * <li>Cocooned livestock cry out loudly and often.</li>
 * </ul>
 * Drawn on the client as pixel-art silk wound round the victim (client/spider/SilkCocoonRenderer).
 */
public class SilkCocoon extends Entity implements Gripper {
	/** Where its thread is tied (the nest ceiling, or the spider's perch). */
	private static final EntityDataAccessor<Vector3fc> DATA_ANCHOR = SynchedEntityData.defineId(SilkCocoon.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Float> DATA_SILK = SynchedEntityData.defineId(SilkCocoon.class, EntityDataSerializers.FLOAT);

	private int cryCooldown;

	public SilkCocoon(EntityType<? extends SilkCocoon> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ANCHOR, new Vector3f());
		builder.define(DATA_SILK, SpiderTuning.COCOON_SILK);
	}

	public Vec3 anchor() {
		Vector3fc a = entityData.get(DATA_ANCHOR);
		return new Vec3(a.x(), a.y(), a.z());
	}

	/** How much silk is left to cut: SpiderTuning.COCOON_SILK fresh, 0 free. */
	public float silk() {
		return entityData.get(DATA_SILK);
	}

	public @Nullable LivingEntity victim() {
		return getFirstPassenger() instanceof LivingEntity living ? living : null;
	}

	/**
	 * The cocoon's shape for a victim (owner, 2026-11-22): animals (four-legged, and birds) are slung lengthwise in a
	 * horizontal cocoon; upright bodies (players, villagers, zombies) hang head down in a vertical one. Anything wider
	 * than it is tall counts as an animal too. Worked out from the victim alone, so both sides agree.
	 */
	public static boolean horizontal(Entity victim) {
		return victim instanceof Animal || victim.getBbWidth() > victim.getBbHeight();
	}

	/** A horizontal cocoon's length along the animal's body (its box is square; its body is longer than wide). */
	public static double bodyLength(Entity victim) {
		return Math.max(victim.getBbWidth() * 1.6, victim.getBbHeight() * 0.9);
	}

	@Override
	public String grabbedHintKey() {
		return "bubbas_reworked_mobs.spider.cocooned";
	}

	/**
	 * Wraps {@code victim}: a cocoon at its feet, hanging from {@code anchor}, the victim riding it and poisoned. The
	 * victim must already be free of any vehicle. Returns the cocoon, or null.
	 */
	public static @Nullable SilkCocoon wrap(ServerLevel level, LivingEntity victim, Vec3 anchor) {
		SilkCocoon cocoon = Spiders.SILK_COCOON.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (cocoon == null) {
			return null;
		}
		cocoon.setPos(victim.getX(), victim.getY(), victim.getZ());
		cocoon.entityData.set(DATA_ANCHOR, new Vector3f((float) anchor.x, (float) anchor.y, (float) anchor.z));
		level.addFreshEntity(cocoon);
		if (!victim.startRiding(cocoon, true, true)) {
			cocoon.discard();
			return null;
		}
		poison(victim);
		level.playSound(null, cocoon.getX(), cocoon.getY(), cocoon.getZ(), SoundEvents.WOOL.placeSound().orElseThrow().value(), SoundSource.HOSTILE, 1.0F, 0.6F);
		return cocoon;
	}

	private static void poison(LivingEntity victim) {
		MobEffectInstance current = victim.getEffect(MobEffects.POISON);
		if (current == null || current.getDuration() < 40) {
			victim.addEffect(new MobEffectInstance(MobEffects.POISON, SpiderTuning.COCOON_POISON_TICKS, 0));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		setDeltaMovement(Vec3.ZERO);
		LivingEntity victim = victim();
		if (victim == null || !victim.isAlive()) {
			discard();
			return;
		}
		if (tickCount % 20 == 0) {
			poison(victim);
		}
		if (victim instanceof Animal animal && --cryCooldown <= 0) {
			cryCooldown = SpiderTuning.COCOON_CRY_MIN + random.nextInt(SpiderTuning.COCOON_CRY_MAX - SpiderTuning.COCOON_CRY_MIN + 1);
			SoundEvent cry = ((MobAmbientSoundInvoker) animal).bubbas_reworked_mobs$ambientSound();
			if (cry != null) {
				level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), cry, SoundSource.NEUTRAL, SpiderTuning.COCOON_CRY_VOLUME,
						0.9F + random.nextFloat() * 0.3F);
			}
		}
	}

	// --- cutting -------------------------------------------------------------------------------------------

	/** Silk one hit cuts, by what it's made with. */
	public static float cutPoints(@Nullable ItemStack weapon) {
		if (weapon == null || weapon.isEmpty()) {
			return SpiderTuning.COCOON_CUT_HAND;
		}
		if (weapon.is(Items.SHEARS)) {
			return SpiderTuning.COCOON_CUT_SHEARS;
		}
		if (weapon.is(ItemTags.SWORDS)) {
			return SpiderTuning.COCOON_CUT_SWORD;
		}
		if (weapon.is(ItemTags.AXES)) {
			return SpiderTuning.COCOON_CUT_AXE;
		}
		return SpiderTuning.COCOON_CUT_OTHER;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (isRemoved() || source.getEntity() instanceof net.minecraft.world.entity.monster.spider.Spider) {
			return false;
		}
		if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
			release(level);
			return true;
		}
		ItemStack weapon = source.getWeaponItem();
		if (weapon == null && source.getEntity() instanceof LivingEntity attacker) {
			weapon = attacker.getMainHandItem();
		}
		cut(level, cutPoints(weapon));
		return true;
	}

	/** Cuts {@code points} of silk; at 0 the victim is free. */
	public void cut(ServerLevel level, float points) {
		float left = silk() - points;
		entityData.set(DATA_SILK, Math.max(0.0F, left));
		level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.WOOL.breakSound().orElseThrow().value(), SoundSource.BLOCKS, 0.8F, 1.4F + random.nextFloat() * 0.3F);
		if (left <= 0.0F) {
			release(level);
		}
	}

	/** The silk gives: the victim drops out, alive. */
	public void release(ServerLevel level) {
		LivingEntity victim = victim();
		ejectPassengers();
		if (victim != null) {
			victim.removeEffect(MobEffects.POISON);
			victim.setDeltaMovement(0.0, 0.1, 0.0);
			victim.resetFallDistance();
		}
		level.playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 0.8F);
		discard();
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.SHEARS)) {
			return InteractionResult.PASS;
		}
		if (level() instanceof ServerLevel level) {
			held.hurtAndBreak(1, player, hand);
			cut(level, SpiderTuning.COCOON_SHEAR_USE);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isAttackable() {
		return true;
	}

	/** Pickable and hittable over the whole wrapped victim. */
	@Override
	protected AABB makeBoundingBox(Vec3 pos) {
		Entity victim = getFirstPassenger();
		if (victim == null) {
			return super.makeBoundingBox(pos);
		}
		double w = (horizontal(victim) ? bodyLength(victim) : victim.getBbWidth()) * 0.5 + 0.12;
		return new AABB(pos.x - w, pos.y - 0.05, pos.z - w, pos.x + w, pos.y + victim.getBbHeight() + 0.1, pos.z + w);
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		setBoundingBox(makeBoundingBox(position()));
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return Vec3.ZERO;
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return passenger.position();
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	public boolean shouldBeSaved() {
		// Players aren't saved with their vehicle; a cocoon with a player in it is rebuilt by nothing, so it isn't kept.
		return getFirstPassenger() != null && !(getFirstPassenger() instanceof Player) && super.shouldBeSaved();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		Vector3fc a = entityData.get(DATA_ANCHOR);
		output.putFloat("AnchorX", a.x());
		output.putFloat("AnchorY", a.y());
		output.putFloat("AnchorZ", a.z());
		output.putFloat("Silk", silk());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(DATA_ANCHOR, new Vector3f(input.getFloatOr("AnchorX", (float) getX()), input.getFloatOr("AnchorY", (float) getY() + 2.0F),
				input.getFloatOr("AnchorZ", (float) getZ())));
		entityData.set(DATA_SILK, input.getFloatOr("Silk", SpiderTuning.COCOON_SILK));
	}

}

// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedAccess;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedLurkGoal;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedMind;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedTuning;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.VanillaOnlyGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every vanilla drowned becomes a lurker (specs/drowned.md), no new entity type. Stronger attributes; no trident
 * throwing, melee chasing, beach walks or targeting (the mind grabs instead); our goal holds movement while the mind
 * steers; a grabbed victim rides it, held in front of it (computed the same on client and server, so the victim's own
 * client places them exactly); upright or lying is vanilla's synced swimming flag.
 */
@Mixin(Drowned.class)
abstract class DrownedMixin extends Zombie implements DrownedAccess {
	@Unique
	private @Nullable DrownedMind brm$mind;
	@Unique
	private boolean brm$vanilla;
	@Unique
	private static final String BRM_VANILLA_KEY = "bubbas_reworked_mobs:vanilla";

	protected DrownedMixin(EntityType<? extends Zombie> type, Level level) {
		super(type, level);
	}

	@Override
	public DrownedMind brm$mind() {
		// Lazy: goals are registered inside Mob's constructor, before this class's fields are initialised.
		if (brm$mind == null) {
			brm$mind = new DrownedMind();
		}
		return brm$mind;
	}

	@Override
	public void brm$makeVanilla() {
		if (brm$vanilla) {
			return;
		}
		brm$vanilla = true;
		brm$mind().release((Drowned) (Object) this, false);
		// Vanilla's numbers (Zombie.createAttributes: health 20, armor 2, no knockback resistance, speed 0.23).
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
		getAttribute(Attributes.ARMOR).setBaseValue(2.0);
		getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.23F);
		setHealth(Math.min(getHealth(), getMaxHealth()));
	}

	@Override
	public boolean brm$isVanilla() {
		return brm$vanilla;
	}

	/** Spawners and trial spawners keep vanilla drowned (owner, 2026-11-16). Before vanilla's own setup, so it picks vanilla gear (tridents included). */
	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void brm$spawnerStaysVanilla(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty,
			net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.world.entity.@Nullable SpawnGroupData groupData,
			CallbackInfoReturnable<net.minecraft.world.entity.SpawnGroupData> cir) {
		if (net.minecraft.world.entity.EntitySpawnReason.isSpawner(reason)) {
			brm$makeVanilla();
		}
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (brm$vanilla) {
			output.putBoolean(BRM_VANILLA_KEY, true);
		}
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		// The saved attributes are already vanilla's; only the flag is needed.
		brm$vanilla = input.getBooleanOr(BRM_VANILLA_KEY, false);
	}

	@Inject(method = "createAttributes", at = @At("RETURN"))
	private static void brm$stronger(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		cir.getReturnValue()
				.add(Attributes.MAX_HEALTH, DrownedTuning.MAX_HEALTH)
				.add(Attributes.ARMOR, DrownedTuning.ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, DrownedTuning.KNOCKBACK_RESISTANCE)
				.add(Attributes.MOVEMENT_SPEED, DrownedTuning.MOVEMENT_SPEED);
	}

	@Inject(method = "addBehaviourGoals", at = @At("TAIL"))
	private void brm$lurkerGoals(CallbackInfo ci) {
		List<WrappedGoal> goals = new ArrayList<>(goalSelector.getAvailableGoals());
		for (WrappedGoal wrapped : goals) {
			Goal goal = wrapped.getGoal();
			// The trident goal, the melee chase, and the night walk up the beach (the turtle-egg goal, also a
			// MoveToBlockGoal, stays).
			if (goal instanceof RangedAttackGoal || goal instanceof ZombieAttackGoal
					|| goal instanceof MoveToBlockGoal && !(goal instanceof RemoveBlockGoal)) {
				goalSelector.removeGoal(goal);
				goalSelector.addGoal(wrapped.getPriority(), new VanillaOnlyGoal(goal, this::brm$isVanilla));
			}
		}
		// It doesn't hunt by target: it lurks and grabs (a vanilla drowned keeps vanilla's targeting).
		for (WrappedGoal wrapped : new ArrayList<>(targetSelector.getAvailableGoals())) {
			targetSelector.removeGoal(wrapped.getGoal());
			targetSelector.addGoal(wrapped.getPriority(), new VanillaOnlyGoal(wrapped.getGoal(), this::brm$isVanilla));
		}
		goalSelector.addGoal(0, new DrownedLurkGoal((Drowned) (Object) this));
	}

	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void brm$noTridents(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		if (!brm$vanilla && getMainHandItem().is(Items.TRIDENT)) {
			setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
	}

	@Inject(method = "wantsToSwim", at = @At("HEAD"), cancellable = true)
	private void brm$wantsToSwim(CallbackInfoReturnable<Boolean> cir) {
		if (!level().isClientSide() && !brm$vanilla && brm$mind().wantsToSwim()) {
			cir.setReturnValue(true);
		}
	}

	/** Upright or lying (the synced swimming flag, which the client draws): lying swimming and crawling, upright lurking and dragging in water. */
	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void brm$posture(CallbackInfo ci) {
		if (!level().isClientSide() && !brm$vanilla) {
			setSwimming(brm$mind().lying((Drowned) (Object) this));
			ci.cancel();
		}
	}

	@Override
	public void tick() {
		if (!level().isClientSide() && isAlive() && !brm$vanilla) {
			brm$mind().tick((Drowned) (Object) this);
		}
		super.tick();
	}

	/** Nothing riding a drowned steers it (a grabbed villager would otherwise take over its movement goals). */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	/** The victim is held in front of it, feet level with its own (both sides compute this, so it needs no syncing). */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		double up = passenger.getVehicleAttachmentPoint(this).y + DrownedTuning.HOLD_UP;
		return new Vec3(0.0, up, DrownedTuning.HOLD_FORWARD).yRot(-getYRot() * ((float) Math.PI / 180.0F));
	}

	/** Let go where they are (not on top of its head). */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return passenger.position();
	}
}

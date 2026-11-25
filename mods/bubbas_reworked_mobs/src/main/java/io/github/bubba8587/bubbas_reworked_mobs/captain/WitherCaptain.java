// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** The Wither Captain (specs/wither-captain.md): a wither skeleton with a summoning phase and a boss bar. */
public class WitherCaptain extends WitherSkeleton {
	private static final EntityDataAccessor<Boolean> DATA_SUMMONING = SynchedEntityData.defineId(WitherCaptain.class, EntityDataSerializers.BOOLEAN);
	private static final String ENGAGED_KEY = "bubbas_reworked_mobs:engaged";
	private static final String HITS_KEY = "bubbas_reworked_mobs:hits";

	/** Client hook: runs each client tick while summoning (soul fire particles; set by the client initializer). */
	public static Consumer<WitherCaptain> clientSummoningTick = captain -> {
	};

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
			BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
	private boolean engaged;
	private int hitsSinceSummon;
	private int phaseTicks;
	private int blazesToSummon;
	private int blazesSummoned;
	private int lastSummonTick;
	private float summonBlend;
	private float summonBlendO;

	public WitherCaptain(EntityType<? extends WitherCaptain> type, Level level) {
		super(type, level);
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return AbstractSkeleton.createAttributes()
				.add(Attributes.MAX_HEALTH, CaptainTuning.CAPTAIN_HEALTH)
				.add(Attributes.ATTACK_DAMAGE, CaptainTuning.CAPTAIN_ATTACK_DAMAGE)
				.add(Attributes.SCALE, CaptainTuning.CAPTAIN_SCALE)
				.add(Attributes.KNOCKBACK_RESISTANCE, CaptainTuning.CAPTAIN_KNOCKBACK_RESISTANCE)
				.add(Attributes.FOLLOW_RANGE, CaptainTuning.CAPTAIN_FOLLOW_RANGE);
	}

	// --- state -------------------------------------------------------------------------------------------

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_SUMMONING, false);
	}

	public boolean isSummoning() {
		return this.entityData.get(DATA_SUMMONING);
	}

	/** Client: the summoning pose blend, 0 (normal) to 1 (summoning). */
	public float summonBlend(float partialTicks) {
		return Mth.lerp(partialTicks, this.summonBlendO, this.summonBlend);
	}

	public boolean hasEngaged() {
		return this.engaged;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean(ENGAGED_KEY, this.engaged);
		output.putInt(HITS_KEY, this.hitsSinceSummon);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.engaged = input.getBooleanOr(ENGAGED_KEY, false);
		this.hitsSinceSummon = input.getIntOr(HITS_KEY, 0);
		if (this.hasCustomName()) {
			this.bossEvent.setName(this.getDisplayName());
		}
	}

	@Override
	public void setCustomName(net.minecraft.network.chat.@Nullable Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	// --- gear and spawning -------------------------------------------------------------------------------

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(0, new SummonGoal());
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		super.populateDefaultEquipmentSlots(random, difficulty);
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CAPTAINS_HELM));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(CaptainTuning.CAPTAIN_ATTACK_DAMAGE);
		this.setDropChance(EquipmentSlot.HEAD, 0.0F);
		return data;
	}

	// --- fighting ----------------------------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean summoning = this.isSummoning();
		boolean hurt = super.hurtServer(level, source, summoning ? damage * CaptainTuning.SUMMONING_DAMAGE_FACTOR : damage);
		if (hurt && !summoning && this.isAlive() && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			if (!this.engaged) {
				this.engage();
			} else if (++this.hitsSinceSummon >= CaptainTuning.HITS_TO_SUMMON) {
				this.startSummoning(level);
			}
		}
		return hurt;
	}

	private void engage() {
		this.engaged = true;
		this.setPersistenceRequired();
		if (this.level() instanceof ServerLevel level) {
			this.startSummoning(level);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!this.engaged && this.getTarget() instanceof Player player && this.canAttack(player)
				&& this.distanceToSqr(player) < CaptainTuning.ENGAGE_RANGE * CaptainTuning.ENGAGE_RANGE) {
			this.engage();
		}
		if (this.isSummoning()) {
			this.tickSummoning(level);
		}
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		if (this.tickCount % 10 == 0) {
			this.updateBossBarPlayers(level);
		}
	}

	/** Starts the summoning phase (no-op while in it). */
	public void startSummoning(ServerLevel level) {
		if (this.isSummoning()) {
			return;
		}
		this.entityData.set(DATA_SUMMONING, true);
		this.phaseTicks = 0;
		this.blazesSummoned = 0;
		this.lastSummonTick = 0;
		this.hitsSinceSummon = 0;
		this.blazesToSummon = Mth.nextInt(this.random, CaptainTuning.BLAZES_MIN, CaptainTuning.BLAZES_MAX);
		this.getNavigation().stop();
		this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.6F);
	}

	private void endSummoning() {
		this.entityData.set(DATA_SUMMONING, false);
		this.hitsSinceSummon = 0;
	}

	private void tickSummoning(ServerLevel level) {
		this.phaseTicks++;
		if (this.blazesSummoned < this.blazesToSummon) {
			if (this.phaseTicks >= CaptainTuning.FIRST_SUMMON_DELAY + this.blazesSummoned * CaptainTuning.SUMMON_INTERVAL) {
				if (this.liveBlazes(level).size() < CaptainTuning.MAX_LIVE_BLAZES) {
					this.summonBlaze(level);
				}
				this.blazesSummoned++;
				this.lastSummonTick = this.phaseTicks;
			}
		} else if (this.phaseTicks >= this.lastSummonTick + CaptainTuning.LINGER_AFTER_LAST) {
			this.endSummoning();
		}
	}

	/** Blazes it summoned this phase (tests). */
	public int blazesSummoned() {
		return this.blazesSummoned;
	}

	public int blazesToSummon() {
		return this.blazesToSummon;
	}

	private @Nullable SoulfireBlaze summonBlaze(ServerLevel level) {
		SoulfireBlaze blaze = Captains.SOULFIRE_BLAZE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (blaze == null) {
			return null;
		}
		boolean placed = false;
		for (int i = 0; i < 8 && !placed; i++) {
			float angle = this.random.nextFloat() * Mth.TWO_PI;
			double r = Mth.nextDouble(this.random, CaptainTuning.SUMMON_RADIUS_MIN, CaptainTuning.SUMMON_RADIUS_MAX);
			blaze.snapTo(this.getX() + Mth.cos(angle) * r, this.getY() + CaptainTuning.SUMMON_HEIGHT, this.getZ() + Mth.sin(angle) * r,
					this.random.nextFloat() * 360.0F, 0.0F);
			placed = level.noCollision(blaze);
		}
		if (!placed) {
			blaze.snapTo(this.getX(), this.getY() + this.getBbHeight() + 0.5, this.getZ(), this.getYRot(), 0.0F);
		}
		blaze.finalizeSpawn(level, level.getCurrentDifficultyAt(blaze.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
		blaze.linkTo(this);
		if (this.getTarget() != null) {
			blaze.setTarget(this.getTarget());
		}
		level.addFreshEntity(blaze);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, blaze.getX(), blaze.getY(0.5), blaze.getZ(), 30, 0.35, 0.6, 0.35, 0.04);
		level.sendParticles(ParticleTypes.SOUL, blaze.getX(), blaze.getY(0.5), blaze.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
		blaze.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
		return blaze;
	}

	/** Its summoned blazes still alive nearby. */
	public List<SoulfireBlaze> liveBlazes(ServerLevel level) {
		return level.getEntitiesOfClass(SoulfireBlaze.class, this.getBoundingBox().inflate(CaptainTuning.BLAZE_LINK_RANGE),
				b -> b.isAlive() && this.getUUID().equals(b.captain()));
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level) {
			for (SoulfireBlaze blaze : this.liveBlazes(level)) {
				blaze.burnOut(level);
			}
		}
		this.bossEvent.removeAllPlayers();
	}

	// --- boss bar ----------------------------------------------------------------------------------------

	private void updateBossBarPlayers(ServerLevel level) {
		double range = CaptainTuning.BOSS_BAR_RANGE * CaptainTuning.BOSS_BAR_RANGE;
		for (ServerPlayer player : List.copyOf(this.bossEvent.getPlayers())) {
			if (!this.isAlive() || player.level() != level || player.distanceToSqr(this) > range) {
				this.bossEvent.removePlayer(player);
			}
		}
		if (this.isAlive()) {
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator() && player.distanceToSqr(this) <= range) {
					this.bossEvent.addPlayer(player);
				}
			}
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		this.bossEvent.removeAllPlayers();
	}

	// --- client ------------------------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.summonBlendO = this.summonBlend;
			this.summonBlend = Mth.approach(this.summonBlend, this.isSummoning() ? 1.0F : 0.0F, 0.1F);
			if (this.isSummoning()) {
				clientSummoningTick.accept(this);
			}
		}
	}

	/** The summoning phase: stands still facing its target while it summons. */
	private class SummonGoal extends Goal {
		SummonGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return WitherCaptain.this.isSummoning();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			WitherCaptain.this.getNavigation().stop();
		}

		@Override
		public void tick() {
			WitherCaptain captain = WitherCaptain.this;
			captain.getNavigation().stop();
			captain.setDeltaMovement(0.0, captain.getDeltaMovement().y, 0.0);
			LivingEntity target = captain.getTarget();
			if (target != null) {
				captain.getLookControl().setLookAt(target, 30.0F, 30.0F);
			}
		}
	}

	/** Captains in this box (natural spawn spacing). */
	public static boolean anyNear(ServerLevel level, AABB box) {
		return !level.getEntitiesOfClass(WitherCaptain.class, box, LivingEntity::isAlive).isEmpty();
	}
}

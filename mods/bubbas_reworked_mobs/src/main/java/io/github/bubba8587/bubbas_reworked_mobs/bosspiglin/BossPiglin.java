// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin;

import java.util.EnumSet;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSite;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Boss Piglin (specs/boss-piglin.md): sits on its outpost's throne while its roster lives, then flees for the
 * portal room and escapes through a lit portal. Goal-driven on an AbstractPiglin; its brain is only a memory store.
 */
public class BossPiglin extends AbstractPiglin {
	private static final EntityDataAccessor<Boolean> DATA_SEATED = SynchedEntityData.defineId(BossPiglin.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_FLEEING = SynchedEntityData.defineId(BossPiglin.class, EntityDataSerializers.BOOLEAN);
	/** Running away right now (from a player, or for the portal), as opposed to just being in fleeing mode. */
	private static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(BossPiglin.class, EntityDataSerializers.BOOLEAN);
	private static final String SITE_KEY = "bubbas_reworked_mobs:outpost";
	private static final String SEAT_KEY = "bubbas_reworked_mobs:seat";
	private static final String SEAT_YAW_KEY = "bubbas_reworked_mobs:seat_yaw";
	private static final String PORTAL_KEY = "bubbas_reworked_mobs:portal";
	private static final String SEATED_KEY = "bubbas_reworked_mobs:seated";
	private static final String FLEEING_KEY = "bubbas_reworked_mobs:fleeing";

	/** Memories vanilla piglin code reads or writes on any piglin near it (anger broadcasts, hunting checks). */
	private static final Brain.Provider<BossPiglin> BRAIN = brainProvider();

	@SuppressWarnings("deprecation")
	private static Brain.Provider<BossPiglin> brainProvider() {
		return Brain.provider(List.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ANGRY_AT, MemoryModuleType.UNIVERSAL_ANGER,
				MemoryModuleType.HUNTED_RECENTLY, MemoryModuleType.AVOID_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
				MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,
				MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEARBY_ADULT_PIGLINS, MemoryModuleType.LOOK_TARGET),
				List.of(), body -> List.of());
	}

	private @Nullable Long siteId;
	private @Nullable Vec3 seat;
	private float seatYaw;
	private @Nullable BlockPos portal;
	private int cornerTicks;
	private int slapCooldown;
	private @Nullable Goal avoidGoal;
	private @Nullable Goal escapeGoal;
	private float seatedBlend;
	private float seatedBlendO;
	private float fleeingBlend;
	private float fleeingBlendO;

	public BossPiglin(EntityType<? extends BossPiglin> type, Level level) {
		super(type, level);
		this.xpReward = BossPiglinTuning.XP;
		this.setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, BossPiglinTuning.HEALTH)
				.add(Attributes.MOVEMENT_SPEED, BossPiglinTuning.MOVEMENT_SPEED)
				.add(Attributes.ATTACK_DAMAGE, BossPiglinTuning.ATTACK_DAMAGE)
				.add(Attributes.ATTACK_KNOCKBACK, BossPiglinTuning.ATTACK_KNOCKBACK)
				.add(Attributes.KNOCKBACK_RESISTANCE, BossPiglinTuning.KNOCKBACK_RESISTANCE)
				.add(Attributes.FOLLOW_RANGE, BossPiglinTuning.FOLLOW_RANGE);
	}

	@Override
	protected Brain<BossPiglin> makeBrain(Brain.Packed packedBrain) {
		return BRAIN.makeBrain(this, packedBrain);
	}

	// --- state -------------------------------------------------------------------------------------------

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_SEATED, false);
		entityData.define(DATA_FLEEING, false);
		entityData.define(DATA_RUNNING, false);
	}

	public boolean isSeated() {
		return this.entityData.get(DATA_SEATED);
	}

	/** Fleeing, he plans paths over any drop: he can waddle off a cliff. */
	@Override
	public int getMaxFallDistance() {
		return this.isFleeing() ? BossPiglinTuning.FLEE_MAX_FALL : super.getMaxFallDistance();
	}

	public boolean isFleeing() {
		return this.entityData.get(DATA_FLEEING);
	}

	/** The outpost it belongs to (its camp site id), or null for one made by egg or command. */
	public @Nullable Long siteId() {
		return this.siteId;
	}

	/** Where it sits (feet), or null. */
	public @Nullable Vec3 seat() {
		return this.seat;
	}

	/** The portal cell it heads for, or null. */
	public @Nullable BlockPos portal() {
		return this.portal;
	}

	/** Ties it to an outpost: the camp site and its portal (either may be null). */
	public void link(@Nullable Long siteId, @Nullable BlockPos portal) {
		this.siteId = siteId;
		this.portal = portal == null ? null : portal.immutable();
		this.setPersistenceRequired();
	}

	/** Sits it down at {@code seat} facing {@code yaw}. */
	public void seatAt(Vec3 seat, float yaw) {
		this.seat = seat;
		this.seatYaw = yaw;
		this.entityData.set(DATA_SEATED, true);
		this.entityData.set(DATA_FLEEING, false);
		this.cornerTicks = 0;
		this.getNavigation().stop();
		this.holdSeat();
	}

	/** Gets up off the throne (a step forward when there's room) and starts to flee. */
	public void standUp() {
		boolean was = this.isSeated();
		this.entityData.set(DATA_SEATED, false);
		this.entityData.set(DATA_FLEEING, true);
		this.setNoGravity(false);
		if (was && this.seat != null) {
			Direction facing = Direction.fromYRot(this.seatYaw);
			for (double step : BossPiglinTuning.STAND_STEPS) {
				Vec3 front = new Vec3(this.seat.x + facing.getStepX() * step, Mth.floor(this.seat.y), this.seat.z + facing.getStepZ() * step);
				if (this.level().noCollision(this, this.getDimensions(this.getPose()).makeBoundingBox(front))) {
					this.snapTo(front.x, front.y, front.z, this.seatYaw, 0.0F);
					break;
				}
			}
			this.makeSound(SoundEvents.PIGLIN_RETREAT);
		}
	}

	private void holdSeat() {
		if (this.seat == null) {
			return;
		}
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);
		this.setPos(this.seat);
		this.setYRot(this.seatYaw);
		this.yBodyRot = this.seatYaw;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.siteId != null) {
			output.putLong(SITE_KEY, this.siteId);
		}
		output.storeNullable(SEAT_KEY, Vec3.CODEC, this.seat);
		output.putFloat(SEAT_YAW_KEY, this.seatYaw);
		output.storeNullable(PORTAL_KEY, BlockPos.CODEC, this.portal);
		output.putBoolean(SEATED_KEY, this.isSeated());
		output.putBoolean(FLEEING_KEY, this.isFleeing());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.siteId = input.getLong(SITE_KEY).orElse(null);
		this.seat = input.read(SEAT_KEY, Vec3.CODEC).orElse(null);
		this.seatYaw = input.getFloatOr(SEAT_YAW_KEY, 0.0F);
		this.portal = input.read(PORTAL_KEY, BlockPos.CODEC).orElse(null);
		this.entityData.set(DATA_SEATED, input.getBooleanOr(SEATED_KEY, false) && this.seat != null);
		this.entityData.set(DATA_FLEEING, input.getBooleanOr(FLEEING_KEY, false));
	}

	// --- gear --------------------------------------------------------------------------------------------

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		var materials = level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
		var patterns = level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
		for (Gear gear : Gear.values()) {
			ItemStack stack = new ItemStack(gear.item);
			stack.set(net.minecraft.core.component.DataComponents.TRIM,
					new ArmorTrim(materials.getOrThrow(gear.material), patterns.getOrThrow(gear.pattern)));
			this.setItemSlot(gear.slot, stack);
			this.setDropChance(gear.slot, 0.0F);
		}
		this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		return data;
	}

	/** Its trimmed gold armour (the loot table drops the same pieces: loot_table/entities/boss_piglin.json). */
	public enum Gear {
		HELMET(EquipmentSlot.HEAD, Items.GOLDEN_HELMET, TrimPatterns.SNOUT, TrimMaterials.NETHERITE),
		CHESTPLATE(EquipmentSlot.CHEST, Items.GOLDEN_CHESTPLATE, TrimPatterns.RIB, TrimMaterials.REDSTONE),
		LEGGINGS(EquipmentSlot.LEGS, Items.GOLDEN_LEGGINGS, TrimPatterns.EYE, TrimMaterials.NETHERITE),
		BOOTS(EquipmentSlot.FEET, Items.GOLDEN_BOOTS, TrimPatterns.RIB, TrimMaterials.GOLD);

		public final EquipmentSlot slot;
		public final Item item;
		public final ResourceKey<TrimPattern> pattern;
		public final ResourceKey<TrimMaterial> material;

		Gear(EquipmentSlot slot, Item item, ResourceKey<TrimPattern> pattern, ResourceKey<TrimMaterial> material) {
			this.slot = slot;
			this.item = item;
			this.pattern = pattern;
			this.material = material;
		}
	}

	// --- piglin --------------------------------------------------------------------------------------

	@Override
	protected boolean canHunt() {
		return false;
	}

	@Override
	public PiglinArmPose getArmPose() {
		return PiglinArmPose.DEFAULT;
	}

	@Override
	protected void playConvertedSound() {
		this.makeSound(SoundEvents.PIGLIN_CONVERTED_TO_ZOMBIFIED);
	}

	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	// --- sounds --------------------------------------------------------------------------------------

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (this.isSeated()) {
			return this.random.nextBoolean() ? SoundEvents.PIGLIN_ANGRY : SoundEvents.PIGLIN_CELEBRATE;
		}
		return this.isFleeing() ? SoundEvents.PIGLIN_RETREAT : SoundEvents.PIGLIN_AMBIENT;
	}

	@Override
	public void playAmbientSound() {
		this.makeSound(this.getAmbientSound());
	}

	@Override
	public int getAmbientSoundInterval() {
		return BossPiglinTuning.SHOUT_INTERVAL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PIGLIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PIGLIN_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		this.playSound(SoundEvents.PIGLIN_STEP, 0.2F, 0.8F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * BossPiglinTuning.VOICE_PITCH;
	}

	// --- seated: held in place -------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isSeated() && (source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL))) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && source.getEntity() instanceof Player player) {
			PiglinAi.angerNearbyPiglins(level, player, true);
		}
		return hurt;
	}

	@Override
	public void knockback(double power, double xd, double zd, DamageSource source, float damage, boolean comesFromEffect) {
		if (!this.isSeated()) {
			super.knockback(power, xd, zd, source, damage, comesFromEffect);
		}
	}

	@Override
	public boolean isPushable() {
		return !this.isSeated() && super.isPushable();
	}

	@Override
	protected void tickHeadTurn(float yBodyRotT) {
		if (this.isSeated()) {
			this.yBodyRot = this.getYRot();
		} else {
			super.tickHeadTurn(yBodyRotT);
		}
	}

	// --- AI ------------------------------------------------------------------------------------------

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SitGoal());
		this.goalSelector.addGoal(2, new SlapGoal());
		this.escapeGoal = new EscapeGoal();
		this.goalSelector.addGoal(3, this.escapeGoal);
		this.avoidGoal = new AvoidEntityGoal<>(this, Player.class, BossPiglinTuning.FLEE_DISTANCE, BossPiglinTuning.FLEE_WALK,
				BossPiglinTuning.FLEE_SPRINT) {
			@Override
			public boolean canUse() {
				return BossPiglin.this.isFleeing() && super.canUse();
			}
		};
		this.goalSelector.addGoal(4, this.avoidGoal);
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, BossPiglinTuning.HIDE_SPEED) {
			@Override
			public boolean canUse() {
				return !BossPiglin.this.isSeated() && super.canUse();
			}
		});
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.isSeated()) {
			this.holdSeat();
			if (this.siteId != null && this.tickCount % BossPiglinTuning.ROSTER_CHECK == 0 && !this.rosterLives(level)) {
				this.standUp();
			}
			return;
		}
		if (!this.isFleeing() && this.siteId == null) {
			this.entityData.set(DATA_FLEEING, true); // made by egg or command: no throne to keep
		}
		this.entityData.set(DATA_RUNNING, this.goalSelector.getAvailableGoals().stream()
				.anyMatch(g -> g.isRunning() && (g.getGoal() == this.avoidGoal || g.getGoal() == this.escapeGoal)));
		Player near = this.cornering(level);
		this.cornerTicks = near == null ? 0 : this.cornerTicks + 1;
		if (this.slapCooldown > 0) {
			this.slapCooldown--;
		}
		if (this.isFleeing() && this.inPortal()) {
			this.escape(level);
		}
	}

	/** Whether any of its outpost's roster is still alive. */
	private boolean rosterLives(ServerLevel level) {
		CampSite site = CampSites.get(level, this.siteId);
		return site != null && !site.members().isEmpty();
	}

	private @Nullable Player cornering(ServerLevel level) {
		Player player = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), BossPiglinTuning.CORNER_RANGE,
				EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(e -> e.isAlive()));
		return player != null && this.canAttack(player) ? player : null;
	}

	private boolean inPortal() {
		var box = this.getBoundingBox().inflate(BossPiglinTuning.PORTAL_REACH, 0.0, BossPiglinTuning.PORTAL_REACH);
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			if (this.level().getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
				return true;
			}
		}
		return false;
	}

	/** Through the portal and gone, loot and all. */
	public void escape(ServerLevel level) {
		level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 80, 0.5, 1.0, 0.5, 0.6);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.6F, 1.2F);
		this.discard();
	}

	/** Whether the portal it heads for is lit. */
	public boolean portalLit() {
		return this.portal != null && this.level().isLoaded(this.portal) && this.level().getBlockState(this.portal).is(Blocks.NETHER_PORTAL);
	}

	// --- client --------------------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.seatedBlendO = this.seatedBlend;
			this.fleeingBlendO = this.fleeingBlend;
			this.seatedBlend = Mth.approach(this.seatedBlend, this.isSeated() ? 1.0F : 0.0F, BossPiglinTuning.BLEND_STEP);
			this.fleeingBlend = Mth.approach(this.fleeingBlend, this.isFleeing() && this.entityData.get(DATA_RUNNING) ? 1.0F : 0.0F, BossPiglinTuning.BLEND_STEP);
		}
	}

	/** Client: how seated it looks, 0 to 1. */
	public float seatedBlend(float partialTicks) {
		return Mth.lerp(partialTicks, this.seatedBlendO, this.seatedBlend);
	}

	/** Client: how much it waddles like it's fleeing, 0 to 1. */
	public float fleeingBlend(float partialTicks) {
		return Mth.lerp(partialTicks, this.fleeingBlendO, this.fleeingBlend);
	}

	// --- goals ---------------------------------------------------------------------------------------

	/** On the throne: still, head following the nearest player. */
	private class SitGoal extends Goal {
		SitGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return BossPiglin.this.isSeated();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			BossPiglin.this.getNavigation().stop();
		}

		@Override
		public void tick() {
			BossPiglin boss = BossPiglin.this;
			boss.getNavigation().stop();
			Player player = boss.level().getNearestPlayer(boss.getX(), boss.getY(), boss.getZ(), BossPiglinTuning.LOOK_RANGE, EntitySelector.NO_SPECTATORS);
			if (player != null) {
				boss.getLookControl().setLookAt(player, 30.0F, 30.0F);
			}
		}
	}

	/** Cornered: a slap for the player who won't back off. */
	private class SlapGoal extends Goal {
		SlapGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			BossPiglin boss = BossPiglin.this;
			return boss.isFleeing() && boss.slapCooldown <= 0 && boss.cornerTicks >= BossPiglinTuning.CORNER_TICKS;
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			BossPiglin boss = BossPiglin.this;
			if (!(boss.level() instanceof ServerLevel level)) {
				return;
			}
			Player player = boss.cornering(level);
			boss.cornerTicks = 0;
			boss.slapCooldown = BossPiglinTuning.SLAP_COOLDOWN;
			if (player != null) {
				boss.getLookControl().setLookAt(player, 30.0F, 30.0F);
				boss.lookAt(player, 90.0F, 90.0F);
				boss.swingForAttack(InteractionHand.MAIN_HAND);
				boss.doHurtTarget(level, player);
			}
		}
	}

	/** Whether it last gave up on reaching the portal (no path that fits it, or no progress), and is just fleeing. */
	public boolean gaveUpOnPortal() {
		return this.portalGiveUpUntil > this.tickCount;
	}

	private int portalGiveUpUntil;

	/** To the outpost's portal while it's lit; given up for a while when no path fits it or it stops getting closer. */
	private class EscapeGoal extends Goal {
		private int repath;
		private double best;
		private int stalled;

		EscapeGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			BossPiglin boss = BossPiglin.this;
			return boss.isFleeing() && !boss.gaveUpOnPortal() && boss.portalLit();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void start() {
			this.repath = 0;
			this.stalled = 0;
			this.best = Double.MAX_VALUE;
		}

		@Override
		public void stop() {
			BossPiglin.this.getNavigation().stop();
		}

		private void giveUp() {
			BossPiglin boss = BossPiglin.this;
			boss.portalGiveUpUntil = boss.tickCount + BossPiglinTuning.PORTAL_GIVE_UP_TICKS;
			boss.getNavigation().stop();
			boss.makeSound(SoundEvents.PIGLIN_ANGRY);
		}

		@Override
		public void tick() {
			BossPiglin boss = BossPiglin.this;
			if (--this.repath > 0 && !boss.getNavigation().isDone()) {
				return;
			}
			this.repath = BossPiglinTuning.PORTAL_REPATH_TICKS;
			BlockPos target = boss.portal;
			double distance = boss.position().distanceTo(Vec3.atBottomCenterOf(target));
			if (distance < this.best - BossPiglinTuning.PORTAL_PROGRESS) {
				this.best = distance;
				this.stalled = 0;
			} else if ((this.stalled += BossPiglinTuning.PORTAL_REPATH_TICKS) >= BossPiglinTuning.PORTAL_STALL_TICKS) {
				this.giveUp();
				return;
			}
			var path = boss.getNavigation().createPath(target, BossPiglinTuning.PORTAL_PATH_ACCURACY);
			if (path == null || !path.canReach()) {
				this.giveUp(); // no way down that fits it (the tunnel is too narrow for him)
				return;
			}
			boss.getNavigation().moveTo(path, BossPiglinTuning.ESCAPE_SPEED);
		}
	}

	/** Every living boss tied to this outpost (loaded ones). */
	public static List<BossPiglin> of(ServerLevel level, CampSite site) {
		return level.getEntitiesOfClass(BossPiglin.class, net.minecraft.world.phys.AABB.of(site.box()).inflate(32.0),
				b -> b.isAlive() && Long.valueOf(site.id()).equals(b.siteId));
	}
}

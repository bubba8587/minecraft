// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import com.mojang.serialization.Codec;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ancient Fabricators at work (specs/mob-factory.md). A site runs while a player is within
 * {@link FabricatorTuning#ACTIVE_RANGE} of its pad. Each count: the dripper drops its mob onto the pad,
 * deactivated; the eight timer lamps light one by one ({@link FabricatorTuning#STEP_TICKS} each) while portal
 * particles over the pad thicken (they run with nothing on it too); with all eight lit, whatever stands on the pad
 * gets a little Levitation, then it fires: a deactivated mob goes to a dark surface spot above and around (and wakes
 * there), a player or any other living thing rides it up to the surface. Redstone into the pad's lodestone stops the
 * teleporter (powered = stopped, like a hopper): the lamps go dark and mobs pile up on the deck (up to
 * {@link FabricatorTuning#PILE_CAP}). Breaking the hidden spawner stops it all for good (mobs on the deck wake there).
 * The Warden stands passive (no AI) in its office window, turning to watch the pad, until it hears a player going for
 * the machine or a beacon; then it's a vanilla Warden that never digs away. Debuffs (Mining Fatigue III, Weakness,
 * Slowness): the paired beacon's Fabricator Heart with Bubba's Reworked Beacons; without it, while the floor beacon
 * stands, this class gives them itself.
 */
public final class Fabricators {
	public static final ResourceKey<PlacedFeature> FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, BubbasReworkedMobs.id("ancient_fabricator"));

	/** A Fabricator's Warden: true while passive. */
	public static final AttachmentType<Boolean> WARDEN = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("fabricator_warden"), Codec.BOOL);

	/** Tests: where riders and mobs go instead of a surface spot (null: the real rules). */
	public static @Nullable BiFunction<ServerLevel, FabricatorSite, BlockPos> destinationOverride;
	/** Tests: sites that run with no player near. */
	private static final Map<ServerLevel, java.util.Set<FabricatorSite>> FORCED = new WeakHashMap<>();
	private static final Map<ServerLevel, Map<FabricatorSite, Running>> RUNNING = new WeakHashMap<>();

	private Fabricators() {
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, BubbasReworkedMobs.id("ancient_fabricator"), FabricatorFeature.CODEC);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, FEATURE);
		ServerTickEvents.END_LEVEL_TICK.register(Fabricators::tick);
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world instanceof ServerLevel level) {
				heard(level, player, pos);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof Warden warden && Boolean.TRUE.equals(warden.getAttached(WARDEN)) && source.getEntity() instanceof Player player) {
				wakeWarden(warden, player);
			}
		});
	}

	/** Tests: run this site whether or not a player is near. */
	public static void force(ServerLevel level, FabricatorSite site, boolean on) {
		java.util.Set<FabricatorSite> set = FORCED.computeIfAbsent(level, l -> new java.util.HashSet<>());
		if (on) {
			set.add(site);
		} else {
			set.remove(site);
			Map<FabricatorSite, Running> running = RUNNING.get(level);
			if (running != null) {
				running.remove(site);
			}
		}
	}

	/** The running state of a site, if it runs (tests read the count). */
	public static @Nullable Running running(ServerLevel level, FabricatorSite site) {
		Map<FabricatorSite, Running> map = RUNNING.get(level);
		return map == null ? null : map.get(site);
	}

	private static void tick(ServerLevel level) {
		long time = level.getGameTime();
		Map<FabricatorSite, Running> running = RUNNING.computeIfAbsent(level, l -> new HashMap<>());
		if (time % 20 == 0) {
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator()) {
					continue;
				}
				for (FabricatorSite site : FabricatorSites.near(level, player.blockPosition(), FabricatorTuning.ACTIVE_RANGE + 16)) {
					if (site.pad().distSqr(player.blockPosition()) <= (double) FabricatorTuning.ACTIVE_RANGE * FabricatorTuning.ACTIVE_RANGE
							&& level.isLoaded(site.pad())) {
						running.computeIfAbsent(site, Running::new).seen = time;
					}
				}
			}
			for (FabricatorSite site : FORCED.getOrDefault(level, java.util.Set.of())) {
				Running r = running.computeIfAbsent(site, Running::new);
				r.seen = time;
				if (!r.forced) {
					r.forced = true;
					r.resting = false;
				}
			}
			running.entrySet().removeIf(e -> {
				if (time - e.getValue().seen > 40) {
					e.getValue().stop(level);
					return true;
				}
				return false;
			});
		}
		if (running.isEmpty()) {
			return;
		}
		for (Running r : List.copyOf(running.values())) {
			if (level.isLoaded(r.site.pad())) {
				r.tick(level);
			}
		}
	}

	/** One running site's count (not saved: a reload starts the count again). */
	public static final class Running {
		final FabricatorSite site;
		long seen;
		/** Lamps lit (0-8); at 8 the lift runs. */
		int step;
		int ticks;
		int lift = -1;
		boolean wasPowered;
		/** Between counts it rests (dark, nothing drops) until a chance wakes it; it starts resting, so arriving players see nothing at first. */
		boolean resting = true;
		/** Forced on (game tests): never rests, so a test sees a count at once. */
		boolean forced;

		Running(FabricatorSite site) {
			this.site = site;
		}

		public boolean resting() {
			return this.resting;
		}

		public int step() {
			return this.step;
		}

		public boolean lifting() {
			return this.lift >= 0;
		}

		void tick(ServerLevel level) {
			long time = level.getGameTime();
			if (!FabricatorSites.has(level, this.site, FabricatorSites.PREPARED)) {
				prepare(level, this.site);
			}
			if (time % 20 == 0) {
				watchWarden(level, this.site);
			}
			if (time % FabricatorTuning.DEBUFF_EVERY_TICKS == 0) {
				debuffs(level, this.site);
			}
			if (FabricatorSites.has(level, this.site, FabricatorSites.BROKEN)) {
				return;
			}
			if (time % 10 == 0 && level.isLoaded(this.site.spawner()) && !level.getBlockState(this.site.spawner()).is(Blocks.SPAWNER)) {
				breakFabricator(level, this.site);
				stop(level);
				return;
			}
			boolean powered = level.hasNeighborSignal(this.site.control());
			if (powered) {
				if (!this.wasPowered) {
					this.step = 0;
					this.ticks = 0;
					this.lift = -1;
					lamps(level, 0);
				}
				this.wasPowered = true;
				// Stopped: mobs still drop and pile up, at the same chance as a resting machine wakes.
				// (Forced on, in tests: once per count's length instead.)
				if (this.forced ? time % FabricatorTuning.COUNT_TICKS == 0 : time % 20 == 0 && level.getRandom().nextInt(FabricatorTuning.REST_ONE_IN) == 0) {
					tryDrop(level, this.site);
				}
				return;
			}
			this.wasPowered = false;
			if (this.resting) {
				// Resting (owner, 2026-11-16: not a constant stream, so it isn't obvious what it does): lamps dark, a
				// faint shimmer over the pad; each second a chance to wake (B19: a chance, no fixed cooldown).
				if (time % 20 == 0) {
					BlockPos pad = this.site.pad();
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, pad.getX() + 0.5, pad.getY() + 0.3, pad.getZ() + 0.5, 1, 0.9, 0.2, 0.9, 0.01);
					if (level.getRandom().nextInt(FabricatorTuning.REST_ONE_IN) == 0) {
						this.resting = false;
					}
				}
				if (this.resting) {
					return;
				}
			}
			if (this.step == 0 && this.ticks == 0 && this.lift < 0) {
				tryDrop(level, this.site);
			}
			particles(level);
			if (this.lift >= 0) {
				if (this.lift == 0) {
					for (LivingEntity e : riders(level, this.site)) {
						e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, FabricatorTuning.LIFT_TICKS + 10, 0, false, false));
					}
					BlockPos pad = this.site.pad();
					level.playSound(null, pad, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 0.5F);
				}
				if (++this.lift >= FabricatorTuning.LIFT_TICKS) {
					fire(level, this.site);
					this.lift = -1;
					this.step = 0;
					this.ticks = 0;
					lamps(level, 0);
					this.resting = !this.forced;
				}
				return;
			}
			if (++this.ticks >= FabricatorTuning.STEP_TICKS) {
				this.ticks = 0;
				this.step++;
				lamps(level, this.step);
				BlockPos pad = this.site.pad();
				level.playSound(null, pad, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.8F, 0.6F + 0.1F * this.step);
				if (this.step >= FabricatorTuning.TIMER_LAMPS) {
					this.lift = 0;
				}
			}
		}

		void lamps(ServerLevel level, int lit) {
			List<BlockPos> lamps = this.site.timerLamps();
			for (int i = 0; i < lamps.size(); i++) {
				BlockPos p = lamps.get(i);
				BlockState state = level.getBlockState(p);
				if (state.is(Blocks.REDSTONE_LAMP) && state.getValue(RedstoneLampBlock.LIT) != (i < lit)) {
					level.setBlock(p, state.setValue(RedstoneLampBlock.LIT, i < lit), Block.UPDATE_CLIENTS);
				}
			}
		}

		void stop(ServerLevel level) {
			this.step = 0;
			this.ticks = 0;
			this.lift = -1;
			if (level.isLoaded(this.site.pad())) {
				lamps(level, 0);
			}
		}

		/** Portal particles over the pad, thickening as the count goes on (also with nothing on it). */
		void particles(ServerLevel level) {
			if (level.getGameTime() % 3 != 0) {
				return;
			}
			BlockPos pad = this.site.pad();
			int n = 1 + this.step + (this.lift >= 0 ? 8 : 0);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, pad.getX() + 0.5, pad.getY() + 0.3, pad.getZ() + 0.5, n, 0.9, 0.2, 0.9, 0.02);
			level.sendParticles(ParticleTypes.PORTAL, pad.getX() + 0.5, pad.getY() + 0.8, pad.getZ() + 0.5, n, 1.0, 0.4, 1.0, 0.3);
		}
	}

	/** What stands on the pad (not spectators, not the Warden). */
	static List<LivingEntity> riders(ServerLevel level, FabricatorSite site) {
		return level.getEntitiesOfClass(LivingEntity.class, site.padArea(), e -> e.isAlive() && !e.isSpectator() && !(e instanceof Warden));
	}

	static void tryDrop(ServerLevel level, FabricatorSite site) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !level.isSpawningMonsters() && !(level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)) {
			return;
		}
		// A ward over the dripper (a Totem of Warding, e.g. in the Fabricator's own beacon) stops its spawning, not its
		// teleporter (owner, 2026-11-16). Every ward reaches us through api/Endermen.keepOut.
		if (io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanWards.isWarded(level, site.dropPoint())) {
			return;
		}
		int pile = level.getEntitiesOfClass(Mob.class, site.deckArea(), FabricatorMobs::isDeactivated).size();
		if (pile < FabricatorTuning.PILE_CAP) {
			FabricatorMobs.drop(level, site);
		}
	}

	/** It fires: everything on the pad goes up. */
	static void fire(ServerLevel level, FabricatorSite site) {
		for (LivingEntity e : riders(level, site)) {
			BlockPos to;
			if (destinationOverride != null) {
				to = destinationOverride.apply(level, site);
			} else if (e instanceof Mob mob && FabricatorMobs.isDeactivated(mob)) {
				to = FabricatorMobs.surfaceForMob(level, site, mob.getType());
			} else {
				to = FabricatorMobs.surfaceForRider(level, site, e);
			}
			if (to == null) {
				continue;
			}
			e.removeEffect(MobEffects.LEVITATION);
			if (e instanceof Mob mob && FabricatorMobs.isDeactivated(mob)) {
				FabricatorMobs.arrive(level, mob, to);
			} else {
				FabricatorMobs.departParticles(level, e.position());
				e.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
				e.resetFallDistance();
				FabricatorMobs.arriveParticles(level, Vec3.atBottomCenterOf(to));
			}
		}
	}

	/** The first time it runs: what worldgen couldn't do, the Warden, and the heart into the floor beacon. */
	public static void prepare(ServerLevel level, FabricatorSite site) {
		site.design().prepare(level, site.origin(), site.rotation());
		if (!FabricatorSites.has(level, site, FabricatorSites.WARDEN_SPAWNED)) {
			spawnWarden(level, site);
			FabricatorSites.set(level, site, FabricatorSites.WARDEN_SPAWNED);
		}
		if (FabricatorLink.beaconsLoaded() && !FabricatorSites.has(level, site, FabricatorSites.HEART_PLACED)) {
			FabricatorLink.placeHeart(level, site.floorBeacon());
			FabricatorSites.set(level, site, FabricatorSites.HEART_PLACED);
		}
		FabricatorSites.set(level, site, FabricatorSites.PREPARED);
	}

	/** The hidden spawner is gone: stopped for good, and whatever lay deactivated on the deck wakes up there. */
	static void breakFabricator(ServerLevel level, FabricatorSite site) {
		FabricatorSites.set(level, site, FabricatorSites.BROKEN);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, site.deckArea(), FabricatorMobs::isDeactivated)) {
			FabricatorMobs.wake(mob);
		}
		BlockPos tip = site.dripperTip();
		level.playSound(null, tip, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0F, 0.5F);
		level.sendParticles(ParticleTypes.SCULK_SOUL, tip.getX() + 0.5, tip.getY(), tip.getZ() + 0.5, 30, 1.0, 1.0, 1.0, 0.05);
	}

	// ---- The Warden ----

	/** One Warden per Fabricator, in its office window, passive (no AI) and persistent. Never respawned. */
	public static @Nullable Warden spawnWarden(ServerLevel level, FabricatorSite site) {
		Warden warden = EntityTypes.WARDEN.create(level, EntitySpawnReason.STRUCTURE);
		if (warden == null) {
			return null;
		}
		BlockPos at = site.wardenSpot();
		float yaw = site.wardenYaw();
		warden.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 20.0F);
		warden.setYHeadRot(yaw);
		warden.setYBodyRot(yaw);
		warden.setNoAi(true);
		warden.setPersistenceRequired();
		warden.setAttached(WARDEN, true);
		level.addFreshEntity(warden);
		return warden;
	}

	public static List<Warden> wardens(ServerLevel level, FabricatorSite site) {
		return level.getEntitiesOfClass(Warden.class, AABB.of(site.box()).inflate(16), w -> w.hasAttached(WARDEN));
	}

	static void watchWarden(ServerLevel level, FabricatorSite site) {
		for (Warden warden : wardens(level, site)) {
			if (Boolean.TRUE.equals(warden.getAttached(WARDEN))) {
				// Passive: it watches the pad (the latest thing on it, else the pad itself).
				Vec3 look = Vec3.atCenterOf(site.pad());
				List<LivingEntity> on = riders(level, site);
				if (!on.isEmpty()) {
					look = on.getFirst().position();
				}
				double dx = look.x - warden.getX();
				double dz = look.z - warden.getZ();
				float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				warden.setYRot(yaw);
				warden.setYHeadRot(yaw);
				warden.setYBodyRot(yaw);
				for (ServerPlayer player : level.players()) {
					if (!player.isShiftKeyDown() && inMachineZone(site, player.blockPosition()) && canAnger(player)) {
						wakeWarden(warden, player);
						break;
					}
				}
			} else {
				// Woken: a vanilla Warden that never digs away.
				warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
			}
		}
	}

	private static boolean canAnger(Player player) {
		return !player.isCreative() && !player.isSpectator();
	}

	/** Going for the machine or a beacon: near the hidden spawner or a beacon, or up in the roof. */
	public static boolean inMachineZone(FabricatorSite site, BlockPos pos) {
		BlockPos pad = site.pad();
		return pos.closerThan(site.spawner(), FabricatorTuning.HEAR_SPAWNER)
				|| pos.closerThan(site.ceilingBeacon(), FabricatorTuning.HEAR_CEILING_BEACON)
				|| pos.closerThan(site.floorBeacon(), FabricatorTuning.HEAR_FLOOR_BEACON)
				|| pos.getY() >= pad.getY() + FabricatorTuning.HEAR_ROOF_HEIGHT
						&& Math.hypot(pos.getX() - pad.getX(), pos.getZ() - pad.getZ()) <= FabricatorTuning.HEAR_ROOF_RADIUS
						&& pos.getY() <= site.spawner().getY() + 4;
	}

	/** A player broke a block: in a Fabricator's machine zone, its passive Warden hears it (even sneaking). */
	static void heard(ServerLevel level, Player player, BlockPos pos) {
		if (!canAnger(player)) {
			return;
		}
		Map<FabricatorSite, Running> running = RUNNING.get(level);
		if (running == null) {
			return;
		}
		for (FabricatorSite site : running.keySet()) {
			if (inMachineZone(site, pos)) {
				for (Warden warden : wardens(level, site)) {
					if (Boolean.TRUE.equals(warden.getAttached(WARDEN))) {
						wakeWarden(warden, player);
					}
				}
			}
		}
	}

	public static void wakeWarden(Warden warden, LivingEntity at) {
		warden.setAttached(WARDEN, false);
		warden.setNoAi(false);
		warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
		warden.increaseAngerAt(at, FabricatorTuning.WAKE_ANGER, true);
	}

	// ---- Debuffs ----

	/** Without Reworked Beacons: while the floor beacon stands, its "core" gives the debuffs itself. */
	static void debuffs(ServerLevel level, FabricatorSite site) {
		if (FabricatorLink.beaconsLoaded() || !level.getBlockState(site.floorBeacon()).is(Blocks.BEACON)) {
			return;
		}
		BlockPos f = site.floorBeacon();
		BlockPos c = site.ceilingBeacon();
		int r = FabricatorTuning.DEBUFF_RANGE;
		AABB area = new AABB(f.getX() - r, Math.min(f.getY(), c.getY()) - 12, f.getZ() - r, f.getX() + r + 1, Math.max(f.getY(), c.getY()) + 12, f.getZ() + r + 1);
		for (Player player : level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
			applyDebuffs(player, FabricatorTuning.DEBUFF_TICKS);
		}
	}

	/** Mining Fatigue III, Weakness I, Slowness I (ambient, like a beacon's). */
	public static void applyDebuffs(LivingEntity e, int ticks) {
		e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, FabricatorTuning.FATIGUE_AMPLIFIER, true, true));
		e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, true, true));
		e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0, true, true));
	}

	/** Whether this jukebox is a Fabricator office's (its Warden ignores it). */
	public static boolean isOfficeJukebox(ServerLevel level, BlockPos pos) {
		for (FabricatorSite site : FabricatorSites.near(level, pos, 48)) {
			if (site.jukebox().equals(pos)) {
				return true;
			}
		}
		return false;
	}

	public static boolean beaconsLoaded() {
		return FabricatorLink.beaconsLoaded();
	}

	static boolean modLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}
}

// dte:B15,B17,B21,C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanWards;
import io.github.bubba8587.bubbas_reworked_mobs.mixin.MapCentreInvoker;
import io.github.bubba8587.bubbas_reworked_mobs.undead.FireWard;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Camp patrols (specs/monster-camps.md "Camp patrols"): a spawner like vanilla's {@code PatrolSpawner}, for our camp
 * mobs. Per player on a timer and a chance; the player's nearest armed, unquiet camp whose territory holds them sends
 * a patrol of its kind from out of sight, led by a banner carrier who drops a map of the camp.
 */
public final class CampPatrols {
	private CampPatrols() {
	}

	/** A camp's last patrol: its members and when it appeared. */
	private record Patrol(List<UUID> members, long spawnedAt) {
	}

	private static final class State {
		final Map<Long, Patrol> byCamp = new HashMap<>();
		/** Spread-grid cells whose camp (if any) has been read and registered. */
		final LongSet scanned = new LongOpenHashSet();
	}

	private static final Map<ServerLevel, State> LEVELS = new WeakHashMap<>();

	private static State state(ServerLevel level) {
		return LEVELS.computeIfAbsent(level, l -> new State());
	}

	/** Whether the camp keeps a stable (it can send riders). */
	public static boolean campHasStable(CampSite site) {
		return site.hasStable();
	}

	// --- the timer ------------------------------------------------------------------------------

	public static void tick(ServerLevel level) {
		if (level.getServer() instanceof GameTestServer || level.getGameTime() % CampTuning.PATROL_CHECK_TICKS != 0 || !allowed(level)) {
			return;
		}
		RandomSource random = level.getRandom();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && random.nextFloat() < CampTuning.PATROL_CHANCE) {
				trySpawn(level, player, random, CampTuning.PATROL_MIN_DISTANCE, CampTuning.PATROL_MAX_DISTANCE);
			}
		}
	}

	/** The gamerules and difficulty that let patrols spawn. */
	public static boolean allowed(ServerLevel level) {
		return level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().get(GameRules.SPAWN_MOBS)
				&& level.getGameRules().get(GameRules.SPAWN_MONSTERS) && level.getGameRules().get(GameRules.SPAWN_PATROLS);
	}

	/**
	 * A patrol for {@code player} if every rule allows it (not a spectator, not warded, in an armed camp's territory,
	 * that camp's patrol gone and its cooldown over, dark enough for its kind, a spawn spot {@code min} to {@code max}
	 * blocks off); returns the members, or an empty list.
	 */
	public static List<Mob> trySpawn(ServerLevel level, ServerPlayer player, RandomSource random, int min, int max) {
		if (player.isSpectator() || EndermanWards.isWarded(level, player.blockPosition())) {
			return List.of();
		}
		CampSite site = campFor(level, player.blockPosition());
		if (site == null || site.kind == null || busy(level, site) || (site.kind.burns() && CampShade.sunBurns(level, player.blockPosition()))) {
			return List.of();
		}
		BlockPos at = spot(level, player, random, min, max, site.kind.mob());
		if (at == null) {
			return List.of();
		}
		boolean mounted = campHasStable(site) && site.kind.mount() != null && random.nextFloat() < CampTuning.PATROL_MOUNTED_CHANCE;
		return spawn(level, site, at, player.blockPosition(), mounted, random);
	}

	/** A patrol from {@code site} near the player now, ignoring the timer and territory (the command); mounted null: by chance. */
	public static List<Mob> spawnNear(ServerLevel level, CampSite site, ServerPlayer player, @Nullable Boolean mounted) {
		RandomSource random = level.getRandom();
		if (site.kind == null || site.kind.nether()) {
			return List.of();
		}
		BlockPos at = spot(level, player, random, CampTuning.PATROL_COMMAND_MIN_DISTANCE, CampTuning.PATROL_COMMAND_MAX_DISTANCE, site.kind.mob());
		if (at == null) {
			return List.of();
		}
		boolean rides = site.kind.mount() != null && (mounted != null ? mounted : campHasStable(site) && random.nextFloat() < CampTuning.PATROL_MOUNTED_CHANCE);
		return spawn(level, site, at, player.blockPosition(), rides, random);
	}

	/** The nearest registered camp of {@code kind}, or null. */
	public static @Nullable CampSite nearestOf(ServerLevel level, BlockPos pos, CampKind kind) {
		registerAround(level, pos, CampTuning.PATROL_TERRITORY);
		CampSite best = null;
		double bestD = Double.MAX_VALUE;
		for (CampSite site : CampSites.all(level)) {
			if (site.kind == kind) {
				double d = site.centre.distSqr(pos);
				if (d < bestD) {
					bestD = d;
					best = site;
				}
			}
		}
		return best;
	}

	/** The registered camp of a natural camp's piece, registering it if new. */
	public static CampSite known(ServerLevel level, CampPiece piece) {
		CampSite site = CampSites.get(level, ChunkPos.containing(piece.centre()).pack());
		return site != null ? site : CampSites.register(level, piece);
	}

	/** Whether the camp's last patrol still lives or its cooldown runs. */
	private static boolean busy(ServerLevel level, CampSite site) {
		Patrol last = state(level).byCamp.get(site.id);
		if (last == null) {
			return false;
		}
		if (level.getGameTime() - last.spawnedAt < CampTuning.PATROL_COOLDOWN) {
			return true;
		}
		for (UUID uuid : last.members) {
			Entity entity = level.getEntity(uuid);
			if (entity != null && entity.isAlive()) {
				return true;
			}
		}
		return false;
	}

	// --- territory ------------------------------------------------------------------------------

	/** Whether a camp owns territory: an overworld camp, armed, not quieted by a fire. */
	public static boolean ownsTerritory(CampSite site) {
		return site.kind != null && !site.kind.nether() && site.armed && !site.quiet;
	}

	/** The nearest camp whose territory holds {@code pos}, or null. Natural camps round it are registered first. */
	public static @Nullable CampSite campFor(ServerLevel level, BlockPos pos) {
		registerAround(level, pos, CampTuning.PATROL_TERRITORY);
		CampSite best = null;
		double bestD = (double) CampTuning.PATROL_TERRITORY * CampTuning.PATROL_TERRITORY;
		for (CampSite site : CampSites.all(level)) {
			if (!ownsTerritory(site)) {
				continue;
			}
			double dx = site.centre.getX() + 0.5 - (pos.getX() + 0.5);
			double dz = site.centre.getZ() + 0.5 - (pos.getZ() + 0.5);
			double d = dx * dx + dz * dz;
			if (d <= bestD) {
				bestD = d;
				best = site;
			}
		}
		return best;
	}

	/** Reads (once per cell) the camp structure starts of the spread-grid cells within {@code radius}, and registers their camps. */
	private static void registerAround(ServerLevel level, BlockPos pos, int radius) {
		var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(Camps.CAMP_KEY).orElse(null);
		var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(
				ResourceKey.create(Registries.STRUCTURE_SET, BubbasReworkedMobs.id("monster_camps")));
		if (holder == null || set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)
				|| level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).isEmpty()) {
			return;
		}
		var structure = holder.value();
		State state = state(level);
		int cell = placement.spacing() * 16;
		for (int gx = Math.floorDiv(pos.getX() - radius, cell); gx <= Math.floorDiv(pos.getX() + radius, cell); gx++) {
			for (int gz = Math.floorDiv(pos.getZ() - radius, cell); gz <= Math.floorDiv(pos.getZ() + radius, cell); gz++) {
				if (!state.scanned.add(ChunkPos.pack(gx, gz))) {
					continue;
				}
				ChunkPos chunk = placement.getPotentialStructureChunk(level.getSeed(), gx * placement.spacing(), gz * placement.spacing());
				if (CampSites.get(level, chunk.pack()) != null) {
					continue;
				}
				ChunkAccess access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS, true);
				StructureStart start = access == null ? null : access.getStartForStructure(structure);
				if (start != null && start.isValid() && start.getPieces().getFirst() instanceof CampPiece piece && CampSites.get(level, chunk.pack()) == null) {
					CampSites.register(level, piece);
				}
			}
		}
	}

	/** The centre of a patrol member's camp, or null. */
	public static @Nullable BlockPos campCentre(Mob mob) {
		CampSite site = siteOf(mob);
		return site == null ? null : site.centre;
	}

	private static @Nullable CampSite siteOf(Mob mob) {
		String patrol = CampPatrolGoal.patrolOf(mob);
		if (patrol == null || !(mob.level() instanceof ServerLevel level)) {
			return null;
		}
		try {
			return CampSites.get(level, Long.parseLong(patrol.substring(CampTuning.PATROL_TAG.length() + 1)));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// --- where ------------------------------------------------------------------------------------

	/** A spawn spot {@code min} to {@code max} blocks from the player, out of their sight if any is; or null. */
	private static @Nullable BlockPos spot(ServerLevel level, ServerPlayer player, RandomSource random, int min, int max, EntityType<?> type) {
		BlockPos seen = null;
		for (int i = 0; i < CampTuning.PATROL_SPOT_TRIES; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			int distance = min + random.nextInt(max - min + 1);
			int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
			int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
			if (!level.hasChunksAt(x - 10, z - 10, x + 10, z + 10)) {
				continue;
			}
			BlockPos column = new BlockPos(x, player.getBlockY(), z);
			BlockPos pos = column.atY(ground(level, column, player.getBlockY(), type));
			if (!valid(level, pos, type)) {
				continue;
			}
			if (!sees(level, player, pos)) {
				return pos;
			}
			if (seen == null) {
				seen = pos;
			}
		}
		return seen;
	}

	/** Valid spawn ground for the mob, dim enough (vanilla's patrol rule), not warded, no lit fire near (undead fire ward). */
	private static boolean valid(ServerLevel level, BlockPos pos, EntityType<?> type) {
		BlockState state = level.getBlockState(pos);
		return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(), type) && SpawnPlacements.isSpawnPositionOk(type, level, pos)
				&& level.getBrightness(LightLayer.BLOCK, pos) <= CampTuning.PATROL_MAX_BLOCK_LIGHT && !EndermanWards.isWarded(level, pos)
				&& level.findBlocksInBoxByManhattanDistance(pos, UndeadTuning.FIRE_WARD_RADIUS, UndeadTuning.FIRE_WARD_VERTICAL).filterState(FireWard::repels)
						.findFirst().isEmpty();
	}

	private static boolean sees(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Vec3 to = Vec3.atBottomCenterOf(pos).add(0.0, 1.5, 0.0);
		return level.clip(new ClipContext(player.getEyePosition(), to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
	}

	// --- spawning ---------------------------------------------------------------------------------

	/**
	 * Spawns a patrol of {@code site}'s kind at {@code at}, headed for {@code target}: size-scaled, the first member
	 * the banner-carrying leader, members spread round it like vanilla's; {@code mounted}: the leader and some
	 * followers ride. Returns the members (riders and passengers, not the mounts).
	 */
	public static List<Mob> spawn(ServerLevel level, CampSite site, BlockPos at, BlockPos target, boolean mounted, RandomSource random) {
		List<Mob> members = new ArrayList<>();
		CampKind kind = site.kind;
		if (kind == null || kind.nether() || site.size == null) {
			return members;
		}
		int size = CampTuning.PATROL_SIZE_MIN + site.size.ordinal() + random.nextInt(CampTuning.PATROL_SIZE_SPREAD);
		BlockPos.MutableBlockPos pos = at.mutable();
		for (int i = 0; members.size() < size && i < size * 3; i++) {
			boolean leader = members.isEmpty();
			if (!leader) {
				pos.setY(ground(level, pos, at.getY(), kind.mob()));
			}
			if (leader || valid(level, pos, kind.mob())) {
				boolean rides = mounted && (leader || random.nextFloat() < CampTuning.PATROL_RIDER_SHARE);
				members.addAll(member(level, site, kind, pos.immutable(), leader, rides, size - members.size(), random));
			}
			pos.setX(pos.getX() + random.nextInt(5) - random.nextInt(5));
			pos.setZ(pos.getZ() + random.nextInt(5) - random.nextInt(5));
		}
		if (!members.isEmpty()) {
			CampPatrolGoal.setTarget(members.getFirst(), target); // followers get the leader's steps
		}
		state(level).byCamp.put(site.id, new Patrol(members.stream().map(Entity::getUUID).toList(), level.getGameTime()));
		return members;
	}

	/** The surface height at {@code pos}'s column, or a valid one within 4 of {@code near} when the surface is far off (an overhang). */
	private static int ground(ServerLevel level, BlockPos pos, int near, EntityType<?> type) {
		int top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY();
		if (Math.abs(top - near) <= 4) {
			return top;
		}
		for (int i = 0; i <= 8; i++) {
			int y = near + (i % 2 == 0 ? i / 2 : -(i + 1) / 2);
			if (valid(level, new BlockPos(pos.getX(), y, pos.getZ()), type)) {
				return y;
			}
		}
		return top;
	}

	/** One patrol member (and, riding, its mount and any passenger; at most {@code room} members). */
	private static List<Mob> member(ServerLevel level, CampSite site, CampKind kind, BlockPos at, boolean leader, boolean rides, int room,
			RandomSource random) {
		Mob mob = create(level, site, kind.mob(), at, random);
		if (mob == null) {
			return List.of();
		}
		if (leader) {
			mob.addTag(CampTuning.PATROL_LEADER_TAG);
			mob.setItemSlot(EquipmentSlot.HEAD, CampBanners.item(kind, level.registryAccess()));
			mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
			mob.setCanPickUpLoot(false);
		}
		EntityType<? extends Mob> mountType = rides ? kind.mount() : null;
		Mob mount = mountType == null ? null : mountType.create(level, EntitySpawnReason.PATROL);
		if (mount == null) {
			return level.addFreshEntity(mob) ? List.of(mob) : List.of();
		}
		mount.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, mob.getYRot(), 0.0F);
		mount.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.PATROL, null);
		mount.addTag(CampTuning.PATROL_MOUNT_TAG);
		if (mount instanceof AbstractHorse horse) {
			horse.setAge(0);
		}
		if (mob instanceof Zombie && random.nextFloat() < CampTuning.PATROL_SPEAR_CHANCE) {
			mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		}
		List<Mob> riders = new ArrayList<>();
		mob.startRiding(mount, true, false);
		riders.add(mob);
		if (kind == CampKind.HUSK && room > 1) {
			Mob parched = create(level, site, EntityTypes.PARCHED, at, random);
			if (parched != null) {
				parched.startRiding(mount, true, false);
				riders.add(parched);
			}
		}
		level.addFreshEntityWithPassengers(mount);
		return level.getEntity(mount.getUUID()) == mount ? riders : List.of();
	}

	private static @Nullable Mob create(ServerLevel level, CampSite site, EntityType<? extends Mob> type, BlockPos at, RandomSource random) {
		Mob mob = type.create(level, EntitySpawnReason.PATROL);
		if (mob == null) {
			return null;
		}
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.PATROL, null);
		if (mob instanceof Zombie zombie && zombie.isBaby()) {
			zombie.setBaby(false);
		}
		if (mob.isPassenger()) {
			Entity vehicle = mob.getVehicle();
			mob.stopRiding();
			if (vehicle != null) {
				vehicle.discard();
			}
		}
		mob.addTag(CampTuning.PATROL_TAG);
		mob.addTag(CampTuning.PATROL_TAG + ":" + site.id);
		return mob;
	}

	// --- the leader's map ---------------------------------------------------------------------------

	/** A patrol leader died: it drops a map of its camp (not its banner). */
	public static void died(LivingEntity entity) {
		if (!(entity instanceof Mob mob) || !mob.entityTags().contains(CampTuning.PATROL_LEADER_TAG) || !(mob.level() instanceof ServerLevel level)
				|| !level.getGameRules().get(GameRules.MOB_DROPS)) {
			return;
		}
		CampSite site = siteOf(mob);
		if (site != null) {
			mob.spawnAtLocation(level, map(level, site.centre));
		}
	}

	/** The zoom that fits a territory on a map (128 blocks wide at zoom 0). */
	public static byte mapZoom() {
		byte zoom = 0;
		while (zoom < 4 && (128 << zoom) < 2 * CampTuning.PATROL_TERRITORY) {
			zoom++;
		}
		return zoom;
	}

	/** A vanilla filled map centred on the camp, its territory in view, the camp marked as explorer maps mark a structure. */
	public static ItemStack map(ServerLevel level, BlockPos centre) {
		ItemStack map = new ItemStack(Items.FILLED_MAP);
		MapItemSavedData data = MapCentreInvoker.brm$create(centre.getX(), centre.getZ(), mapZoom(), true, true, false, level.dimension());
		MapId id = level.getFreeMapId();
		level.setMapData(id, data);
		map.set(DataComponents.MAP_ID, id);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, centre, "+", MapDecorationTypes.TARGET_X);
		map.set(DataComponents.ITEM_NAME, Component.translatable("item.bubbas_reworked_mobs.camp_map"));
		return map;
	}
}

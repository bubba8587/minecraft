// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import io.github.bubba8587.bubbas_reworked_mobs.world.NaturalSpawns;
import java.util.UUID;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Where creepers come from: vanilla's random natural creeper spawns are off, and each player
 * instead gets one "natural" watcher at night, spawned behind cover just outside their view at a watching distance,
 * so that turning round quickly you might catch one half behind a tree, looking at you. Spawners, structures,
 * commands and eggs are untouched. Both halves follow the game rule {@code bubbas_reworked_mobs:watcher_creepers}
 * (off: vanilla spawns come back, ours stop). Vanilla's spawn rules still apply to our spot: the biome must list
 * creepers among its natural monsters, the spot must pass the creeper's own spawn placement and darkness rules, and
 * vanilla's spawn_mobs / spawn_monsters rules and peaceful turn it off.
 *
 * <p>Cost: every SPAWN_INTERVAL ticks, per player without a watcher, up to SPAWN_TRIES column probes and a few raycasts.
 */
public final class WatcherSpawning {
	/** On a mod-natural creeper: the player it was spawned to watch (saved, so a reload keeps the one-per-player count). */
	public static final AttachmentType<UUID> WATCHER_FOR = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("watcher_for"), UUIDUtil.CODEC);

	private WatcherSpawning() {
	}

	/** Natural spawning asks this before each creeper; false blocks vanilla's random creeper spawns (not structure spawns). */
	public static boolean blocksVanillaSpawn(ServerLevel level, EntityType<?> type) {
		return type == EntityTypes.CREEPER && level.getGameRules().get(ModGameRules.WATCHER_CREEPERS);
	}

	static void tick(ServerLevel level) {
		// The game test server runs fixed scenes; tests call trySpawnFor themselves.
		if (level.getServer() instanceof GameTestServer || level.getGameTime() % CreeperTuning.SPAWN_INTERVAL != 0) {
			return;
		}
		if (!level.getGameRules().get(ModGameRules.WATCHER_CREEPERS) || !level.getGameRules().get(GameRules.SPAWN_MOBS)
				|| !level.getGameRules().get(GameRules.SPAWN_MONSTERS) || level.getDifficulty() == Difficulty.PEACEFUL
				|| !level.isDarkOutside()) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && !(CreeperTuning.IGNORE_CREATIVE_PLAYERS && player.isCreative())
					&& watchersOf(level, player) < CreeperTuning.WATCHERS_PER_PLAYER) {
				trySpawnFor(level, player);
			}
		}
	}

	/** This player's mod-natural creepers still near them. */
	public static int watchersOf(ServerLevel level, ServerPlayer player) {
		UUID id = player.getUUID();
		return level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(CreeperTuning.WATCHER_RELEASE_DISTANCE),
				c -> c.isAlive() && id.equals(c.getAttached(WATCHER_FOR))).size();
	}

	/** Tries to place one watcher for {@code player}; the creeper, or null when no spot worked this time. */
	public static @Nullable Creeper trySpawnFor(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		Vec3 eye = player.getEyePosition();
		float lookYaw = player.getYHeadRot();
		for (int i = 0; i < CreeperTuning.SPAWN_TRIES; i++) {
			// Anywhere outside the player's view cone, at a watching distance.
			double off = CreeperTuning.SPAWN_OUTSIDE_VIEW_HALF_ANGLE + random.nextDouble() * (180.0 - CreeperTuning.SPAWN_OUTSIDE_VIEW_HALF_ANGLE);
			double yaw = Math.toRadians(lookYaw + (random.nextBoolean() ? off : -off));
			double distance = Mth.lerp(random.nextDouble(), CreeperTuning.SPAWN_MIN_DISTANCE, CreeperTuning.SPAWN_MAX_DISTANCE);
			// Minecraft's yaw: 0 faces +Z (south), 90 faces -X.
			int x = Mth.floor(player.getX() - Math.sin(yaw) * distance);
			int z = Mth.floor(player.getZ() + Math.cos(yaw) * distance);
			BlockPos cell = spawnCell(level, x, player.getBlockY(), z, random);
			if (cell == null) {
				continue;
			}
			CoverFinder.PeekSpot spot = CoverFinder.evaluate(level, EntityTypes.CREEPER.getDimensions(), cell, eye);
			if (spot == null || CoverFinder.clear(level, eye, spot.hidden().add(0.0, EntityTypes.CREEPER.getDimensions().eyeHeight(), 0.0))) {
				continue;
			}
			Creeper creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.NATURAL);
			if (creeper == null) {
				return null;
			}
			Vec3 at = spot.hidden();
			float faceYaw = (float) (Mth.atan2(eye.z - at.z, eye.x - at.x) * Mth.RAD_TO_DEG) - 90.0F;
			creeper.snapTo(at.x, at.y, at.z, faceYaw, 0.0F);
			creeper.setYHeadRot(faceYaw);
			creeper.finalizeSpawn(level, level.getCurrentDifficultyAt(cell), EntitySpawnReason.NATURAL, null);
			creeper.setAttached(WATCHER_FOR, player.getUUID());
			CreeperMind mind = CreeperAccess.mind(creeper);
			mind.watch(player, level.getGameTime());
			mind.setSpot(spot, true);
			if (level.tryAddFreshEntityWithPassengers(creeper)) {
				return creeper;
			}
			return null;
		}
		return null;
	}

	/** The highest cell in this column, from 4 above to 6 below {@code y}, where vanilla's rules let a creeper spawn. */
	private static @Nullable BlockPos spawnCell(ServerLevel level, int x, int y, int z, RandomSource random) {
		return NaturalSpawns.highest(x, y, z, 4, 6,
				pos -> CoverFinder.standable(level, pos) && NaturalSpawns.allowed(level, EntityTypes.CREEPER, pos, random));
	}
}

// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Areas that draw caretaker endermen like a cairn nexus (a Totem of the Beginning in a Reworked Beacons beacon, set by
 * Bubba's Totems & Artifacts through api/Endermen): every spawn check an area draws one with
 * {@link EndermanTuning#NEXUS_SPAWN_CHANCE} until {@link EndermanTuning#NEXUS_CAP} caretakers are in it. A newcomer
 * arrives carrying the original block of a hole in the area if it finds one (a filler), else empty-handed (a repairer
 * that tidies the ground round where it stands). They have no home cairn. The cairn spawner's rules hold: the game
 * rule, spawn_monsters and Peaceful, vanilla's spawn placement and darkness, mobGriefing for the jobs; a ward wins.
 * Each area belongs to a source key and lapses unless refreshed, like {@link EndermanWards}. Server thread only.
 */
public final class EndermanNexus {
	private record Area(AABB box, long expiresAt) {
	}

	private static final Map<ServerLevel, Map<Object, Area>> AREAS = new WeakHashMap<>();

	private EndermanNexus() {
	}

	public static void put(ServerLevel level, Object source, AABB box, int lifetimeTicks) {
		AREAS.computeIfAbsent(level, l -> new HashMap<>()).put(source, new Area(box, level.getGameTime() + Math.max(1, lifetimeTicks)));
	}

	public static void remove(ServerLevel level, Object source) {
		Map<Object, Area> areas = AREAS.get(level);
		if (areas != null) {
			areas.remove(source);
		}
	}

	/** Whether this block is inside a live nexus area (tests). */
	public static boolean isInside(ServerLevel level, BlockPos pos) {
		Map<Object, Area> areas = AREAS.get(level);
		if (areas == null) {
			return false;
		}
		long now = level.getGameTime();
		for (Area area : areas.values()) {
			if (area.expiresAt > now && area.box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
				return true;
			}
		}
		return false;
	}

	static void tick(ServerLevel level) {
		Map<Object, Area> areas = AREAS.get(level);
		if (areas == null || areas.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		areas.values().removeIf(area -> area.expiresAt <= now);
		if (level.dimension() != Level.OVERWORLD || level.getServer() instanceof GameTestServer || now % EndermanTuning.SPAWN_INTERVAL != 0
				|| !EndermanSpawning.spawningAllowed(level)) {
			return;
		}
		for (Area area : List.copyOf(areas.values())) {
			trySpawnIn(level, area.box, false);
		}
	}

	/** One spawn check for an area; {@code force} skips the chance roll (tests). Returns the new enderman, or null. */
	public static @Nullable Enderman trySpawnIn(ServerLevel level, AABB box, boolean force) {
		RandomSource random = level.getRandom();
		List<Enderman> present = level.getEntitiesOfClass(Enderman.class, box.inflate(EndermanTuning.NEXUS_MARGIN),
				e -> e.isAlive() && EndermanMind.of(e) != null);
		if (present.size() >= EndermanTuning.NEXUS_CAP || (!force && random.nextFloat() >= EndermanTuning.NEXUS_SPAWN_CHANCE)) {
			return null;
		}
		for (int i = 0; i < EndermanTuning.NEXUS_TRIES; i++) {
			int x = (int) Math.floor(box.minX + random.nextDouble() * (box.maxX - box.minX));
			int z = (int) Math.floor(box.minZ + random.nextDouble() * (box.maxZ - box.minZ));
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos cell = EndermanSpawning.spawnCell(level, x, top, z);
			if (cell == null || !box.contains(x + 0.5, cell.getY() + 0.5, z + 0.5) || EndermanWards.isWarded(level, cell)
					|| !EndermanSpawning.allowedHere(level, cell, random)
					|| level.getNearestPlayer(x + 0.5, cell.getY(), z + 0.5, EndermanTuning.NEXUS_PLAYER_DISTANCE, false) != null) {
				continue;
			}
			Enderman enderman = EntityTypes.ENDERMAN.create(level, EntitySpawnReason.NATURAL);
			if (enderman == null) {
				return null;
			}
			enderman.snapTo(x + 0.5, cell.getY(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (!level.noCollision(enderman)) {
				continue;
			}
			((EndermanAccess) enderman).brm$markCairnSpawn();
			enderman.finalizeSpawn(level, level.getCurrentDifficultyAt(cell), EntitySpawnReason.NATURAL, null);
			giveHole(level, enderman, box, random);
			return level.tryAddFreshEntityWithPassengers(enderman) ? enderman : null;
		}
		return null;
	}

	/** A filler: a hole somewhere in the area whose original block it brings. Nothing found: it comes empty-handed. */
	private static void giveHole(ServerLevel level, Enderman enderman, AABB box, RandomSource random) {
		EndermanMind mind = EndermanMind.of(enderman);
		if (mind == null || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int i = 0; i < EndermanTuning.FILL_COLUMNS; i++) {
			int x = (int) Math.floor(box.minX + random.nextDouble() * (box.maxX - box.minX));
			int z = (int) Math.floor(box.minZ + random.nextDouble() * (box.maxZ - box.minZ));
			int surface = TargetWorld.surface(level, x, z);
			if (surface == Integer.MIN_VALUE || !level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			for (int y = surface; y > surface - EndermanTuning.FILL_DEPTH; y--) {
				at.set(x, y, z);
				BlockState target = TargetWorld.target(level, at);
				if (target != null && TerrainKinds.family(target) != null && EndermanWork.needsFill(level, at, target)) {
					enderman.setCarriedBlock(target);
					mind.set(EndermanMind.Job.DELIVER, at.immutable(), level.getGameTime());
					return;
				}
			}
		}
	}
}

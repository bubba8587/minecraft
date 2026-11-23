// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import io.github.bubba8587.bubbas_reworked_mobs.world.LapsingAreas;
import io.github.bubba8587.bubbas_reworked_mobs.world.NaturalSpawns;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Areas that draw caretaker endermen like a cairn (specs/endermen.md "the nexus"). */
public final class EndermanNexus {
	private static final LapsingAreas AREAS = new LapsingAreas();

	private EndermanNexus() {
	}

	public static void put(ServerLevel level, Object source, AABB box, int lifetimeTicks) {
		AREAS.put(level, source, box, lifetimeTicks);
	}

	public static void remove(ServerLevel level, Object source) {
		AREAS.remove(level, source);
	}

	/** Whether this block is inside a live nexus area (tests). */
	public static boolean isInside(ServerLevel level, BlockPos pos) {
		return AREAS.contains(level, pos);
	}

	static void tick(ServerLevel level) {
		List<AABB> boxes = AREAS.live(level);
		if (boxes.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		if (level.dimension() != Level.OVERWORLD || level.getServer() instanceof GameTestServer || now % EndermanTuning.SPAWN_INTERVAL != 0
				|| !EndermanSpawning.spawningAllowed(level)) {
			return;
		}
		for (AABB box : boxes) {
			trySpawnIn(level, box, false);
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
			Enderman enderman = NaturalSpawns.create(level, EntityTypes.ENDERMAN, x + 0.5, cell.getY(), z + 0.5, random.nextFloat() * 360.0F);
			if (enderman == null) {
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
		for (int i = 0; i < EndermanTuning.FILL_COLUMNS; i++) {
			int x = (int) Math.floor(box.minX + random.nextDouble() * (box.maxX - box.minX));
			int z = (int) Math.floor(box.minZ + random.nextDouble() * (box.maxZ - box.minZ));
			if (EndermanWork.carryToHole(level, enderman, mind, x, z)) {
				return;
			}
		}
	}
}

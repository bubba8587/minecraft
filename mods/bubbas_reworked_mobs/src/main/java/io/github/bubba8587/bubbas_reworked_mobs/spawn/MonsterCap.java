// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spawn;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedTuning;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Natural monster cap cut (specs/undead.md "Natural spawning"). The game rules scale vanilla's global monster cap;
 * spawns picked from a structure's own spawn list (witch huts, monuments, outposts, fortresses) keep the full cap.
 * Spawners, trial spawners and raids never go through here. Called from {@code SpawnStateMixin}.
 */
public final class MonsterCap {
	/** Vanilla NaturalSpawner.MAGIC_NUMBER (17 x 17 chunks per player). */
	private static final int CHUNKS_PER_PLAYER = 17 * 17;

	private MonsterCap() {
	}

	/** The cut global monster cap for this many spawnable chunks, or vanilla's when the cut is off. */
	public static int scaledGlobalCap(ServerLevel level, int spawnableChunks) {
		int vanilla = MobCategory.MONSTER.getMaxInstancesPerChunk() * spawnableChunks / CHUNKS_PER_PLAYER;
		if (!level.getGameRules().get(ModGameRules.CUT_MONSTER_SPAWNS)) {
			return vanilla;
		}
		return vanilla * level.getGameRules().get(ModGameRules.MONSTER_CAP_PERCENT) / 100;
	}

	/** Whether one more natural monster may spawn at {@code pos} with {@code monsters} already counted. */
	public static boolean allows(ServerLevel level, EntityType<?> type, BlockPos pos, int monsters, int spawnableChunks) {
		if (type.getCategory() != MobCategory.MONSTER || monsters < scaledGlobalCap(level, spawnableChunks)) {
			return true;
		}
		if (type == EntityTypes.DROWNED) {
			// Drowned have their own allowance past the cut (owner, 2026-11-16: "a handful if you're travelling near the
			// ocean"; land monsters otherwise fill the cut cap at night): fewer than NATURAL_NEAR_CAP within range.
			AABB box = new AABB(pos).inflate(DrownedTuning.NATURAL_NEAR_RANGE);
			return level.getEntitiesOfClass(Drowned.class, box, EntitySelector.ENTITY_STILL_ALIVE).size() < DrownedTuning.NATURAL_NEAR_CAP;
		}
		return isStructureSpawn(level, pos);
	}

	/** True where the monster list comes from a structure's spawn overrides rather than the biome (same test vanilla's getMobsAt makes). */
	public static boolean isStructureSpawn(ServerLevel level, BlockPos pos) {
		if (NaturalSpawner.isInNetherFortressBounds(pos, level, MobCategory.MONSTER, level.structureManager())) {
			return true;
		}
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		return generator.getMobsAt(level, level.structureManager(), MobCategory.MONSTER, pos)
				!= level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos).getMobsToSpawn(MobCategory.MONSTER);
	}
}

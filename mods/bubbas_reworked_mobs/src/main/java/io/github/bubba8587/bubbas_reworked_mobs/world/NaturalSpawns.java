// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.world;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import org.jspecify.annotations.Nullable;

/** Shared steps for mobs we spawn ourselves under vanilla's natural rules. */
public final class NaturalSpawns {
	private NaturalSpawns() {
	}

	/** Vanilla's rules for a natural monster of {@code type} here: the biome lists it, and its placement and light rules pass. */
	public static boolean allowed(ServerLevel level, EntityType<?> type, BlockPos cell, RandomSource random) {
		boolean listed = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, cell)
				.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream().anyMatch(w -> w.value().type() == type);
		return listed && SpawnPlacements.isSpawnPositionOk(type, level, cell)
				&& SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, cell, random);
	}

	/** A new mob placed at the spot, or null when it couldn't be made or has no room there. */
	public static <T extends Mob> @Nullable T create(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw) {
		T mob = type.create(level, EntitySpawnReason.NATURAL);
		if (mob == null) {
			return null;
		}
		mob.snapTo(x, y, z, yaw, 0.0F);
		return level.noCollision(mob) ? mob : null;
	}

	/** The highest cell in column x, z from {@code y + up} down to {@code y - down} that passes {@code ok}, or null. */
	public static @Nullable BlockPos highest(int x, int y, int z, int up, int down, Predicate<BlockPos> ok) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dy = up; dy >= -down; dy--) {
			pos.set(x, y + dy, z);
			if (ok.test(pos)) {
				return pos.immutable();
			}
		}
		return null;
	}
}

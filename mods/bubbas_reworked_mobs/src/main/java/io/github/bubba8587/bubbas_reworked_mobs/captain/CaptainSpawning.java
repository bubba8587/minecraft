// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Natural captains: a fortress wither skeleton spawn is sometimes a captain instead ({@code NaturalSpawnerCaptainMixin}). */
public final class CaptainSpawning {
	private CaptainSpawning() {
	}

	/** The captain to spawn in place of a natural {@code type} spawn at {@code pos}, or null for the normal mob. */
	public static @Nullable WitherCaptain replaceNatural(ServerLevel level, EntityType<?> type, BlockPos pos) {
		if (type != EntityTypes.WITHER_SKELETON || level.getRandom().nextFloat() >= CaptainTuning.FORTRESS_CAPTAIN_CHANCE) {
			return null;
		}
		return captainAllowedAt(level, pos) ? Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.NATURAL) : null;
	}

	/** In a fortress, with no captain within the spacing. */
	public static boolean captainAllowedAt(ServerLevel level, BlockPos pos) {
		return NaturalSpawner.isInNetherFortressBounds(pos, level, MobCategory.MONSTER, level.structureManager())
				&& !WitherCaptain.anyNear(level, new AABB(pos).inflate(CaptainTuning.CAPTAIN_SPACING));
	}
}

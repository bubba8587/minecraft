// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.spawn.MonsterCap;
import io.github.bubba8587.bubbas_reworked_mobs.warding.Wards;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The natural monster cap cut. {@code canSpawn} is the per-mob test natural spawning runs after a position and mob
 * were picked (and only there), so the cut sees the mob type and position: biome spawns get the scaled cap, structure
 * spawn lists keep vanilla's. Also no natural monster spawns inside a ward (warding/Wards). Injecting at HEAD and only ever returning false composes with other spawn mods.
 */
@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState")
abstract class SpawnStateMixin {
	@Shadow
	@Final
	private int spawnableChunkCount;

	@Shadow
	@Final
	private Object2IntOpenHashMap<MobCategory> mobCategoryCounts;

	@Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
	private void brm$cutMonsterCap(EntityType<?> type, Level level, BlockPos pos, ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
		if (level instanceof ServerLevel serverLevel
				&& (!MonsterCap.allows(serverLevel, type, pos, mobCategoryCounts.getInt(MobCategory.MONSTER), spawnableChunkCount)
						|| Wards.blocksNaturalSpawn(serverLevel, type, pos))) {
			cir.setReturnValue(false);
		}
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.WatcherSpawning;
import io.github.bubba8587.bubbas_reworked_mobs.spawn.MonsterCap;
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

/** Natural monster cap cut and the natural creeper block (specs/undead.md "monster cap", specs/creepers.md). */
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
				&& WatcherSpawning.blocksVanillaSpawn(serverLevel, type)
				&& !MonsterCap.isStructureSpawn(serverLevel, pos)) {
			cir.setReturnValue(false);
			return;
		}
		if (level instanceof ServerLevel serverLevel
				&& !MonsterCap.allows(serverLevel, type, pos, mobCategoryCounts.getInt(MobCategory.MONSTER), spawnableChunkCount)) {
			cir.setReturnValue(false);
		}
	}
}

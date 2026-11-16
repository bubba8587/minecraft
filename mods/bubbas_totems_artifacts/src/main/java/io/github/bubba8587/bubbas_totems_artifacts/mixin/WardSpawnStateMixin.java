// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.warding.Wards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No natural monster spawns inside a ward (warding/Wards). {@code canSpawn} is the per-mob test natural spawning runs
 * after a position and mob were picked (and only there). Injecting at HEAD and only ever returning false composes with
 * other spawn mods (Bubba's Reworked Mobs cuts the monster cap at the same spot).
 */
@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState")
abstract class WardSpawnStateMixin {
	@Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
	private void bta$noSpawnsInWards(EntityType<?> type, Level level, BlockPos pos, ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
		if (level instanceof ServerLevel serverLevel && Wards.blocksNaturalSpawn(serverLevel, type, pos)) {
			cir.setReturnValue(false);
		}
	}
}

// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mobs.captain.CaptainSpawning;
import io.github.bubba8587.bubbas_reworked_mobs.captain.WitherCaptain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A natural fortress wither skeleton is sometimes a Wither Captain (specs/wither-captain.md). */
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerCaptainMixin {
	@WrapOperation(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;"))
	private static Mob brm$maybeCaptain(ServerLevel level, EntityType<?> type, Operation<Mob> original, @Local(name = "pos") BlockPos.MutableBlockPos pos) {
		WitherCaptain captain = CaptainSpawning.replaceNatural(level, type, pos.immutable());
		return captain != null ? captain : original.call(level, type);
	}
}

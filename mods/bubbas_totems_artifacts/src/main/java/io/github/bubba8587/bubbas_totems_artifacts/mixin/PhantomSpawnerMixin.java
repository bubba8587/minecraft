// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bubba8587.bubbas_totems_artifacts.warding.Wards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Phantoms are a natural spawn outside NaturalSpawner: a ward stops them too (wards stop natural spawns only). */
@Mixin(PhantomSpawner.class)
abstract class PhantomSpawnerMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/NaturalSpawner;isValidEmptySpawnBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/entity/EntityType;)Z"))
	private boolean bta$noPhantomsInWards(BlockGetter level, BlockPos pos, BlockState state, FluidState fluid, EntityType<?> type, Operation<Boolean> original,
			ServerLevel serverLevel) {
		return original.call(level, pos, state, fluid, type) && !Wards.blocksNaturalSpawn(serverLevel, type, pos);
	}
}

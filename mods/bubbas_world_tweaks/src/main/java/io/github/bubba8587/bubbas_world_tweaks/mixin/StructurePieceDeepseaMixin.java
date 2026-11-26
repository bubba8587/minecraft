// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import io.github.bubba8587.bubbas_world_tweaks.fortress.FortressBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deep-sea fortress block swap, pillar fill and barrels. */
@Mixin(StructurePiece.class)
abstract class StructurePieceDeepseaMixin {
	@Shadow
	protected BoundingBox boundingBox;

	@WrapOperation(method = "placeBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean bubbas_world_tweaks$deepseaBlock(WorldGenLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
			@Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
		if (!DeepseaFortresses.placing()) {
			return original.call(level, pos, state, flags);
		}
		BlockState swapped = FortressBlocks.swap((StructurePiece) (Object) this, state, pos, x, y, z);
		boolean placed = original.call(level, pos, swapped, flags);
		if (state.is(Blocks.LAVA)) {
			level.getChunk(pos).markPosForPostProcessing(pos.above());
		}
		return placed;
	}

	@WrapOperation(method = "fillColumnDown", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean bubbas_world_tweaks$deepseaPillar(WorldGenLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
			@Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 2) int z) {
		if (!DeepseaFortresses.placing()) {
			return original.call(level, pos, state, flags);
		}
		int y = pos.getY() - this.boundingBox.minY();
		return original.call(level, pos, FortressBlocks.swap((StructurePiece) (Object) this, state, pos, x, y, z), flags);
	}

	@Inject(method = "createChest(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/block/state/BlockState;)Z",
			at = @At("HEAD"), cancellable = true)
	private void bubbas_world_tweaks$deepseaBarrel(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos,
			ResourceKey<LootTable> lootTable, @Nullable BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (DeepseaFortresses.placing()) {
			cir.setReturnValue(FortressBlocks.placeBarrel(level, chunkBB, random, pos));
		}
	}
}

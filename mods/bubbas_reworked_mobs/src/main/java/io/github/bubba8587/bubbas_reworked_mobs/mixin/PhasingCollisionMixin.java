// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Phasing (soulfire/phasing/Phasing): block collision for a Phasing player follows the scaffolding rule. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class PhasingCollisionMixin {
	@ModifyReturnValue(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("RETURN"))
	private VoxelShape brm$phasing(VoxelShape shape, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Phasing.collisionShape(shape, (BlockState) (Object) this, level, pos, context);
	}
}

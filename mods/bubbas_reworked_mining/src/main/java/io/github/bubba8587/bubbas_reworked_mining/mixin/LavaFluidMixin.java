// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.mixin;

import io.github.bubba8587.bubbas_reworked_mining.natural.NaturalStone;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Stone made by lava flowing onto water counts as placed, so a stone generator can't farm rare finds. */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
	@ModifyArg(method = "spreadTo", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/LevelAccessor;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private BlockState bubbas_reworked_mining$generatedStoneIsPlaced(BlockState state) {
		return NaturalStone.placed(state);
	}
}

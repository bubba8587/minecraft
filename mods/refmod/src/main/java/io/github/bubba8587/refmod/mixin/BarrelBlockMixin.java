// dte:A2,A4
package io.github.bubba8587.refmod.mixin;

import io.github.bubba8587.refmod.blockdata.BarrelTallies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cleanup for data kept outside the block (spec requirement 17b): when a barrel turns into another block, drop its tally.
 * {@code LevelChunk#setBlockState} calls {@code affectNeighborsAfterRemoval} only when the block changes (not on a
 * facing/open state change) and the update flags include UPDATE_NEIGHBORS or a piston move. That covers players,
 * explosions, {@code /setblock} and {@code Level#destroyBlock}, unlike {@code PlayerBlockBreakEvents} (players only).
 * Removals without that flag are caught by {@link BarrelTallies#get} reading a non-barrel position as 0.
 */
@Mixin(BarrelBlock.class)
public abstract class BarrelBlockMixin {
	@Inject(method = "affectNeighborsAfterRemoval", at = @At("HEAD"))
	private void refmod$clearTally(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston, CallbackInfo ci) {
		BarrelTallies.clear(level, pos);
	}
}

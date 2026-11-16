// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every bell ring (hand, projectile, redstone) goes through this method; sleepers hear it to 32 blocks (SleeperBells). */
@Mixin(BellBlock.class)
abstract class BellBlockMixin {
	@Inject(method = "attemptToRing(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
			at = @At("RETURN"))
	private void brm$bellWakesSleepers(@Nullable Entity ringer, Level level, BlockPos pos, @Nullable Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && level instanceof ServerLevel server) {
			SleeperBells.rang(server, pos, ringer);
		}
	}
}

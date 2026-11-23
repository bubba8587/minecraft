// dte:C8,B17
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeyser;
import io.github.bubba8587.bubbas_world_tweaks.lava.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lava bubbles. */
@Mixin(LavaFluid.class)
abstract class LavaFluidBubbleMixin {
	@Inject(method = "animateTick", at = @At("TAIL"))
	private void bubbas_world_tweaks$bubble(Level level, BlockPos pos, FluidState fluidState, RandomSource random, CallbackInfo ci) {
		if (random.nextFloat() >= WorldTweaksTuning.BUBBLE_CHANCE || !level.getBlockState(pos.above()).isAir()) {
			return;
		}
		if (!LavaGeyser.magmaUnder(level, pos)) {
			return;
		}
		double surface = pos.getY() + fluidState.getHeight(level, pos);
		level.addParticle(ModParticles.LAVA_BUBBLE, pos.getX() + 0.15 + random.nextDouble() * 0.7, surface, pos.getZ() + 0.15 + random.nextDouble() * 0.7,
				0.0, 0.0, 0.0);
	}
}

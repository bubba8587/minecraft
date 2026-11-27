// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolem;
import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Amethyst golem pattern beside vanilla's snow, iron and copper golem patterns. */
@Mixin(CarvedPumpkinBlock.class)
abstract class CarvedPumpkinAmethystGolemMixin {
	@Inject(method = "canSpawnGolem", at = @At("RETURN"), cancellable = true)
	private void bubbas_world_tweaks$canSpawnAmethystGolem(LevelReader level, BlockPos topPos, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && AmethystGolems.canBuild(level, topPos)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
	private void bubbas_world_tweaks$trySpawnAmethystGolem(Level level, BlockPos topPos, CallbackInfo ci) {
		BlockPattern.BlockPatternMatch match = AmethystGolems.fullPattern().find(level, topPos);
		if (match == null) {
			return;
		}
		AmethystGolem golem = AmethystGolems.AMETHYST_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
		if (golem != null) {
			CarvedPumpkinBlockInvoker.bubbas_world_tweaks$spawnGolemInWorld(level, match, golem, match.getBlock(0, 1, 0).getPos());
			ci.cancel();
		}
	}
}

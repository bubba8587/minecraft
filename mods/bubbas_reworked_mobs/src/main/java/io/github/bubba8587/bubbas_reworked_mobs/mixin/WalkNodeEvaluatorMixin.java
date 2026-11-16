// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.warding.Wards;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wards: monsters treat blocks inside a ward as blocked, the way every mob treats a wall, so they never path in
 * (walking, climbing, amphibious and flying evaluators all extend this one). One map check when no ward exists.
 */
@Mixin(WalkNodeEvaluator.class)
abstract class WalkNodeEvaluatorMixin {
	@Inject(method = "getPathTypeOfMob", at = @At("HEAD"), cancellable = true)
	private void brm$wardBlocksMonsters(PathfindingContext context, int x, int y, int z, Mob mob, CallbackInfoReturnable<PathType> cir) {
		if (Wards.blocksEntry(mob, x, y, z)) {
			cir.setReturnValue(PathType.BLOCKED);
		}
	}
}

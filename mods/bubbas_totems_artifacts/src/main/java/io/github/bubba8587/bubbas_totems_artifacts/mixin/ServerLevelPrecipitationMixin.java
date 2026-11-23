// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Clear Skies. */
@Mixin(ServerLevel.class)
abstract class ServerLevelPrecipitationMixin {
	@ModifyExpressionValue(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isRaining()Z"))
	private boolean bta$clearSkies(boolean raining, BlockPos pos) {
		return raining && !ClearSkies.clearAt((ServerLevel) (Object) this, pos.getX(), pos.getZ());
	}
}

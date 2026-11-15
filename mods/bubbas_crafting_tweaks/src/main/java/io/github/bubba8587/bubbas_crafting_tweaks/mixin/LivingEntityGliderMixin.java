// dte:C3
package io.github.bubba8587.bubbas_crafting_tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bubba8587.bubbas_crafting_tweaks.glider.GliderHandling;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Glider turns more slowly: in {@code updateFallFlyingMovement} the horizontal velocity blends toward the look
 * direction by the constant 0.1 per tick (used twice, for x and z); with our Glider worn it is GliderHandling.GLIDER_TURN_BLEND (0.03).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGliderMixin {
	@ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "CONSTANT", args = "doubleValue=0.1"), require = 2, allow = 2)
	private double bubbas_crafting_tweaks$slowerGliderTurn(double blend) {
		return GliderHandling.isWearingGlider((LivingEntity) (Object) this) ? GliderHandling.GLIDER_TURN_BLEND : blend;
	}
}

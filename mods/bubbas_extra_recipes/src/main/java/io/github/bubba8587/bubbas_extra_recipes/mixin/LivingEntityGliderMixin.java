// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.bubba8587.bubbas_extra_recipes.glider.GliderHandling;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The Glider turns more slowly. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGliderMixin {
	@ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "CONSTANT", args = "doubleValue=0.1"), require = 2, allow = 2)
	private double bubbas_extra_recipes$slowerGliderTurn(double blend) {
		return GliderHandling.isWearingGlider((LivingEntity) (Object) this) ? GliderHandling.GLIDER_TURN_BLEND : blend;
	}
}

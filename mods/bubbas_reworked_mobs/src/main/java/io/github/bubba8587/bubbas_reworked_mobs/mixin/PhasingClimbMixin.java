// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Phasing (soulfire/phasing/Phasing): inside a block a Phasing player climbs as in scaffolding. */
@Mixin(LivingEntity.class)
abstract class PhasingClimbMixin {
	@ModifyReturnValue(method = "onClimbable", at = @At("RETURN"))
	private boolean brm$phasingClimbs(boolean climbing) {
		return climbing || Phasing.climbing((LivingEntity) (Object) this);
	}

	// Sneaking sinks instead of holding on, as in scaffolding
	@ModifyExpressionValue(method = "handleOnClimbable", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private boolean brm$phasingLikeScaffolding(boolean scaffolding) {
		return scaffolding || Phasing.climbing((LivingEntity) (Object) this);
	}
}

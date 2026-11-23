// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPosts;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A trader llama tethered in a trader post's pen still leaves when its stay is up (specs/monster-camps.md "Trader posts"). */
@Mixin(TraderLlama.class)
abstract class TraderLlamaTetherMixin {
	@ModifyReturnValue(method = "isLeashedToSomethingOtherThanTheWanderingTrader", at = @At("RETURN"))
	private boolean brm$postTetherStillLeaves(boolean original) {
		return original && !TraderPosts.tetheredAtPost((TraderLlama) (Object) this);
	}
}

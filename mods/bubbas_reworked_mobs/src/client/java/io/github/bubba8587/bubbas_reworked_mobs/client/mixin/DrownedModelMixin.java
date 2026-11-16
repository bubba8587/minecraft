// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.client.drowned.DrownedPoses;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The land crawl's limbs (and the head looking ahead while lying), after vanilla's swim strokes. Armor and the outer layer use the same model class, so they move with it. */
@Mixin(DrownedModel.class)
abstract class DrownedModelMixin {
	@Inject(method = "setupSwimAnimation(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;FF)V", at = @At("TAIL"))
	private void brm$crawl(ZombieRenderState state, float animationPos, float swimAmount, CallbackInfo ci) {
		DrownedPoses.limbs((DrownedModel) (Object) this, state, swimAmount);
	}
}

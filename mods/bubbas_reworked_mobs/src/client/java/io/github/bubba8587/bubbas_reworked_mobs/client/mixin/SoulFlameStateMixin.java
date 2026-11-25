// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.client.SoulFlameRendering;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Soul-burning onto the render state (SoulFlamesMixin draws it). */
@Mixin(EntityRenderer.class)
abstract class SoulFlameStateMixin<T extends Entity, S extends EntityRenderState> {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void brm$soulBurning(T entity, S state, float partialTicks, CallbackInfo ci) {
		state.setData(SoulFlameRendering.SOUL_BURNING, state.displayFireAnimation && SoulBurning.isSoulBurning(entity) ? Boolean.TRUE : null);
	}
}

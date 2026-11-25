// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.client.SoulFlameRendering;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.client.SoulFlames;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Blue flames on a soul-burning entity (soulfire/SoulBurning): the soul flame sprites for its flame quads. */
@Mixin(FlameFeatureRenderer.class)
abstract class SoulFlamesMixin {
	@WrapOperation(method = "buildGroup", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer;prepare(Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer$Submit;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
	private void brm$soulFlames(FlameFeatureRenderer renderer, FlameFeatureRenderer.Submit submit, VertexConsumer buffer, TextureAtlasSprite fire1,
			TextureAtlasSprite fire2, Operation<Void> original, @Local(argsOnly = true) FeatureFrameContext context) {
		if (Boolean.TRUE.equals(submit.entityRenderState().getData(SoulFlameRendering.SOUL_BURNING))) {
			original.call(renderer, submit, buffer, context.atlasManager().get(SoulFlames.FIRE_0), context.atlasManager().get(SoulFlames.FIRE_1));
		} else {
			original.call(renderer, submit, buffer, fire1, fire2);
		}
	}
}

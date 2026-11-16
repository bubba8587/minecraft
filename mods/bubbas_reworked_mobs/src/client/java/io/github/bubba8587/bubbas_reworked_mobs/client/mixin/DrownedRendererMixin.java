// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.client.drowned.DrownedPoses;
import net.minecraft.client.renderer.entity.DrownedRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lying down (specs/drowned.md, owner 2026-11-16): vanilla tilts a swimming drowned 10 degrees; ours lies flat like a
 * swimming player, and on land it crawls with a sidewinding sway ({@link DrownedPoses}).
 */
@Mixin(DrownedRenderer.class)
abstract class DrownedRendererMixin {
	@WrapOperation(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateAround(Lorg/joml/Quaternionfc;FFF)V"))
	private void brm$lieDown(PoseStack poseStack, Quaternionfc rotation, float pivotX, float pivotY, float pivotZ, Operation<Void> original,
			ZombieRenderState state, PoseStack sameStack, float bodyRot, float entityScale) {
		DrownedPoses.lieDown(state, poseStack);
	}
}

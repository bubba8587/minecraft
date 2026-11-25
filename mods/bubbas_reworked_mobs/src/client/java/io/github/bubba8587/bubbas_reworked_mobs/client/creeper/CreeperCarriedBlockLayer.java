// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.creeper;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperPillar;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

/** Pillar creeper: the block it carries, floating just over its head (turning with it). */
public final class CreeperCarriedBlockLayer extends RenderLayer<CreeperRenderState, CreeperModel> {
	/** Block size and gap over the head top (blocks). */
	private static final float SIZE = 0.55F;
	private static final float GAP = 0.08F;
	/** Head top in head space (8 px tall). */
	private static final float HEAD_TOP = -8.0F / 16.0F;
	private static @Nullable BlockModelResolver resolver;

	private CreeperCarriedBlockLayer(RenderLayerParent<CreeperRenderState, CreeperModel> renderer) {
		super(renderer);
	}

	@SuppressWarnings("unchecked")
	public static void initialize() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (type == EntityTypes.CREEPER) {
				resolver = context.getBlockModelResolver();
				helper.register(new CreeperCarriedBlockLayer((RenderLayerParent<CreeperRenderState, CreeperModel>) renderer));
			}
		});
	}

	/** Fills the render state from the creeper's synced carried block. */
	public static void extract(Creeper creeper, CreeperRenderState state) {
		BlockModelRenderState carried = ((CreeperCarryState) state).brm$carried();
		BlockState block = creeper.getAttached(CreeperPillar.CARRIED);
		BlockModelResolver r = resolver;
		if (block != null && r != null) {
			r.update(carried, block, EndermanRenderer.BLOCK_DISPLAY_CONTEXT);
		} else {
			carried.clear();
		}
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CreeperRenderState state, float yRot, float xRot) {
		BlockModelRenderState carried = ((CreeperCarryState) state).brm$carried();
		if (carried.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		getParentModel().root().getChild("head").translateAndRotate(poseStack);
		poseStack.translate(0.0F, HEAD_TOP - GAP, 0.0F);
		poseStack.scale(-SIZE, -SIZE, SIZE);
		poseStack.translate(-0.5F, 0.0F, -0.5F);
		carried.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}

// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** The worn Captain's Helm's horns: CaptainsHelmHorns drawn with the head's transform, on any humanoid wearing it. */
public class CaptainsHelmHornsLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(BubbasReworkedMobs.id("captains_helm_horns"), "main");
	public static final Identifier TEXTURE = BubbasReworkedMobs.id("textures/entity/captains_helm_horns.png");

	private final ModelPart horns;

	public CaptainsHelmHornsLayer(RenderLayerParent<S, M> parent, ModelPart horns) {
		super(parent);
		this.horns = horns;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
		if (state.isInvisible || !state.headEquipment.is(ModItems.CAPTAINS_HELM)) {
			return;
		}
		poseStack.pushPose();
		M model = this.getParentModel();
		model.root().translateAndRotate(poseStack);
		model.head.translateAndRotate(poseStack);
		var renderType = state.headEquipment.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(TEXTURE) : RenderTypes.armorCutoutNoCull(TEXTURE);
		submitNodeCollector.order(1).submitModelPart(this.horns, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, null, -1, state.outlineColor);
		poseStack.popPose();
	}
}

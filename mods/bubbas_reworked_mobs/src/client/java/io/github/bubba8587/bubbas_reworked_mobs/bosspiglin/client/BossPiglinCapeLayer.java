// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The Boss Piglin's cape, hung from the shoulders on its own baked model (no layer registration). */
public class BossPiglinCapeLayer<S extends BossPiglinRenderState, M extends BossPiglinModel> extends RenderLayer<S, M> {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("bubbas_reworked_mobs", "textures/entity/boss_piglin_cape.png");

	private final CapeModel model;

	public BossPiglinCapeLayer(RenderLayerParent<S, M> parent, EntityModelSet models) {
		super(parent);
		this.model = new CapeModel(CapeModel.createCapeLayer().bakeRoot());
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		submitNodeCollector.submitModel(this.model, state, poseStack, RenderTypes.entitySolid(TEXTURE), lightCoords,
			LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
	}

	/** The boss model's part tree with only a cape on the body, so it follows the body's waddle and lean. */
	static class CapeModel extends BossPiglinModel {
		// hang point on the top of the humped back
		private static final float TOP_Y = -16.0F;
		private static final float BACK_Z = 9.25F;
		// tilt back from the body (clears the hip band and rear), more when walking and fleeing, flat against the throne
		private static final float TILT = 0.15F;
		private static final float TILT_WALK = 0.35F;
		private static final float TILT_FLEE = 0.25F;
		private static final float FLAP_FLEE = 0.08F;
		private static final float TILT_SEATED = 0.03F;
		private static final float SEATED_LENGTH = 0.8F;
		private static final float SWAY = 0.02F;
		private static final float ROLL_FOLLOW = 0.3F;

		private final ModelPart cape;

		CapeModel(ModelPart root) {
			super(root);
			this.cape = this.body.getChild("cape");
		}

		static LayerDefinition createCapeLayer() {
			MeshDefinition mesh = BossPiglinModel.mesh(part -> CubeListBuilder.create());
			mesh.getRoot().getChild("body").addOrReplaceChild("cape",
				CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -1.0F, 20.0F, 20.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, TOP_Y, BACK_Z, 0.0F, Mth.PI, 0.0F));
			return LayerDefinition.create(mesh, 64, 32);
		}

		@Override
		public void setupAnim(BossPiglinRenderState state) {
			super.setupAnim(state);
			float seat = Mth.clamp(state.seated, 0.0F, 1.0F);
			float flee = Mth.clamp(state.fleeing, 0.0F, 1.0F);
			float walk = state.walkAnimationSpeed * (1.0F - seat);
			float t = state.ageInTicks;
			float tilt = TILT + TILT_WALK * walk + flee * (TILT_FLEE + FLAP_FLEE * Mth.sin(t * 0.7F));
			tilt = Mth.lerp(seat, tilt, TILT_SEATED);
			this.cape.xRot = -tilt;
			this.cape.zRot = SWAY * Mth.sin(t * 0.1F) - this.body.zRot * ROLL_FOLLOW;
			this.cape.yScale = Mth.lerp(seat, 1.0F, SEATED_LENGTH);
		}
	}
}

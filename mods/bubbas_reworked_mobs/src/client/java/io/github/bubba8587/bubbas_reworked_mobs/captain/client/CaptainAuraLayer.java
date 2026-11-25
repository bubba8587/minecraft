// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Wither Captain summoning aura: the Wither's energy swirl over an inflated copy of the skeleton, posed like it. */
public final class CaptainAuraLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
	public static final Identifier WITHER_ARMOR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");
	/** Vanilla Wither's swirl tint. */
	public static final int VANILLA_TINT = 0xFF808080;
	/** Soul fire blue tint. */
	public static final int SOUL_TINT = 0xFF4AA8C0;
	public static final int TINT = SOUL_TINT;
	private static final float INFLATE = 0.55F;

	private final Predicate<S> active;
	private final AuraModel<S> model;

	public CaptainAuraLayer(RenderLayerParent<S, M> parent, EntityModelSet models, Predicate<S> active) {
		super(parent);
		this.active = active;
		this.model = new AuraModel<>(createAuraLayer().bakeRoot(), parent::getModel);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
		if (!this.active.test(state)) {
			return;
		}
		float t = state.ageInTicks;
		collector.order(1).submitModel(
			this.model,
			state,
			poseStack,
			RenderTypes.energySwirl(WITHER_ARMOR_LOCATION, Mth.cos(t * 0.02F) * 3.0F % 1.0F, t * 0.01F % 1.0F),
			lightCoords,
			OverlayTexture.NO_OVERLAY,
			TINT,
			null,
			state.outlineColor);
	}

	// skeleton-shaped humanoid mesh, every cube inflated
	public static LayerDefinition createAuraLayer() {
		CubeDeformation g = new CubeDeformation(INFLATE);
		MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0F);
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, g), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, g), PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, g), PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, g), PartPose.offset(2.0F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	// aura model: poses the parent model for this state, then copies its part transforms
	private static final class AuraModel<S extends HumanoidRenderState> extends HumanoidModel<S> {
		private final Supplier<? extends HumanoidModel<S>> parent;

		AuraModel(ModelPart root, Supplier<? extends HumanoidModel<S>> parent) {
			super(root);
			this.parent = parent;
		}

		@Override
		public void setupAnim(S state) {
			super.setupAnim(state);
			HumanoidModel<S> p = this.parent.get();
			p.setupAnim(state);
			copy(p.root(), this.root());
			copy(p.head, this.head);
			copy(p.hat, this.hat);
			copy(p.body, this.body);
			copy(p.rightArm, this.rightArm);
			copy(p.leftArm, this.leftArm);
			copy(p.rightLeg, this.rightLeg);
			copy(p.leftLeg, this.leftLeg);
		}

		private static void copy(ModelPart from, ModelPart to) {
			to.x = from.x;
			to.y = from.y;
			to.z = from.z;
			to.xRot = from.xRot;
			to.yRot = from.yRot;
			to.zRot = from.zRot;
			to.xScale = from.xScale;
			to.yScale = from.yScale;
			to.zScale = from.zScale;
			to.visible = from.visible;
		}
	}
}

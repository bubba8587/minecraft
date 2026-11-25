// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.piglin.AbstractPiglinModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.util.Mth;

/** Placeholder Boss Piglin model (the art agent's version replaces it): the adult piglin, seated pose and waddle. */
public class BossPiglinModel extends AbstractPiglinModel<BossPiglinRenderState> {
	public BossPiglinModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);
		PartDefinition head = addHead(CubeDeformation.NONE, mesh);
		head.clearChild("hat");
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static ArmorModelSet<LayerDefinition> createArmorLayers() {
		return createArmorMeshSet(new CubeDeformation(0.5F), new CubeDeformation(1.02F)).map(mesh -> LayerDefinition.create(mesh, 64, 32));
	}

	@Override
	public void setupAnim(BossPiglinRenderState state) {
		super.setupAnim(state);
		float s = state.seated;
		if (s > 0.0F) {
			float drop = 10.0F * s;
			this.head.y += drop;
			this.body.y += drop;
			this.rightArm.y += drop;
			this.leftArm.y += drop;
			this.rightLeg.y += drop;
			this.leftLeg.y += drop;
			this.rightLeg.xRot = Mth.lerp(s, this.rightLeg.xRot, -1.4137167F);
			this.leftLeg.xRot = Mth.lerp(s, this.leftLeg.xRot, -1.4137167F);
			this.rightLeg.yRot = Mth.lerp(s, this.rightLeg.yRot, 0.3F);
			this.leftLeg.yRot = Mth.lerp(s, this.leftLeg.yRot, -0.3F);
		}
		if (state.fleeing > 0.0F) {
			float sway = Mth.sin(state.walkAnimationPos * 0.6662F) * 0.18F * Math.min(1.0F, state.walkAnimationSpeed * 2.0F) * state.fleeing;
			this.body.zRot += sway;
			this.head.zRot += sway * 0.5F;
			this.rightArm.zRot += 0.4F * state.fleeing + sway;
			this.leftArm.zRot -= 0.4F * state.fleeing - sway;
		}
	}

	@Override
	protected float getDefaultEarAngleInDegrees() {
		return 30.0F;
	}
}

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Amethyst Golem model: root > body > (head > crystals, left_arm, right_arm), root > left_leg, right_leg. */
public class AmethystGolemModel extends EntityModel<AmethystGolemRenderState> {
	public final ModelPart body;
	public final ModelPart head;
	public final ModelPart crystals;
	public final ModelPart leftArm;
	public final ModelPart rightArm;
	public final ModelPart leftLeg;
	public final ModelPart rightLeg;

	public AmethystGolemModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.crystals = this.head.getChild("crystals");
		this.leftArm = this.body.getChild("left_arm");
		this.rightArm = this.body.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	/** Placeholder geometry, copper golem proportions, 64x64 texture. */
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -5.0F, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 5.0F, 8.0F, new CubeDeformation(0.015F)),
				PartPose.offset(0.0F, -6.0F, 0.0F));
		head.addOrReplaceChild("crystals",
				CubeListBuilder.create()
						.texOffs(32, 0).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F, CubeDeformation.NONE)
						.texOffs(44, 0).addBox(-3.5F, -4.0F, 0.0F, 2.0F, 4.0F, 2.0F, CubeDeformation.NONE)
						.texOffs(52, 0).addBox(1.5F, -3.0F, -1.5F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -5.0F, 0.0F));
		body.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(36, 16).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offset(-4.0F, -5.0F, 0.0F));
		body.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(50, 16).addBox(0.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offset(4.0F, -5.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 27).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -5.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(16, 27).addBox(0.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, -5.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AmethystGolemRenderState state) {
		super.setupAnim(state);
		AmethystGolemAnimations.apply(this, state);
	}
}

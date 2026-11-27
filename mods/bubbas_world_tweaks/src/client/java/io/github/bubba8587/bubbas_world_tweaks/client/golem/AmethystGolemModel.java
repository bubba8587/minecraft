// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client.golem;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Amethyst Golem model: root > body > (head > (jaw, crystals > (crystal_center, crystal_left, crystal_right, crystal_back)),
 * left_arm, right_arm), root > left_leg, right_leg.
 */
public class AmethystGolemModel extends EntityModel<AmethystGolemRenderState> {
	public final ModelPart body;
	public final ModelPart head;
	public final ModelPart jaw;
	public final ModelPart crystals;
	public final ModelPart crystalCenter;
	public final ModelPart crystalLeft;
	public final ModelPart crystalRight;
	public final ModelPart crystalBack;
	public final ModelPart leftArm;
	public final ModelPart rightArm;
	public final ModelPart leftLeg;
	public final ModelPart rightLeg;

	public AmethystGolemModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.crystals = this.head.getChild("crystals");
		this.crystalCenter = this.crystals.getChild("crystal_center");
		this.crystalLeft = this.crystals.getChild("crystal_left");
		this.crystalRight = this.crystals.getChild("crystal_right");
		this.crystalBack = this.crystals.getChild("crystal_back");
		this.leftArm = this.body.getChild("left_arm");
		this.rightArm = this.body.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	/** Geometry, 64x64 texture (layout in tools-local/make_amethyst_golem_textures.py). */
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition().transformed(p -> p.translated(0.0F, 24.0F, 0.0F));
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 11).addBox(-4.0F, -6.0F, -2.5F, 8.0F, 6.0F, 5.0F),
				PartPose.offset(0.0F, -4.0F, 0.0F));

		// Head: skull, cheeks either side of the jaw, back of the mouth
		PartDefinition head = body.addOrReplaceChild("head",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-4.0F, -6.0F, -3.5F, 8.0F, 4.0F, 7.0F)
						.texOffs(48, 0).addBox(-4.0F, -2.0F, -3.5F, 2.0F, 2.0F, 4.0F)
						.texOffs(26, 11).addBox(2.0F, -2.0F, -3.5F, 2.0F, 2.0F, 4.0F)
						.texOffs(32, 6).addBox(-4.0F, -2.0F, 0.5F, 8.0F, 2.0F, 3.0F),
				PartPose.offset(0.0F, -6.0F, 0.0F));
		head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F),
				PartPose.offset(0.0F, -2.0F, 0.5F));

		// Crystal cluster growing from the top of the head
		PartDefinition crystals = head.addOrReplaceChild("crystals", CubeListBuilder.create(), PartPose.offset(0.0F, -6.0F, 0.0F));
		crystals.addOrReplaceChild("crystal_center",
				CubeListBuilder.create()
						.texOffs(0, 32).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 5.0F, 3.0F)
						.texOffs(12, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 2.0F, 2.0F)
						.texOffs(12, 36).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.5F, 0.0F, 0.7854F, 0.0F));
		crystals.addOrReplaceChild("crystal_left",
				CubeListBuilder.create()
						.texOffs(20, 32).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F)
						.texOffs(16, 36).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offsetAndRotation(2.3F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.6F));
		crystals.addOrReplaceChild("crystal_right",
				CubeListBuilder.create()
						.texOffs(28, 32).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F)
						.texOffs(44, 32).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(-2.3F, 0.0F, -0.3F, 0.0F, 0.7854F, -0.5F));
		crystals.addOrReplaceChild("crystal_back",
				CubeListBuilder.create()
						.texOffs(36, 32).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 4.0F, 2.0F)
						.texOffs(48, 32).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(-0.3F, 0.0F, 2.2F, -0.55F, -0.7854F, 0.0F));

		// Arms with basalt hands
		body.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(0, 22).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F),
				PartPose.offset(-4.0F, -5.0F, 0.0F));
		body.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(12, 22).addBox(0.0F, -1.0F, -1.5F, 3.0F, 7.0F, 3.0F),
				PartPose.offset(4.0F, -5.0F, 0.0F));

		// Legs with basalt feet
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(24, 22).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 4.0F, 4.0F),
				PartPose.offset(-2.0F, -4.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(38, 22).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 4.0F, 4.0F),
				PartPose.offset(2.0F, -4.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AmethystGolemRenderState state) {
		super.setupAnim(state);
		AmethystGolemAnimations.apply(this, state);
	}
}

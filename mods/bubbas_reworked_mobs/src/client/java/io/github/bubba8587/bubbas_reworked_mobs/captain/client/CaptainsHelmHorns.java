// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Captain's Helm horns: two three-segment horns; parent is the humanoid head (render with root + head transforms). */
public final class CaptainsHelmHorns {
	public static final int TEXTURE_WIDTH = 16;
	public static final int TEXTURE_HEIGHT = 16;
	public static final String RIGHT_HORN = "right_horn";
	public static final String LEFT_HORN = "left_horn";

	/** Attachment point on the helmet's side (head-local model units). */
	private static final float ROOT_X = 4.6F;
	private static final float ROOT_Y = -6.0F;
	/** Segment length and where the next segment starts (overlapping the joint). */
	private static final float SEGMENT = 3.0F;
	private static final float JOINT = 2.6F;
	/** Bend of each segment (radians, upward). */
	private static final float BEND_BASE = 0.15F;
	private static final float BEND_MID = 0.75F;
	private static final float BEND_TIP = 0.65F;

	private CaptainsHelmHorns() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		horn(root, RIGHT_HORN, -1.0F);
		horn(root, LEFT_HORN, 1.0F);
		return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}

	// one horn: base, mid and tip segments, each bent further up
	private static void horn(PartDefinition root, String name, float side) {
		boolean mirror = side > 0;
		float x0 = side > 0 ? 0.0F : -SEGMENT;
		PartDefinition base = root.addOrReplaceChild(name,
			CubeListBuilder.create().texOffs(0, 0).mirror(mirror).addBox(x0, -1.0F, -1.0F, SEGMENT, 2.0F, 2.0F),
			PartPose.offsetAndRotation(side * ROOT_X, ROOT_Y, 0.0F, 0.0F, 0.0F, -side * BEND_BASE));
		PartDefinition mid = base.addOrReplaceChild(name + "_mid",
			CubeListBuilder.create().texOffs(0, 4).mirror(mirror)
				.addBox(x0, -1.0F, -1.0F, SEGMENT, 2.0F, 2.0F, new CubeDeformation(-0.15F)),
			PartPose.offsetAndRotation(side * JOINT, 0.0F, 0.0F, 0.0F, 0.0F, -side * BEND_MID));
		mid.addOrReplaceChild(name + "_tip",
			CubeListBuilder.create().texOffs(0, 8).mirror(mirror).addBox(x0, -0.5F, -0.5F, SEGMENT, 1.0F, 1.0F),
			PartPose.offsetAndRotation(side * JOINT, 0.0F, 0.0F, 0.0F, 0.0F, -side * BEND_TIP));
	}
}

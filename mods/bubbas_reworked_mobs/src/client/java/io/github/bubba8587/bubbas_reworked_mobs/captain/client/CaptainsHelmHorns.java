// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Captain's Helm horns, in head space (placeholder: the art agent's model replaces it). */
public final class CaptainsHelmHorns {
	private CaptainsHelmHorns() {
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -7.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
		root.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(5.0F, -7.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 16, 16);
	}
}

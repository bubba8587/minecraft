// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;

/** Wither skeleton model plus the summoning pose (body and armour models alike, so the helm follows the head). */
public class CaptainModel extends SkeletonModel<CaptainRenderState> {
	public CaptainModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(CaptainRenderState state) {
		super.setupAnim(state);
		if (state.summoning > 0.0F) {
			CaptainPose.apply(this, state.summoning, state.ageInTicks);
		}
	}
}

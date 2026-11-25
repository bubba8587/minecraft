// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Placeholder cape layer (the art agent's version replaces it): draws nothing. */
public class BossPiglinCapeLayer<S extends BossPiglinRenderState, M extends BossPiglinModel> extends RenderLayer<S, M> {
	public BossPiglinCapeLayer(RenderLayerParent<S, M> parent, EntityModelSet models) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
	}
}

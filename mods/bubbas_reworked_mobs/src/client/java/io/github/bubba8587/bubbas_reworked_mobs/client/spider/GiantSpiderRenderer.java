// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A giant spider (specs/spiders.md "Giant spiders"): vanilla's spider model and texture at its scale (B17). Normally
 * posed by Stormie's Spiders like any spider. While it hauls a catch up a trunk it's posed here instead (owner,
 * 2026-11-22, the same as the nest spiders): vertical and head down, facing the catch hanging below it, climbing rear
 * first, with a pixel-art thread from its abdomen up to the web it's heading for.
 */
public class GiantSpiderRenderer extends SpiderRenderer<GiantSpider> {
	/** The abdomen's end above the body's middle when it hangs head down, per unit of scale (a vanilla spider's). */
	private static final float ABDOMEN = 0.7F;

	public static class State extends LivingEntityRenderState {
		public boolean hauling;
		public List<PixelStrands.Pixel> thread = List.of();
	}

	public GiantSpiderRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GiantSpider entity, LivingEntityRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		State s = (State) state;
		s.hauling = entity.haulingUp();
		s.thread = List.of();
		if (s.hauling && entity.syncedLarder().isPresent()) {
			BlockPos larder = entity.syncedLarder().get();
			Vec3 at = entity.getPosition(partialTicks);
			Vec3 from = at.add(0.0, state.boundingBoxHeight * 0.5 + ABDOMEN * entity.getScale(), 0.0);
			Vec3 to = Vec3.atCenterOf(larder);
			if (to.y > from.y) {
				List<PixelStrands.Pixel> pixels = new ArrayList<>();
				PixelStrands.trace(pixels, List.of(from, to), at, PixelStrands.texel(), entity.level());
				s.thread = pixels;
			}
		}
	}

	@Override
	protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		if (!((State) state).hauling) {
			return;
		}
		float half = state.boundingBoxHeight * 0.5F / entityScale;
		poseStack.translate(0.0F, half, 0.0F);
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.translate(0.0F, -half, 0.0F);
	}

	@Override
	public void submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		PixelStrands.submit(((State) state).thread, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(GiantSpider entity, float partialTicks) {
		AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
		return entity.haulingUp() && entity.syncedLarder().isPresent()
				? box.minmax(new AABB(entity.syncedLarder().get())) : box;
	}
}

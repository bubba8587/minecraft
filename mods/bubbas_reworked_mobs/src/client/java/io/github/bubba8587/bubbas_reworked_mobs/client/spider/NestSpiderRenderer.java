// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.bubba8587.bubbas_reworked_mobs.spider.NestSpider;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A nest spider (specs/spiders.md "Cave nests"): vanilla's spider model and texture (B17), posed by phase. Asleep it
 * hangs upside down from the ceiling (vanilla's upside-down flip); dropping it points head down; reeling and holding
 * it points head up, climbing its thread. The thread is a line from its back to where it's tied in the nest, drawn
 * in pixel art like the spun webs (crisp texels in the cobweb palette, lit by world light), while it drops, dangles,
 * reels and holds.
 */
public class NestSpiderRenderer extends SpiderRenderer<NestSpider> {

	public static class State extends LivingEntityRenderState {
		public NestSpider.Phase phase = NestSpider.Phase.SLEEPING;
		public java.util.List<PixelStrands.Pixel> thread = java.util.List.of();
	}

	public NestSpiderRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(NestSpider entity, LivingEntityRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		State s = (State) state;
		s.phase = entity.phase();
		s.isUpsideDown = s.phase == NestSpider.Phase.SLEEPING;
		s.thread = java.util.List.of();
		if (s.phase != NestSpider.Phase.SLEEPING) {
			// The thread, as pixel art: from its back up to where it's tied in the nest, taut.
			Vec3 at = entity.getPosition(partialTicks);
			Vec3 from = at.add(0.0, state.boundingBoxHeight * 0.5 + 0.7, 0.0);
			Vec3 to = entity.anchor();
			if (to.y > from.y) {
				java.util.List<PixelStrands.Pixel> pixels = new java.util.ArrayList<>();
				PixelStrands.trace(pixels, java.util.List.of(from, to), at, PixelStrands.texel(), entity.level());
				s.thread = pixels;
			}
		}
	}

	@Override
	protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		NestSpider.Phase phase = ((State) state).phase;
		if (phase == NestSpider.Phase.SLEEPING) {
			return;
		}
		// Vertical, pivoting about its middle: head down while it drops, head up while it climbs the thread.
		float half = state.boundingBoxHeight * 0.5F / entityScale;
		poseStack.translate(0.0F, half, 0.0F);
		poseStack.rotateDegrees(Axis.XP, phase == NestSpider.Phase.DROPPING || phase == NestSpider.Phase.DANGLING ? -90.0F : 90.0F);
		poseStack.translate(0.0F, -half, 0.0F);
	}

	@Override
	public void submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		PixelStrands.submit(((State) state).thread, poseStack, collector, camera);
	}

	/** Keep drawing the thread while the spider itself is below the view. */
	@Override
	protected AABB getBoundingBoxForCulling(NestSpider entity, float partialTicks) {
		AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
		return entity.sleeping() ? box : box.minmax(AABB.unitCubeFromLowerCorner(entity.anchor()));
	}
}

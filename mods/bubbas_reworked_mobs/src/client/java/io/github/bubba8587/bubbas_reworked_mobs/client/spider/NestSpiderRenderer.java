// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import com.mojang.blaze3d.vertex.PoseStack;
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
 * hangs upside down from the ceiling (vanilla's upside-down flip); off the ceiling it points head down: it drops face
 * first, and climbs back up its thread rear first, facing the victim below it (owner). The thread is a line from its back to where it's tied in the nest, drawn
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
		// Asleep, and backed off into the nest waiting by its cocoon, it hangs upside down from the ceiling.
		s.isUpsideDown = s.phase == NestSpider.Phase.SLEEPING || s.phase == NestSpider.Phase.WAITING;
		s.thread = java.util.List.of();
		if (s.phase != NestSpider.Phase.SLEEPING && s.phase != NestSpider.Phase.WAITING && s.phase != NestSpider.Phase.FEEDING) {
			// thread from its abdomen up to the nest
			Vec3 at = entity.getPosition(partialTicks);
			s.thread = PixelStrands.thread(at, at.add(0.0, state.boundingBoxHeight * 0.5 + 0.7, 0.0), entity.anchor(), entity.level());
		}
	}

	@Override
	protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		NestSpider.Phase phase = ((State) state).phase;
		if (phase == NestSpider.Phase.SLEEPING || phase == NestSpider.Phase.WAITING) {
			return;
		}
		PixelStrands.headDown(poseStack, state.boundingBoxHeight, entityScale); // off the ceiling: head down
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
		return entity.sleeping() || entity.phase() == NestSpider.Phase.WAITING ? box : box.minmax(AABB.unitCubeFromLowerCorner(entity.anchor()));
	}
}

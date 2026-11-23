// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.bubba8587.bubbas_reworked_mobs.spider.NestSpider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A nest spider (specs/spiders.md "Cave nests"): vanilla's spider model and texture (B17), posed by phase. Asleep it
 * hangs upside down from the ceiling (vanilla's upside-down flip); dropping it points head down; reeling and holding
 * it points head up, climbing its thread. The thread is a line from its back to where it's tied in the nest, drawn
 * like vanilla's fishing line, while it drops, reels and holds.
 */
public class NestSpiderRenderer extends SpiderRenderer<NestSpider> {
	/** Silk: off-white, a touch translucent-looking against stone. */
	private static final int THREAD_COLOR = 0xFFE6E3DC;

	public static class State extends LivingEntityRenderState {
		public NestSpider.Phase phase = NestSpider.Phase.SLEEPING;
		public Vec3 threadOffset = Vec3.ZERO;
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
		s.threadOffset = entity.anchor().subtract(entity.getPosition(partialTicks));
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
		State s = (State) state;
		if (s.phase == NestSpider.Phase.SLEEPING) {
			return;
		}
		float startY = s.phase == NestSpider.Phase.DROPPING ? state.boundingBoxHeight * 0.5F + 0.7F : state.boundingBoxHeight * 0.5F + 0.7F;
		float x1 = (float) s.threadOffset.x;
		float y1 = (float) s.threadOffset.y;
		float z1 = (float) s.threadOffset.z;
		if (y1 <= startY) {
			return;
		}
		float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
		collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
			float nx = x1;
			float ny = y1 - startY;
			float nz = z1;
			float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			nx /= len;
			ny /= len;
			nz /= len;
			buffer.addVertex(pose, 0.0F, startY, 0.0F).setColor(THREAD_COLOR).setNormal(pose, nx, ny, nz).setLineWidth(width);
			buffer.addVertex(pose, x1, y1, z1).setColor(THREAD_COLOR).setNormal(pose, nx, ny, nz).setLineWidth(width);
		});
	}

	/** Keep drawing the thread while the spider itself is below the view. */
	@Override
	protected AABB getBoundingBoxForCulling(NestSpider entity, float partialTicks) {
		AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
		return entity.sleeping() ? box : box.minmax(AABB.unitCubeFromLowerCorner(entity.anchor()));
	}
}

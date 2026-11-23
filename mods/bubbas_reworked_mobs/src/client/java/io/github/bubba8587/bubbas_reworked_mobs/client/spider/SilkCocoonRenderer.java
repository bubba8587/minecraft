// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SilkCocoon;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTuning;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A silk cocoon (specs/spiders.md "Cocoons"): pixel-art silk (the same crisp cobweb-palette pixels as the webs and
 * threads, lit by the world) wound round its victim. Two shapes (owner): upright victims hang head down in a vertical
 * spindle tied at the feet; animals are slung lengthwise in a horizontal one along the body, from both ends. Two spirals
 * wound opposite ways criss-cross over the spindle, narrower at the ends, with a few strands lengthwise.
 * As the silk is cut, strands go (seeded, so the same ones each frame) until it gives. Worked out once per cocoon and
 * refreshed each second, not every frame.
 */
public class SilkCocoonRenderer extends EntityRenderer<SilkCocoon, SilkCocoonRenderer.State> {
	private static final int REFRESH_TICKS = 20;

	public static class State extends EntityRenderState {
		public List<PixelStrands.Pixel> pixels = List.of();
	}

	private final PixelStrands.Cache cache = new PixelStrands.Cache(REFRESH_TICKS, 128);

	public SilkCocoonRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SilkCocoon entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		Entity victim = entity.getFirstPassenger();
		if (victim == null) {
			state.pixels = List.of();
			return;
		}
		double texel = PixelStrands.texel();
		String key = entity.position() + "|" + (int) victim.getYRot() + "|" + victim.getBbWidth() + "|" + victim.getBbHeight() + "|" + (int) entity.silk() + "|" + texel;
		state.pixels = cache.get(entity, key, () -> build(entity, victim, texel));
	}

	@Override
	protected AABB getBoundingBoxForCulling(SilkCocoon entity, float partialTicks) {
		return entity.getBoundingBox().minmax(AABB.unitCubeFromLowerCorner(entity.anchor().subtract(0.5, 0.5, 0.5)));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		PixelStrands.submit(state.pixels, poseStack, collector, camera);
		super.submit(state, poseStack, collector, camera);
	}

	private static List<PixelStrands.Pixel> build(SilkCocoon cocoon, Entity victim, double texel) {
		List<PixelStrands.Pixel> out = new ArrayList<>();
		Vec3 base = cocoon.position();
		Random rnd = new Random(cocoon.getUUID().hashCode());
		double keep = Math.max(0.15, cocoon.silk() / SpiderTuning.COCOON_SILK);
		Vec3 up = new Vec3(0.0, 1.0, 0.0);
		Vec3 axis;
		Vec3 centre;
		double length;
		double radius;
		List<Vec3> slings = new ArrayList<>();
		if (SilkCocoon.horizontal(victim)) {
			// Slung lengthwise along the animal's body, from both ends.
			float yaw = victim.getYRot() * ((float) Math.PI / 180.0F);
			axis = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
			length = SilkCocoon.bodyLength(victim) + 0.1;
			radius = victim.getBbHeight() * 0.5 + 0.06;
			centre = base.add(0.0, victim.getBbHeight() * 0.5, 0.0);
			slings.add(centre.add(axis.scale(length * 0.32)).add(0.0, radius * 0.8, 0.0));
			slings.add(centre.add(axis.scale(-length * 0.32)).add(0.0, radius * 0.8, 0.0));
		} else {
			// Upright victims hang head down; the thread is tied at their feet, the top.
			axis = up;
			length = victim.getBbHeight() + 0.2;
			radius = victim.getBbWidth() * 0.5 + 0.1;
			centre = base.add(0.0, victim.getBbHeight() * 0.5, 0.0);
			slings.add(centre.add(0.0, length * 0.5, 0.0));
		}
		// Two unit vectors round the axis.
		Vec3 side = Math.abs(axis.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : axis.cross(up).normalize();
		Vec3 other = axis.cross(side).normalize();
		int turns = Math.max(4, (int) Math.round(length / 0.22));
		int steps = turns * 12;
		for (int dir = -1; dir <= 1; dir += 2) {
			double phase = rnd.nextDouble() * Math.PI * 2.0;
			List<Vec3> run = new ArrayList<>();
			for (int k = 0; k <= steps; k++) {
				double t = (double) k / steps;
				double r = radius * (0.55 + 0.45 * Math.sin(Math.PI * t));
				double a = phase + dir * t * turns * Math.PI * 2.0;
				Vec3 p = centre.add(axis.scale((t - 0.5) * length)).add(side.scale(Math.cos(a) * r)).add(other.scale(Math.sin(a) * r));
				// Cut silk: whole runs go missing, seeded.
				if (k % 6 == 0 && !run.isEmpty() && rnd.nextDouble() > keep) {
					if (run.size() > 1) {
						PixelStrands.trace(out, run, base, texel, cocoon.level());
					}
					run = new ArrayList<>();
					continue;
				}
				run.add(p);
			}
			if (run.size() > 1) {
				PixelStrands.trace(out, run, base, texel, cocoon.level());
			}
		}
		// Lengthwise strands.
		for (int m = 0; m < 4; m++) {
			if (rnd.nextDouble() > keep + 0.2) {
				continue;
			}
			double a = m * Math.PI / 2.0 + rnd.nextDouble() * 0.6;
			List<Vec3> line = new ArrayList<>();
			for (int k = 0; k <= 8; k++) {
				double t = k / 8.0;
				double r = radius * (0.5 + 0.5 * Math.sin(Math.PI * t)) + 0.02;
				line.add(centre.add(axis.scale((t - 0.5) * length)).add(side.scale(Math.cos(a) * r)).add(other.scale(Math.sin(a) * r)));
			}
			PixelStrands.trace(out, line, base, texel, cocoon.level());
		}
		// The thread (or the two of a sling) up to where it hangs from.
		Vec3 anchor = cocoon.anchor();
		for (Vec3 tie : slings) {
			if (anchor.y > tie.y + 0.1) {
				PixelStrands.trace(out, List.of(tie, anchor), base, texel, cocoon.level());
			}
		}
		return out;
	}
}

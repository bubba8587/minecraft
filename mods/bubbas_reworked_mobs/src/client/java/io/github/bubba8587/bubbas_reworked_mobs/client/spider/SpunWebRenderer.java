// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a spun web (specs/spiders.md "Spun webs"): a tangle of strands criss-crossing between anchor points (owner,
 * 2026-11-22), in pixels, so it sits next to vanilla's cobweb block (owner: smooth strands looked out of place).
 * <ul>
 * <li>Each strand is a stepped line of crisp square pixels snapped to the world grid at the cobweb texture's own texel
 * size (1/16 with vanilla, 1/32 with a 32x pack), one texel wide (two for the heavy guy-lines), sagging a little.</li>
 * <li>Pixels take the cobweb texture's three colours (white, pale and cool grey), chosen per texel cell, so a strand
 * reads like a line drawn in the cobweb's own pixel art.</li>
 * <li>Lit like the world around each pixel (sky and block light where it hangs, no floor), so strands in shade go grey
 * like cobwebs do, not warm.</li>
 * <li>Torn strands (by the web's tear, seeded) snap: each half drapes down from its anchor.</li>
 * </ul>
 * The pixels are worked out once per web (and their light refreshed every 2 seconds), not every frame. Bounded: a web
 * has at most a few dozen strands, about a couple of thousand pixel quads; frustum-culled over its span.
 */
public class SpunWebRenderer extends EntityRenderer<SpunWeb, SpunWebRenderer.State> {
	/** Sag at the middle, as a fraction of the strand's length (less for steep strands). */
	private static final double SAG = 0.06;
	private static final int LIGHT_REFRESH_TICKS = 40;

	public static class State extends EntityRenderState {
		public List<PixelStrands.Pixel> pixels = List.of();
	}

	private final PixelStrands.Cache cache = new PixelStrands.Cache(LIGHT_REFRESH_TICKS, 256);

	public SpunWebRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SpunWeb entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		double texel = PixelStrands.texel();
		String key = entity.getUUID() + "|" + entity.strands().hashCode() + "|" + entity.tear() + "|" + entity.position() + "|" + texel;
		state.pixels = cache.get(entity, key, () -> build(entity, entity.level(), texel));
	}

	@Override
	protected AABB getBoundingBoxForCulling(SpunWeb entity, float partialTicks) {
		return entity.span();
	}

	@Override
	public boolean shouldRender(SpunWeb entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
		return !entity.strands().isEmpty() && super.shouldRender(entity, culler, camX, camY, camZ, partialTicks);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.pixels.isEmpty()) {
			return;
		}
		PixelStrands.submit(state.pixels, poseStack, collector, camera);
		super.submit(state, poseStack, collector, camera);
	}

	// --- the pixels ----------------------------------------------------------------------------------------

	private static List<PixelStrands.Pixel> build(SpunWeb web, Level level, double texel) {
		List<PixelStrands.Pixel> out = new ArrayList<>();
		Vec3 hub = web.position();
		Random rnd = new Random(web.lookSeed());
		float tear = web.tear();
		for (SpunWeb.Strand s : web.strands()) {
			// A few heavy guy-lines two texels wide; the rest one texel.
			int size = rnd.nextFloat() < 0.3F ? 2 : 1;
			if (rnd.nextFloat() < tear * 0.7F) {
				double at = 0.3 + rnd.nextDouble() * 0.4;
				PixelStrands.trace(out, world(hub, drape(s.a(), s.a().lerp(s.b(), at), rnd)), hub, texel * size, level);
				PixelStrands.trace(out, world(hub, drape(s.b(), s.b().lerp(s.a(), 1.0 - at), rnd)), hub, texel * size, level);
				continue;
			}
			double len = s.length();
			double steep = Math.abs(s.b().y - s.a().y) / Math.max(1.0E-3, len);
			double sag = SAG * (1.0 - 0.7 * steep);
			List<Vec3> line = new ArrayList<>();
			int parts = Math.max(2, (int) Math.ceil(len * 1.5));
			for (int k = 0; k <= parts; k++) {
				double t = (double) k / parts;
				line.add(s.a().lerp(s.b(), t).add(0.0, -sag * len * 4.0 * t * (1.0 - t), 0.0));
			}
			PixelStrands.trace(out, world(hub, line), hub, texel * size, level);
		}
		return out;
	}

	/** A snapped half: hangs from its anchor, out a little and then down under its weight. */
	private static List<Vec3> drape(Vec3 anchor, Vec3 brk, Random rnd) {
		double len = anchor.distanceTo(brk) * (0.45 + rnd.nextDouble() * 0.25);
		Vec3 flat = brk.subtract(anchor).multiply(1.0, 0.0, 1.0);
		Vec3 side = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
		List<Vec3> line = new ArrayList<>();
		for (int k = 0; k <= 8; k++) {
			double t = k / 8.0;
			line.add(anchor.add(side.scale(len * 0.35 * Math.sin(t * Math.PI * 0.5))).add(0.0, -len * t * t, 0.0));
		}
		return line;
	}

	private static List<Vec3> world(Vec3 hub, List<Vec3> line) {
		List<Vec3> out = new ArrayList<>(line.size());
		for (Vec3 p : line) {
			out.add(hub.add(p));
		}
		return out;
	}
}

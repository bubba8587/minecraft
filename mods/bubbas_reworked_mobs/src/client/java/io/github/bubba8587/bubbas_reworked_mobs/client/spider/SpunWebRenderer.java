// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws a spun web (specs/spiders.md "Spun webs"): a tangle of strands criss-crossing between anchor points (owner,
 * 2026-11-22), in pixels, so it sits next to vanilla's cobweb block (owner: smooth strands looked out of place).
 * <ul>
 * <li>Each strand is a stepped line of crisp square pixels snapped to the world's 1/16 texel grid, one texel wide (two
 * for the heavy guy-lines, on a 1/8 grid), sagging a little.</li>
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
	private static final Identifier PIXEL = BubbasReworkedMobs.id("textures/entity/spun_web_strand.png");
	private static final RenderType TYPE = RenderTypes.entityCutout(PIXEL);
	/** Vanilla cobweb's colours (textures/block/cobweb.png). */
	private static final int[][] PALETTE = {{255, 255, 255}, {228, 233, 233}, {196, 206, 210}};
	private static final double TEXEL = 1.0 / 16.0;
	/** Sag at the middle, as a fraction of the strand's length (less for steep strands). */
	private static final double SAG = 0.06;
	private static final int LIGHT_REFRESH_TICKS = 40;
	/** Pixels a touch bigger than their cell, so diagonal strands stay solid lines instead of dotted ones. */
	private static final double OVERLAP = 1.25;

	/** One pixel: centre (hub-relative), half-size, colour index, packed light. */
	private record Pixel(float x, float y, float z, float half, int colour, int light) {
	}

	private static final class Cached {
		String key;
		long lightTick;
		List<Pixel> pixels = List.of();
	}

	public static class State extends EntityRenderState {
		public List<Pixel> pixels = List.of();
	}

	private final Map<Integer, Cached> cache = new HashMap<>();

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
		Cached c = cache.computeIfAbsent(entity.getId(), id -> new Cached());
		String key = entity.getUUID() + "|" + entity.strands().hashCode() + "|" + entity.tear() + "|" + entity.position();
		long now = entity.level().getGameTime();
		if (!key.equals(c.key) || now - c.lightTick >= LIGHT_REFRESH_TICKS) {
			c.pixels = build(entity, entity.level());
			c.key = key;
			c.lightTick = now;
		}
		if (cache.size() > 256) {
			cache.keySet().removeIf(id -> entity.level().getEntity(id) == null);
		}
		state.pixels = c.pixels;
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
		// Camera-facing squares: the camera's right and up axes.
		Vector3f right = camera.orientation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
		Vector3f up = camera.orientation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
		List<Pixel> pixels = state.pixels;
		collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> {
			for (Pixel p : pixels) {
				int[] rgb = PALETTE[p.colour];
				float rx = right.x * p.half;
				float ry = right.y * p.half;
				float rz = right.z * p.half;
				float ux = up.x * p.half;
				float uy = up.y * p.half;
				float uz = up.z * p.half;
				vertex(buffer, pose, p.x - rx - ux, p.y - ry - uy, p.z - rz - uz, 0.0F, 1.0F, rgb, p.light);
				vertex(buffer, pose, p.x + rx - ux, p.y + ry - uy, p.z + rz - uz, 1.0F, 1.0F, rgb, p.light);
				vertex(buffer, pose, p.x + rx + ux, p.y + ry + uy, p.z + rz + uz, 1.0F, 0.0F, rgb, p.light);
				vertex(buffer, pose, p.x - rx + ux, p.y - ry + uy, p.z - rz + uz, 0.0F, 0.0F, rgb, p.light);
			}
		});
		super.submit(state, poseStack, collector, camera);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int[] rgb, int light) {
		buffer.addVertex(pose, x, y, z)
				.setColor(rgb[0], rgb[1], rgb[2], 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	// --- the pixels ----------------------------------------------------------------------------------------

	private static List<Pixel> build(SpunWeb web, Level level) {
		List<Pixel> out = new ArrayList<>();
		Vec3 hub = web.position();
		Random rnd = new Random(web.lookSeed());
		float tear = web.tear();
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		for (SpunWeb.Strand s : web.strands()) {
			// A few heavy guy-lines two texels wide; the rest one texel.
			int size = rnd.nextFloat() < 0.3F ? 2 : 1;
			if (rnd.nextFloat() < tear * 0.7F) {
				double at = 0.3 + rnd.nextDouble() * 0.4;
				trace(out, drape(s.a(), s.a().lerp(s.b(), at), rnd), hub, size, level, probe);
				trace(out, drape(s.b(), s.b().lerp(s.a(), 1.0 - at), rnd), hub, size, level, probe);
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
			trace(out, line, hub, size, level, probe);
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

	/** Walks a polyline in half-texel steps and adds one pixel per grid cell it enters (snapped to the world grid). */
	private static void trace(List<Pixel> out, List<Vec3> line, Vec3 hub, int size, Level level, BlockPos.MutableBlockPos probe) {
		double cell = TEXEL * size;
		long last = Long.MIN_VALUE;
		for (int i = 0; i + 1 < line.size(); i++) {
			Vec3 a = hub.add(line.get(i));
			Vec3 b = hub.add(line.get(i + 1));
			int steps = Math.max(1, (int) Math.ceil(a.distanceTo(b) / (cell * 0.5)));
			for (int k = 0; k <= steps; k++) {
				Vec3 w = a.lerp(b, (double) k / steps);
				long cx = (long) Math.floor(w.x / cell);
				long cy = (long) Math.floor(w.y / cell);
				long cz = (long) Math.floor(w.z / cell);
				long id = (cx * 73856093L) ^ (cy * 19349663L) ^ (cz * 83492791L);
				if (id == last) {
					continue;
				}
				last = id;
				double px = (cx + 0.5) * cell;
				double py = (cy + 0.5) * cell;
				double pz = (cz + 0.5) * cell;
				probe.set(Math.floor(px), Math.floor(py), Math.floor(pz));
				int light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, probe), level.getBrightness(LightLayer.SKY, probe));
				int h = (int) ((id >>> 7) & 15);
				int colour = h < 7 ? 0 : (h < 13 ? 1 : 2);
				out.add(new Pixel((float) (px - hub.x), (float) (py - hub.y), (float) (pz - hub.z), (float) (cell * 0.5 * OVERLAP), colour, light));
			}
		}
	}
}

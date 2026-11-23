// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import com.mojang.math.Axis;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Silk drawn as pixel art: webs, cocoons and spider threads (specs/spiders.md "pixel art"). */
public final class PixelStrands {
	public static final Identifier PIXEL = BubbasReworkedMobs.id("textures/entity/spun_web_strand.png");
	public static final RenderType TYPE = RenderTypes.entityCutout(PIXEL);
	/** Vanilla cobweb's colours (textures/block/cobweb.png). */
	private static final int[][] PALETTE = {{255, 255, 255}, {228, 233, 233}, {196, 206, 210}};
	private static final Identifier COBWEB_SPRITE = Identifier.withDefaultNamespace("block/cobweb");
	private static final double OVERLAP = 1.25;

	/** One pixel: centre relative to an origin, half-size, colour index, packed light. */
	public record Pixel(float x, float y, float z, float half, int colour, int light) {
	}

	private PixelStrands() {
	}

	/** A taut thread from {@code from} up to {@code to}, relative to {@code origin}; empty unless {@code to} is higher. */
	public static List<Pixel> thread(Vec3 origin, Vec3 from, Vec3 to, Level level) {
		if (to.y <= from.y) {
			return List.of();
		}
		List<Pixel> pixels = new ArrayList<>();
		trace(pixels, List.of(from, to), origin, texel(), level);
		return pixels;
	}

	/** Turns a spider model a quarter turn about its middle so it hangs head down. */
	public static void headDown(PoseStack poseStack, float boundingBoxHeight, float entityScale) {
		float half = boundingBoxHeight * 0.5F / entityScale;
		poseStack.translate(0.0F, half, 0.0F);
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.translate(0.0F, -half, 0.0F);
	}

	/** Pixels per entity, rebuilt when their key changes or after {@code refreshTicks} (light changes). */
	public static final class Cache {
		private record Entry(String key, long tick, List<Pixel> pixels) {
		}

		private final Map<Integer, Entry> entries = new HashMap<>();
		private final int refreshTicks;
		private final int pruneAbove;

		public Cache(int refreshTicks, int pruneAbove) {
			this.refreshTicks = refreshTicks;
			this.pruneAbove = pruneAbove;
		}

		public List<Pixel> get(Entity entity, String key, Supplier<List<Pixel>> build) {
			long now = entity.level().getGameTime();
			Entry e = this.entries.get(entity.getId());
			if (e == null || !key.equals(e.key) || now - e.tick >= this.refreshTicks) {
				e = new Entry(key, now, build.get());
				this.entries.put(entity.getId(), e);
			}
			if (this.entries.size() > this.pruneAbove) {
				this.entries.keySet().removeIf(id -> entity.level().getEntity(id) == null);
			}
			return e.pixels;
		}
	}

	/** One texel of the loaded cobweb texture (1/16 vanilla, 1/32 with a 32x pack), capped at 1/32. */
	public static double texel() {
		try {
			int w = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(COBWEB_SPRITE).contents().width();
			return 1.0 / Math.max(16, Math.min(32, w));
		} catch (RuntimeException e) {
			return 1.0 / 16.0;
		}
	}

	/**
	 * Adds the pixels of a polyline given in world coordinates ({@code points}), each relative to {@code origin}, with
	 * square cells of side {@code cell}.
	 */
	public static void trace(List<Pixel> out, List<Vec3> points, Vec3 origin, double cell, Level level) {
		long last = Long.MIN_VALUE;
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		long lightBlock = Long.MIN_VALUE;
		int light = 0;
		for (int i = 0; i + 1 < points.size(); i++) {
			Vec3 a = points.get(i);
			Vec3 b = points.get(i + 1);
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
				long block = probe.asLong();
				if (block != lightBlock) {
					lightBlock = block;
					light = LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, probe), level.getBrightness(LightLayer.SKY, probe));
				}
				int h = (int) ((id >>> 7) & 15);
				int colour = h < 7 ? 0 : (h < 13 ? 1 : 2);
				out.add(new Pixel((float) (px - origin.x), (float) (py - origin.y), (float) (pz - origin.z), (float) (cell * 0.5 * OVERLAP),
						colour, light));
			}
		}
	}

	/** Draws pixels as camera-facing squares in the current pose (whose origin is the pixels' origin). */
	public static void submit(List<Pixel> pixels, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (pixels.isEmpty()) {
			return;
		}
		Vector3f right = camera.orientation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
		Vector3f up = camera.orientation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
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
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int[] rgb, int light) {
		buffer.addVertex(pose, x, y, z)
				.setColor(rgb[0], rgb[1], rgb[2], 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}

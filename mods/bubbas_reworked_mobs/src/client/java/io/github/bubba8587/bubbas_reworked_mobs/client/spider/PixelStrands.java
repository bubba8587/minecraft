// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.client.spider;

import java.util.List;

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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Silk drawn as pixel art (owner, 2026-11-22: custom visuals are crisp pixels in a vanilla palette, lit by world light,
 * like vanilla's lead and fishing line), shared by the spun webs and the nest spiders' drop thread.
 * <ul>
 * <li>A line is a run of square pixels, one per world-grid cell it passes through, at the loaded cobweb texture's texel
 * size (1/16 with vanilla, 1/32 with a 32x pack), so silk always matches the cobweb blocks beside it.</li>
 * <li>Each pixel takes one of the cobweb texture's three colours (white, pale grey, cool grey), picked per cell.</li>
 * <li>Each pixel is lit by the sky and block light of the block it's in (one lookup per block crossed), with no glow
 * floor, so silk in shade goes grey like cobwebs.</li>
 * <li>Pixels are camera-facing squares a touch bigger than their cell, so diagonal lines stay solid.</li>
 * </ul>
 */
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

// dte:C1,B19
package io.github.bubba8587.bubbas_reworked_mobs.client.meter;

import net.minecraft.client.gui.GuiPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.meter.Meters;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import io.github.bubba8587.bubbas_reworked_mobs.grip.Gripper;
import net.minecraft.world.entity.monster.zombie.Drowned;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Meter bars: on the held player's HUD and over mobs in the world (specs/drowned.md, specs/creepers.md). */
public final class MeterBars {
	/** Entity id to (kind << 8 | value). */
	private static final Int2IntOpenHashMap METERS = new Int2IntOpenHashMap();
	/** Bars over mobs show this close (blocks). */
	public static final double SHOW_DISTANCE = 32.0;
	private static final int WIDTH = 182;
	private static final int HEIGHT = 5;
	/** World bar size (blocks): about a block wide, a little thicker than the sprite's proportions so it reads. */
	private static final float WORLD_WIDTH = 1.0F;
	private static final float WORLD_HEIGHT = 0.1F;
	/** Gap above the head (blocks). */
	private static final float ABOVE_HEAD = 0.35F;
	private static final int FULL_BRIGHT = 15728880;

	private static final Identifier[] BACKGROUND = {Identifier.withDefaultNamespace("boss_bar/blue_background"), Identifier.withDefaultNamespace("boss_bar/red_background")};
	private static final Identifier[] PROGRESS = {Identifier.withDefaultNamespace("boss_bar/blue_progress"), Identifier.withDefaultNamespace("boss_bar/red_progress")};
	private static final Identifier NOTCHES_BACKGROUND = Identifier.withDefaultNamespace("boss_bar/notched_20_background");
	private static final Identifier NOTCHES_PROGRESS = Identifier.withDefaultNamespace("boss_bar/notched_20_progress");

	/** The same sprites as plain textures for the world bars (each sprite's file is a loadable texture). */
	private static RenderType texture(Identifier sprite) {
		return RenderTypes.text(sprite.withPath(path -> "textures/gui/sprites/" + path + ".png"));
	}

	private static final RenderType[] WORLD_BACKGROUND = {texture(BACKGROUND[0]), texture(BACKGROUND[1])};
	private static final RenderType[] WORLD_PROGRESS = {texture(PROGRESS[0]), texture(PROGRESS[1])};
	private static final RenderType WORLD_NOTCHES_BACKGROUND = texture(NOTCHES_BACKGROUND);
	private static final RenderType WORLD_NOTCHES_PROGRESS = texture(NOTCHES_PROGRESS);

	private MeterBars() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(Meters.MeterPayload.TYPE, (payload, context) -> {
			if (payload.value() <= 0) {
				METERS.remove(payload.id());
			} else {
				METERS.put(payload.id(), (payload.kind() << 8) | (payload.value() & 0xFF));
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> METERS.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> METERS.clear());
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> METERS.remove(entity.getId()));
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BubbasReworkedMobs.id("grip_meter"), MeterBars::extractHud);
	}

	/** The meter value (1-100) of this entity, or 0. */
	public static int value(Entity entity) {
		return METERS.getOrDefault(entity.getId(), 0) & 0xFF;
	}

	private static int kindIndex(int packed) {
		return (packed >> 8) == Meters.STARTLE ? 1 : 0;
	}

	/** Render-state extract: which bar, and where (over the head, or over a held victim's head). */
	public static void extract(Entity entity, EntityRenderState state, float partialTicks) {
		MeterState meter = (MeterState) state;
		int packed = METERS.getOrDefault(entity.getId(), 0);
		if ((packed & 0xFF) == 0 || state.distanceToCameraSq > SHOW_DISTANCE * SHOW_DISTANCE) {
			meter.brm$setMeter(0, 0.0F, 0.0F, 0.0F);
			return;
		}
		float x = 0.0F;
		float y = entity.getBbHeight() + ABOVE_HEAD;
		float z = 0.0F;
		for (Entity passenger : entity.getPassengers()) {
			// Over the held victim's head, not across their body.
			x = (float) (passenger.getX(partialTicks) - entity.getX(partialTicks));
			z = (float) (passenger.getZ(partialTicks) - entity.getZ(partialTicks));
			y = Math.max(y, (float) (passenger.getY(partialTicks) - entity.getY(partialTicks)) + passenger.getBbHeight() + ABOVE_HEAD);
		}
		meter.brm$setMeter(packed, x, y, z);
	}

	/** The world bar: camera-facing, background and notches, then the filled part. */
	public static void submit(MeterState meter, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		int packed = meter.brm$meter();
		int kind = kindIndex(packed);
		float fill = (packed & 0xFF) / 100.0F;
		poseStack.pushPose();
		poseStack.translate(meter.brm$barX(), meter.brm$barY(), meter.brm$barZ());
		poseStack.rotate(camera.orientation);
		// Name-tag space: x right, y down; one unit is one sprite pixel.
		poseStack.scale(WORLD_WIDTH / WIDTH, -WORLD_HEIGHT / HEIGHT, WORLD_WIDTH / WIDTH);
		float x0 = -WIDTH / 2.0F;
		float y0 = -HEIGHT / 2.0F;
		quad(collector, poseStack, WORLD_BACKGROUND[kind], x0, y0, WIDTH, 1.0F, 0.0F);
		quad(collector, poseStack, WORLD_NOTCHES_BACKGROUND, x0, y0, WIDTH, 1.0F, 0.5F);
		quad(collector, poseStack, WORLD_PROGRESS[kind], x0, y0, WIDTH * fill, fill, 1.0F);
		quad(collector, poseStack, WORLD_NOTCHES_PROGRESS, x0, y0, WIDTH * fill, fill, 1.5F);
		poseStack.popPose();
	}

	/** One layer, {@code width} pixels wide showing the left {@code u1} of the sprite, {@code z} units toward the camera (positive is toward it in this space). */
	private static void quad(SubmitNodeCollector collector, PoseStack poseStack, RenderType type, float x0, float y0, float width, float u1, float z) {
		if (width <= 0.0F) {
			return;
		}
		collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
			vertex(buffer, pose, x0, y0 + HEIGHT, z, 0.0F, 1.0F);
			vertex(buffer, pose, x0 + width, y0 + HEIGHT, z, u1, 1.0F);
			vertex(buffer, pose, x0 + width, y0, z, u1, 0.0F);
			vertex(buffer, pose, x0, y0, z, 0.0F, 0.0F);
		});
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
		buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setLight(FULL_BRIGHT);
	}

	/** The held player's grip bar, under the crosshair. */
	private static void extractHud(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		Entity holder = mc.player == null ? null : mc.player.getVehicle();
		if (!(holder instanceof Drowned || holder instanceof Gripper)) {
			return;
		}
		int packed = METERS.getOrDefault(holder.getId(), 0);
		int value = packed & 0xFF;
		if (value == 0) {
			return;
		}
		int kind = kindIndex(packed);
		int x = (graphics.guiWidth() - WIDTH) / 2;
		int y = graphics.guiHeight() / 2 + 14;
		int filled = Math.round(value / 100.0F * WIDTH);
		graphics.blitSprite(GuiPipeline.TEXTURED, BACKGROUND[kind], WIDTH, HEIGHT, 0, 0, x, y, WIDTH, HEIGHT);
		graphics.blitSprite(GuiPipeline.TEXTURED, NOTCHES_BACKGROUND, WIDTH, HEIGHT, 0, 0, x, y, WIDTH, HEIGHT);
		if (filled > 0) {
			graphics.blitSprite(GuiPipeline.TEXTURED, PROGRESS[kind], WIDTH, HEIGHT, 0, 0, x, y, filled, HEIGHT);
			graphics.blitSprite(GuiPipeline.TEXTURED, NOTCHES_PROGRESS, WIDTH, HEIGHT, 0, 0, x, y, filled, HEIGHT);
		}
	}
}

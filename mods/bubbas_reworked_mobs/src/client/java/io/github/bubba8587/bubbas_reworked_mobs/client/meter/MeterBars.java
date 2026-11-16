// dte:C1,B19
package io.github.bubba8587.bubbas_reworked_mobs.client.meter;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.meter.Meters;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/**
 * Draws the visible meters ({@link Meters}, game rule {@code show_meters}): a bar of 20 marks under the mob's name-tag
 * spot (blue grip on a drowned, red startle on a creeper, filled marks coloured, the rest dark grey), and on the
 * screen of a player a drowned holds, a blue bar under the crosshair (filled rectangles, vanilla-style: black edge,
 * dark well). The values come from the server; nothing is computed here.
 */
public final class MeterBars {
	/** Entity id to (kind << 8 | value). */
	private static final Int2IntOpenHashMap METERS = new Int2IntOpenHashMap();
	private static final int MARKS = 20;
	/** Bars above heads show this close (blocks). */
	public static final double SHOW_DISTANCE = 32.0;
	private static final int HUD_WIDTH = 81;
	private static final int HUD_HEIGHT = 5;
	private static final int BLUE = 0xFF3F8CFF;
	private static final int BLUE_DARK = 0xFF1A2A40;

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
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, BubbasReworkedMobs.id("grip_meter"), MeterBars::extractHud);
	}

	/** The meter value (1-100) of this entity, or 0. */
	public static int value(Entity entity) {
		return METERS.getOrDefault(entity.getId(), 0) & 0xFF;
	}

	/** The bar to draw above this entity, or null. */
	public static @Nullable Component bar(Entity entity) {
		int packed = METERS.getOrDefault(entity.getId(), 0);
		int value = packed & 0xFF;
		if (value == 0) {
			return null;
		}
		ChatFormatting colour = (packed >> 8) == Meters.STARTLE ? ChatFormatting.RED : ChatFormatting.BLUE;
		int filled = Math.max(1, Math.round(value / 100.0F * MARKS));
		MutableComponent bar = Component.literal("|".repeat(filled)).withStyle(colour, ChatFormatting.BOLD);
		if (filled < MARKS) {
			bar.append(Component.literal("|".repeat(MARKS - filled)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
		}
		return bar;
	}

	/** The held player's grip bar, under the crosshair. */
	private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !(mc.player.getVehicle() instanceof Drowned drowned)) {
			return;
		}
		int value = value(drowned);
		if (value == 0) {
			return;
		}
		int x0 = (graphics.guiWidth() - HUD_WIDTH) / 2;
		int y0 = graphics.guiHeight() / 2 + 12;
		graphics.fill(x0 - 1, y0 - 1, x0 + HUD_WIDTH + 1, y0 + HUD_HEIGHT + 1, 0xFF000000);
		graphics.fill(x0, y0, x0 + HUD_WIDTH, y0 + HUD_HEIGHT, BLUE_DARK);
		int filled = Math.round(value / 100.0F * HUD_WIDTH);
		graphics.fill(x0, y0, x0 + filled, y0 + HUD_HEIGHT, BLUE);
		// A lighter top line, like vanilla's bars.
		graphics.fill(x0, y0, x0 + filled, y0 + 1, 0xFF8FC0FF);
	}
}

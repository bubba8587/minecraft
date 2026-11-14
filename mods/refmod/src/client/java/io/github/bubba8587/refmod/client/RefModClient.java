// dte:B5,B6
package io.github.bubba8587.refmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.component.TallyCount;
import io.github.bubba8587.refmod.network.LampTogglePayload;
import io.github.bubba8587.refmod.registry.ModAttachments;
import io.github.bubba8587.refmod.registry.ModComponents;
import io.github.bubba8587.refmod.registry.ModEntities;
import io.github.bubba8587.refmod.registry.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class RefModClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(RefMod.id("main"));
	public static final KeyMapping SHOW_STATS_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.refmod.show_stats", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, CATEGORY));

	@Override
	public void onInitializeClient() {
		// Entity: thrown-item renderer for pebbles.
		EntityRenderers.register(ModEntities.PEBBLE, ThrownItemRenderer::new);

		// Networking: log the lamp toggle and show it in the action bar.
		ClientPlayNetworking.registerGlobalReceiver(LampTogglePayload.TYPE, (payload, context) -> {
			RefMod.LOGGER.info("Lamp at {} is now {} (you have toggled {} lamps)", payload.pos().toShortString(), payload.lit() ? "lit" : "unlit", payload.total());
			context.player().sendOverlayMessage(Component.translatable(
					payload.lit() ? "message.refmod.lamp_on" : "message.refmod.lamp_off", payload.total()));
		});

		// Client: key mapping printing the synced attachment copy.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (SHOW_STATS_KEY.consumeClick()) {
				if (client.player != null) {
					int toggles = ModAttachments.getLampToggles(client.player);
					client.player.sendSystemMessage(Component.translatable("message.refmod.stats_self", toggles));
				}
			}
		});

		// Client: HUD line while holding the tally stick.
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, RefMod.id("tally_hud"), RefModClient::extractTallyHud);
	}

	private static void extractTallyHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		ItemStack held = mc.player.getMainHandItem();
		if (!held.is(ModItems.TALLY_STICK)) {
			return;
		}
		int uses = held.getOrDefault(ModComponents.TALLY_COUNT, TallyCount.ZERO).uses();
		graphics.text(mc.font, Component.translatable("hud.refmod.tally", uses), 4, 4, CommonColors.WHITE, true);
	}
}

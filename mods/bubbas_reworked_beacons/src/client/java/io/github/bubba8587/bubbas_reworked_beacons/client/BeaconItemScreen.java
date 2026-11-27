// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.client;

import net.minecraft.client.gui.GuiPipeline;
import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.Payloads;
import io.github.bubba8587.bubbas_reworked_beacons.menu.BeaconItemMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** The reworked beacon screen. */
public class BeaconItemScreen extends AbstractContainerScreen<BeaconItemMenu> {
	private static final Identifier BEACON_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/beacon.png");
	private static final int PANEL = 0xFF212121;
	private static final int GREY = 0xFFC6C6C6;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int DIM = 0xFFA0A0A0;
	private static final int PANEL_LEFT = 10;
	private static final int PANEL_RIGHT = 220;
	private static final int TEXT_WIDTH = PANEL_RIGHT - PANEL_LEFT - 8;

	public BeaconItemScreen(BeaconItemMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 230, 219);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics) {
		super.extractBackground(graphics);
		int xo = this.leftPos;
		int yo = this.topPos;
		graphics.blit(GuiPipeline.TEXTURED, BEACON_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
		graphics.fill(xo + PANEL_LEFT, yo + 7, xo + PANEL_RIGHT, yo + 100, PANEL);
		// Vanilla's payment row is gone
		graphics.fill(xo + 12, yo + 103, xo + 132, yo + 132, GREY);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics) {
		int y = 12;
		graphics.centeredText(this.font, this.title, this.imageWidth / 2, y, TEXT);
		y += 14;
		for (FormattedCharSequence line : this.font.split(statusLine(), TEXT_WIDTH)) {
			graphics.text(this.font, line, PANEL_LEFT + 4, y, DIM, false);
			y += 10;
		}
		y += 4;
		for (Component c : itemLines()) {
			for (FormattedCharSequence line : this.font.split(c, TEXT_WIDTH)) {
				if (y > 90) {
					return;
				}
				graphics.text(this.font, line, PANEL_LEFT + 4, y, TEXT, false);
				y += 10;
			}
		}
	}

	private Component statusLine() {
		int levels = this.menu.levels();
		return switch (this.menu.mode()) {
			case SKY -> Component.translatable("screen.bubbas_reworked_beacons.sky", levels, BeaconContext.skyRange(levels));
			case PAIRED -> Component.translatable("screen.bubbas_reworked_beacons.paired", levels, BeaconContext.pairedRange(levels));
			case IDLE -> Component.translatable(this.menu.floorLevels() > 0
					? "screen.bubbas_reworked_beacons.idle_blocked" : "screen.bubbas_reworked_beacons.idle_no_pyramid");
		};
	}

	private List<Component> itemLines() {
		List<Component> lines = new ArrayList<>();
		ItemStack item = this.menu.item();
		BeaconPayload payload = Payloads.find(item);
		if (payload == null) {
			lines.add(Component.translatable("screen.bubbas_reworked_beacons.empty").withStyle(ChatFormatting.GRAY));
			return lines;
		}
		int levels = Math.max(1, this.menu.levels());
		int power = payload.power(item);
		BeaconContext ctx = new BeaconContext(this.minecraft.level, BlockPos.ZERO,
				this.menu.mode() == BeaconMode.PAIRED ? BeaconMode.PAIRED : BeaconMode.SKY, levels, power, new AABB(BlockPos.ZERO));
		lines.add(item.getHoverName().copy().withStyle(ChatFormatting.WHITE));
		lines.addAll(payload.describe(item, ctx));
		int required = payload.requiredLevels(item);
		if (this.menu.mode() != BeaconMode.IDLE && this.menu.levels() < required) {
			lines.add(Component.translatable("screen.bubbas_reworked_beacons.needs_levels", required).withStyle(ChatFormatting.RED));
		} else if (power > ctx.powerCap()) {
			lines.add(Component.translatable("screen.bubbas_reworked_beacons.capped", Component.translatable("enchantment.level." + ctx.effectivePower()))
					.withStyle(ChatFormatting.YELLOW));
		}
		return lines;
	}
}

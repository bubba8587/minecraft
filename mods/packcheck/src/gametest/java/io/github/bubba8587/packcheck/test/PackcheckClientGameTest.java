// dte:A4
package io.github.bubba8587.packcheck.test;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client game test (./gradlew :packcheck:runClientGameTest): puts the assembled result of every
 * bubbas_crafting_tweaks recipe in a survival player's inventory, opens the inventory, and screenshots it once
 * as an overview and once hovering each item with a custom name or lore, so the tooltip shows.
 * Screenshots: build/run/clientGameTest/screenshots/. specs/pack-testing.md.
 */
@SuppressWarnings("UnstableApiUsage")
public class PackcheckClientGameTest implements FabricClientGameTest {
	/** Results whose tooltips are worth a close look: custom item_name or lore. Screenshot name, item. */
	private static final List<Object[]> HOVERS = List.of(
		new Object[] {"bamboo_paper", Items.PAPER},
		new Object[] {"charcoal_block", Items.COAL_BLOCK},
		new Object[] {"synthetic_diamond_ore", Items.DEEPSLATE_DIAMOND_ORE},
		new Object[] {"organic_glowstone_dust", Items.GLOWSTONE_DUST},
		new Object[] {"rotten_flesh_leather", Items.LEATHER}
	);

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();

			int given = singleplayer.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().clearContent();
				List<ItemStack> results = PackcheckGameTest.assembledResults(server.overworld());
				results.forEach(stack -> player.getInventory().add(stack.copy()));
				player.getInventory().setItem(40, new ItemStack(Items.PAPER, 3)); // offhand: searched after the hotbar, so hover checks still find Bamboo Paper first // plain paper, to compare with Bamboo Paper
				return results.size();
			});
			if (given != PackcheckGameTest.recipeCaseCount()) {
				throw new AssertionError("expected " + PackcheckGameTest.recipeCaseCount() + " assembled recipe results, got " + given);
			}
			context.waitTicks(5);

			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			context.waitTicks(2);
			context.getInput().setCursorPos(5, 5);
			context.waitTick();
			context.takeScreenshot("packcheck_crafting_tweaks_inventory");

			for (Object[] hover : HOVERS) {
				String name = (String) hover[0];
				Item item = (Item) hover[1];
				double[] pos = context.computeOnClient(mc -> slotCenterInWindow(mc, item));
				context.getInput().setCursorPos(pos[0], pos[1]);
				context.waitTicks(2);
				String hovered = context.computeOnClient(PackcheckClientGameTest::hoveredItem);
				if (!hovered.equals(item.toString())) {
					throw new AssertionError("hovering " + name + " but the screen's hovered slot holds " + hovered);
				}
				context.takeScreenshot("packcheck_tooltip_" + name);
			}
			context.setScreen(() -> null);
		}
	}

	/** Window pixel coordinates of the centre of the inventory slot holding this item. */
	private static double[] slotCenterInWindow(Minecraft mc, Item item) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
		Slot slot = screen.getMenu().slots.stream().filter(s -> s.getItem().is(item)).findFirst()
			.orElseThrow(() -> new AssertionError(item + " is not in the open inventory"));
		double scale = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
		int left = intField(screen, "leftPos");
		int top = intField(screen, "topPos");
		return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
	}

	private static String hoveredItem(Minecraft mc) {
		try {
			Field f = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
			f.setAccessible(true);
			Slot slot = (Slot) f.get(mc.gui.screen());
			return slot == null ? "nothing" : slot.getItem().getItem().toString();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// leftPos/topPos are protected; Minecraft 26.x is unobfuscated, so reflection by Mojang name is stable.
	private static int intField(AbstractContainerScreen<?> screen, String name) {
		try {
			Field f = AbstractContainerScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}

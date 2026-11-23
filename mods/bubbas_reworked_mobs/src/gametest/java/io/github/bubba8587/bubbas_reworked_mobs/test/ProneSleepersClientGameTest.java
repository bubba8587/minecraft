// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperItem;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Face-down sleepers by eye (specs/undead.md "Sleeper poses"; client part {@code prone}): every kind face down in a
 * row beside a lying and a sitting one (prone-row, prone-low for floor clipping, prone-close), and the item tooltip
 * of a face-down statue with no raw block-state lines (prone-tooltip).
 */
@SuppressWarnings("UnstableApiUsage")
public class ProneSleepersClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("prone")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY();
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 10, x + 10, y + 6, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 10, y - 1, z - 10, x + 10, y - 1, z + 10));

			// Row (z - 4), heads north: the five kinds face down, two blocks apart; then a lying and a sitting skeleton.
			UndeadVariant[] kinds = UndeadVariant.values();
			for (int i = 0; i < kinds.length; i++) {
				server.runCommand("setblock %d %d %d bubbas_reworked_mobs:%s[pose=prone,facing=south]".formatted(x - 6 + i * 2, y, z - 4, kinds[i].sleeperId()));
			}
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:skeleton_sleeper[pose=lying,facing=south]".formatted(x + 5, y, z - 4));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:skeleton_sleeper[pose=sitting,facing=south]".formatted(x + 7, y, z - 4));
			// An armoured face-down zombie.
			BlockPos armoured = new BlockPos(x - 6, y, z + 1);
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:zombie_sleeper[pose=prone,facing=east]".formatted(armoured.getX(), armoured.getY(), armoured.getZ()));
			server.runOnServer(s -> {
				if (s.overworld().getBlockEntity(armoured) instanceof io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity sleeper) {
					sleeper.setGear(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
					sleeper.setGear(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
				}
			});
			context.waitTicks(10);

			tp(context, server, x + 0.5, y + 4.0, z + 3.0, 180, 40);
			context.takeScreenshot("prone-row");
			tp(context, server, x - 9.5, y + 0.35, z - 4.0, -90, 0);
			context.takeScreenshot("prone-low");
			tp(context, server, x - 4.5, y + 2.2, z - 1.5, 160, 50);
			context.takeScreenshot("prone-close");
			tp(context, server, x - 4.0, y + 1.6, z + 1.5, 120, 35);
			context.takeScreenshot("prone-armoured");

			// The tooltip of a face-down statue in the survival inventory.
			server.runCommand("gamemode survival @p");
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				p.getInventory().clearContent();
				p.getInventory().setItem(0, SleeperItem.form(ModItems.sleeper(UndeadVariant.SKELETON), SleeperPose.PRONE, true));
			});
			context.waitTicks(5);
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitTicks(5);
			double[] at = context.computeOnClient(mc -> {
				InventoryScreen screen = (InventoryScreen) mc.gui.screen();
				Slot slot = screen.getMenu().getSlot(36);
				int left = (screen.width - 176) / 2;
				int top = (screen.height - 166) / 2;
				double scale = mc.getWindow().getGuiScale();
				return new double[] {(left + slot.x + 8) * scale, (top + slot.y + 8) * scale};
			});
			context.getInput().setCursorPos(at[0], at[1]);
			context.waitTicks(3);
			context.takeScreenshot("prone-tooltip");
			context.runOnClient(mc -> mc.gui.setScreen(null));
		}
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(4);
	}
}

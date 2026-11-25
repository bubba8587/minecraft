// dte:C1,B15,B16
package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Eye of the Soul by eye (specs/soulfire.md "Eye of the Soul: phasing"; client part {@code phasing}). A stone cube on a
 * blackstone platform in the sky, with a lit gold-and-glowstone room inside it. Shots: phasing-in-stone (eye inside
 * the stone, looking at the room 3 blocks ahead), phasing-in-stone-night-vision (the same with Night Vision),
 * phasing-room (inside the room), phasing-room-night-vision, phasing-outside (on the platform, looking at the cube),
 * phasing-reference (the room without Phasing), phasing-icons and phasing-effect (hotbar eye, the inventory effect list);
 * those again with the 32x pack (-32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class PhasingClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("phasing")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY() + 20;
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 2, z - 8));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:polished_blackstone".formatted(x - 8, y - 1, z - 12, x + 8, y - 1, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 4, y, z - 3, x + 4, y + 6, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 2, y + 1, z + 3, x + 2, y + 4, z + 7));
			server.runCommand("fill %d %d %d %d %d %d minecraft:gold_block".formatted(x - 2, y + 1, z + 7, x + 2, y + 4, z + 7));
			server.runCommand("setblock %d %d %d minecraft:glowstone".formatted(x, y + 2, z + 7));
			server.runCommand("setblock %d %d %d minecraft:diamond_block".formatted(x - 1, y + 1, z + 6));
			server.runCommand("setblock %d %d %d minecraft:wall_torch[facing=south]".formatted(x - 2, y + 3, z + 4));
			server.runCommand("gamemode survival @p");
			server.runCommand("effect give @p minecraft:resistance infinite 4 true");
			server.runCommand("effect give @p minecraft:regeneration infinite 4 true");
			server.runCommand("effect give @p minecraft:saturation infinite 0 true");
			context.waitTicks(10);

			// --- reference: the room without Phasing --------------------------------------------------------
			tp(context, server, x + 0.5, y + 1, z + 3.5, 0.0F, 10.0F);
			context.waitTicks(10);
			context.takeScreenshot("phasing-reference");

			server.runCommand("effect give @p bubbas_reworked_mobs:phasing infinite 0 true");
			context.waitFor(mc -> mc.player.hasEffect(io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing.PHASING));
			// Eye inside the stone, 3 blocks short of the room
			tp(context, server, x + 0.5, y + 1, z + 0.5, 0.0F, 10.0F);
			context.waitTicks(10);
			context.takeScreenshot("phasing-in-stone");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			context.waitTicks(5);
			context.takeScreenshot("phasing-in-stone-night-vision");
			server.runCommand("effect clear @p minecraft:night_vision");
			// In the room
			tp(context, server, x + 0.5, y + 1, z + 3.5, 0.0F, 10.0F);
			context.waitTicks(10);
			context.takeScreenshot("phasing-room");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			context.waitTicks(5);
			context.takeScreenshot("phasing-room-night-vision");
			server.runCommand("effect clear @p minecraft:night_vision");
			// Outside, looking at the cube
			tp(context, server, x + 0.5, y, z - 8.5, 0.0F, 5.0F);
			context.waitTicks(10);
			context.takeScreenshot("phasing-outside");

			// --- icons: the eye in the hotbar, the Phasing effect in the inventory --------------------------
			server.runCommand("item replace entity @p hotbar.0 with bubbas_reworked_mobs:eye_of_the_soul 16");
			server.runCommand("item replace entity @p hotbar.1 with bubbas_reworked_mobs:soul_blaze_powder");
			server.runCommand("item replace entity @p hotbar.2 with minecraft:ender_eye");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			icons(context, "");
			setPack(context, true);
			icons(context, "-32x");
			setPack(context, false);

			// --- fade-out: 3 s of Phasing in the room, shot partway through its last 2 s ---------------------
			context.setScreen(() -> null);
			server.runCommand("effect clear @p bubbas_reworked_mobs:phasing");
			server.runCommand("effect give @p bubbas_reworked_mobs:phasing 3 0 true");
			context.waitFor(mc -> mc.player.hasEffect(io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing.PHASING));
			tp(context, server, x + 0.5, y + 1, z + 3.5, 0.0F, 10.0F);
			context.waitTicks(30);
			context.takeScreenshot("phasing-fading");
			context.waitTicks(20);
			context.takeScreenshot("phasing-fading-late");
		}
	}

	private static void icons(ClientGameTestContext context, String suffix) {
		context.takeScreenshot("phasing-icons" + suffix);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("phasing-effect" + suffix);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(3);
	}

	private static void setPack(ClientGameTestContext context, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (repo.getSelectedIds().contains(PACK_32X) == on) {
				return false;
			}
			if (on) {
				repo.addPack(PACK_32X);
			} else {
				repo.removePack(PACK_32X);
			}
			mc.options.updateResourcePacks(repo);
			return true;
		});
		if (changed) {
			context.waitTicks(2);
			context.waitFor(mc -> mc.gui.overlay() == null, 1200);
			context.waitTicks(5);
		}
	}
}

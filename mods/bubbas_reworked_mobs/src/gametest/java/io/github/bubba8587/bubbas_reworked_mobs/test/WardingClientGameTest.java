// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Warding, by eye (client test part {@code warding}): the Totem of Warding in hand and in the hotbar; a pulse (soul-fire
 * ring, the item cooldown sweep on the slot) from first and third person; a sleeper lit by torches at night.
 * Screenshots in build/run/clientGameTest/screenshots/.
 */
@SuppressWarnings("UnstableApiUsage")
public class WardingClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_reworked_mobs";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("warding")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(base.getX() - 8, base.getY(), base.getZ() - 8, base.getX() + 8, base.getY() + 6, base.getZ() + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate_tiles".formatted(base.getX() - 8, base.getY() - 1, base.getZ() - 8, base.getX() + 8, base.getY() - 1, base.getZ() + 8));
			server.runCommand("tp @p %d.5 %d %d.5 0 25".formatted(base.getX(), base.getY(), base.getZ()));

			// The item: in the off hand only first (no pulse sweep yet: the totem goes on cooldown as soon as it's held),
			// so the icon shows in the hotbar plainly from slot 2 while the main hand is empty.
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().setItem(1, new ItemStack(ModItems.TOTEM_OF_WARDING)));
			context.waitTicks(10);
			context.takeScreenshot("warding-item-hotbar");

			// Held: the first pulse fires at once; the ring flies out and the slot shows vanilla's cooldown sweep.
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(1));
			context.waitTicks(3);
			context.takeScreenshot("warding-pulse-first-person");
			context.waitTicks(60);
			context.takeScreenshot("warding-cooldown-sweep");
			// Next pulse (10 s after the first) from the front, in third person.
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitFor(mc -> !mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem()), 300);
			context.waitTicks(4);
			context.takeScreenshot("warding-pulse-third-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));

			// A sleeper lit by torches, at night.
			server.runCommand("time set midnight");
			BlockPos s = base.offset(0, 0, 4);
			server.runCommand("setblock %d %d %d %s:zombie_sleeper[facing=north,pose=sitting]".formatted(s.getX(), s.getY(), s.getZ(), ID));
			server.runCommand("setblock %d %d %d minecraft:torch".formatted(s.getX() - 1, s.getY(), s.getZ()));
			server.runCommand("setblock %d %d %d minecraft:torch".formatted(s.getX() + 1, s.getY(), s.getZ()));
			server.runCommand("setblock %d %d %d minecraft:torch".formatted(s.getX() + 1, s.getY(), s.getZ() + 1));
			context.waitFor(mc -> mc.level.getBlockState(s).is(ModBlocks.sleeper(UndeadVariant.ZOMBIE)));
			server.runCommand("tp @p %d.5 %d %d.5 0 20".formatted(base.getX(), base.getY(), base.getZ() + 1));
			context.waitTicks(20);
			context.takeScreenshot("warding-sleeper-near-torches");
		}
	}
}

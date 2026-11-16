// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModBlocks;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Warding, by eye (client test part {@code warding}): the Totem of Warding in hand and in the hotbar; a pulse (soul-fire
 * ring, the item cooldown sweep on the slot) from first and third person; a Totem of Warding statue.
 * Screenshots in build/run/clientGameTest/screenshots/.
 */
@SuppressWarnings("UnstableApiUsage")
public class WardingClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_totems_artifacts";

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

			// A Totem of Warding set down as a statue, two blocks south, facing north at the player; then from the side
			// and up close. (The torch-lit sleeper shot stayed with Reworked Mobs' light calming.)
			// Wait out the last pulse's particles first.
			context.waitTicks(60);
			BlockPos s = base.offset(0, 0, 2);
			server.runCommand("setblock %d %d %d %s:totem_statue[facing=north]".formatted(s.getX(), s.getY(), s.getZ(), ID));
			context.waitFor(mc -> mc.level.getBlockState(s).is(ModBlocks.TOTEM_STATUE));
			statueShots(context, server, base, "");
			// The same in the built-in 32x pack (B16).
			setPack(context, PACK_32X, true);
			statueShots(context, server, base, "-32x");
			setPack(context, PACK_32X, false);
		}
	}

	private static final String PACK_32X = ID + ":" + ID + "_32x";

	private static void statueShots(ClientGameTestContext context, TestServerContext server, BlockPos base, String suffix) {
		server.runCommand("tp @p %d.5 %d %d.5 0 20".formatted(base.getX(), base.getY(), base.getZ()));
		context.waitTicks(10);
		context.takeScreenshot("warding-statue-front" + suffix);
		server.runCommand("tp @p %d.5 %d %d.5 -40 30".formatted(base.getX() - 2, base.getY(), base.getZ()));
		context.waitTicks(10);
		context.takeScreenshot("warding-statue-angle" + suffix);
		server.runCommand("tp @p %d.5 %d.2 %d.9 0 30".formatted(base.getX(), base.getY(), base.getZ()));
		context.waitTicks(10);
		context.takeScreenshot("warding-statue-close" + suffix);
	}

	/** Turns a resource pack on or off and waits for the reload. */
	private static void setPack(ClientGameTestContext context, String id, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (!repo.getAvailableIds().contains(id)) {
				throw new AssertionError("no resource pack " + id + "; available: " + repo.getAvailableIds());
			}
			if (repo.getSelectedIds().contains(id) == on) {
				return false;
			}
			if (on) {
				repo.addPack(id);
			} else {
				repo.removePack(id);
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

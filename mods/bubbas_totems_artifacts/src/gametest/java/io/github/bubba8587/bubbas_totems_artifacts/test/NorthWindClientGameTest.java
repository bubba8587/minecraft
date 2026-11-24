// dte:C6,B16
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.Locale;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Totem of the North Wind by eye (client part {@code northwind}): held in first person and in the hotbar
 * (northwind-held), its cooldown sweep (northwind-cooldown), its statue beside the Clear Skies statue from the front
 * and the side (northwind-statue, -statue-side), each at 32x too (-32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class NorthWindClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_totems_artifacts:bubbas_totems_artifacts_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("northwind")) {
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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 8, x + 8, y + 5, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y - 1, z - 8, x + 8, y - 1, z + 8));
			server.runCommand("setblock %d %d %d bubbas_totems_artifacts:totem_statue[facing=south,totem=north_wind]".formatted(x, y, z - 4));
			server.runCommand("setblock %d %d %d bubbas_totems_artifacts:totem_statue[facing=south,totem=clear_skies]".formatted(x - 2, y, z - 4));
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				p.getInventory().setItem(0, new ItemStack(ModItems.TOTEM_OF_NORTH_WIND));
				p.getInventory().setItem(1, new ItemStack(ModItems.TOTEM_OF_CLEAR_SKIES));
			});
			shots(context, server, x, y, z, "");
			setPack(context, true);
			shots(context, server, x, y, z, "-32x");
			setPack(context, false);
		}
	}

	private static void shots(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String suffix) {
		server.runCommand("gamemode creative @p");
		tp(context, server, x + 0.5, y, z + 2.5, 200, 20);
		context.takeScreenshot("northwind-held" + suffix);
		server.runOnServer(s -> {
			ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
			p.getCooldowns().addCooldown(p.getInventory().getItem(0), 200);
		});
		context.waitTicks(40);
		context.takeScreenshot("northwind-cooldown" + suffix);
		server.runCommand("gamemode spectator @p");
		tp(context, server, x - 0.5, y - 0.4, z - 1.6, 180, 12);
		context.takeScreenshot("northwind-statue" + suffix);
		tp(context, server, x - 1.6, y - 0.3, z - 2.4, -115, 12);
		context.takeScreenshot("northwind-statue-side" + suffix);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
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

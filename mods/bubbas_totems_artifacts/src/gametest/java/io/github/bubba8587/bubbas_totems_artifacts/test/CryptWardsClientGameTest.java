// dte:C6,C1
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.Locale;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Reworked Mobs' crypts with their warding statues by eye (client part {@code crypts}, needs -PwithMobs): a family
 * vault (crypt-wards-family), a noble crypt's chamber (crypt-wards-noble) and both side halls
 * (crypt-wards-noble-west, -east), each built from a seed with a statue in every wing, with night vision.
 */
@SuppressWarnings("UnstableApiUsage")
public class CryptWardsClientGameTest implements FabricClientGameTest {
	private static final int PLATFORM_Y = 150;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("crypts") || !TestSupport.withMobs()) {
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
			server.runCommand("gamerule max_command_modification_block_count 100000");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			Mobs.shoot(context, sp, server, base.getX(), base.getZ(), false);
			Mobs.shoot(context, sp, server, base.getX() + 64, base.getZ(), true);
		}
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}

	/** Reworked Mobs' side; only loaded with that mod. */
	private static final class Mobs {
		private Mobs() {
		}

		static void shoot(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, int x, int z, boolean noble) {
			var wealth = noble ? io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.NOBLE
					: io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.FAMILY;
			int wings = noble ? 3 : 1;
			tp(context, server, x + 0.5, PLATFORM_Y + 30, z - 20.5, 0, 50);
			context.waitTicks(10);
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 20, PLATFORM_Y - 12, z - 4, x + 20, PLATFORM_Y - 1, z + 50));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 20, PLATFORM_Y, z - 4, x + 20, PLATFORM_Y, z + 50));
			BlockPos gate = new BlockPos(x, PLATFORM_Y + 1, z);
			var piece = server.computeOnServer(s -> {
				long seed = 1;
				while (new io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece(seed, gate, 19, 19, true,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardTrees.forBiome(s.overworld().getBiome(gate)),
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.KEPT, wealth,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Climate.TEMPERATE).wardSpots().size() < wings) {
					seed++;
				}
				return io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand.build(s.overworld(), gate, seed, 19, 19, true,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.KEPT, wealth);
			});
			context.waitTicks(10);
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			String name = "crypt-wards-" + (noble ? "noble" : "family");
			var spots = piece.wardSpots();
			BlockPos first = spots.getFirst().pos();
			tp(context, server, first.getX() + 0.5, first.getY() + 0.6, first.getZ() - 3.5, 0, 12);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(name);
			if (noble) {
				BlockPos west = spots.get(1).pos();
				tp(context, server, west.getX() + 3.5, west.getY() + 0.6, west.getZ() + 0.5, 90, 12);
				context.takeScreenshot(name + "-west");
				BlockPos east = spots.get(2).pos();
				tp(context, server, east.getX() - 2.5, east.getY() + 0.6, east.getZ() + 0.5, -90, 12);
				context.takeScreenshot(name + "-east");
			}
			server.runCommand("effect clear @p");
		}
	}
}

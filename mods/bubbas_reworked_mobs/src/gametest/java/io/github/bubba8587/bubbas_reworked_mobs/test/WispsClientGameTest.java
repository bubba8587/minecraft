// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand;
import io.github.bubba8587.bubbas_reworked_mobs.grave.Wisps;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Will-o-wisps by eye (client part {@code wisps}): at night, from outside a yard, wisps over the graves and a hint. */
@SuppressWarnings("UnstableApiUsage")
public class WispsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("wisps")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 18000");
			CryptYardPiece piece = server.computeOnServer(s -> GraveyardCommand.build(s.overworld(), base.offset(0, 0, 14), true, 61L));
			BlockPos gate = piece.gate();
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f 0 15", gate.getX() + 0.5, gate.getY() + 3.0, gate.getZ() - 16.5));
			context.waitTicks(20);
			for (int shot = 0; shot < 3; shot++) {
				server.runOnServer(s -> {
					var level = s.overworld();
					var player = s.getPlayerList().getPlayers().getFirst();
					var yard = Wisps.yards(level).get(ChunkPos.containing(gate).pack());
					for (int i = 0; i < 25; i++) {
						Wisps.overGraves(level, player, yard, level.getRandom());
					}
					for (int i = 0; i < 4; i++) {
						Wisps.hint(level, player, yard, level.getRandom());
					}
				});
				context.waitTicks(8);
				context.takeScreenshot("wisps-" + shot);
			}
		}
	}
}

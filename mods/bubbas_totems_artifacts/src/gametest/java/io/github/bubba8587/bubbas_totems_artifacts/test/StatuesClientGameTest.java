// dte:C6,B16
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.Locale;

import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Every totem's statue by eye (client part {@code statues}): all of them in a row on a stone floor, facing the camera
 * (statues-row), two at a time close up (statues-close-0..5), at night to show the glowing
 * eyes and flames (statues-night), and the same with the 32x pack (-32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class StatuesClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_totems_artifacts:bubbas_totems_artifacts_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("statues")) {
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
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 6, x + 12, y + 4, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 10, y - 1, z - 6, x + 12, y - 1, z + 8));
			TotemType[] types = TotemType.values();
			for (int i = 0; i < types.length; i++) {
				server.runCommand("setblock %d %d %d bubbas_totems_artifacts:totem_statue[facing=south,totem=%s]".formatted(
						x - 9 + 2 * i, y, z - 3, types[i].getSerializedName()));
			}
			context.waitTicks(10);
			shots(context, server, x, y, z, "");
			setPack(context, true);
			shots(context, server, x, y, z, "-32x");
			setPack(context, false);
		}
	}

	private static void shots(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String suffix) {
		server.runCommand("time set noon");
		tp(context, server, x + 1.5, y + 1.0, z + 7.5, 180, 15);
		context.takeScreenshot("statues-row" + suffix);
		// Close up, two at a time
		for (int pair = 0; pair < 6; pair++) {
			tp(context, server, x - 7.5 + 4 * pair, y - 0.75, z - 0.6, 180, 12);
			context.takeScreenshot("statues-close-" + pair + suffix);
		}
		server.runCommand("time set 18000");
		tp(context, server, x + 0.5, y + 1.0, z + 6.5, 180, 15);
		context.takeScreenshot("statues-night" + suffix);
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

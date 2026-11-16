// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The look of an Ancient Fabricator (mob factory) (specs/mob-factory.md; client part {@code factories}): builds factories with the op
 * command deep under a real (default-preset) world and photographs them. A zombie factory from many angles
 * (factory-overview, -overview-lit with night vision, -side, -ceiling, -ceiling-close, -platform, -face, -warden,
 * -beam, -tower), then one wall and overview per other mob (factory-face-creeper, ...). Run with
 * {@code -PwithBeacons} to see the paired beam (Reworked Beacons on the classpath).
 */
@SuppressWarnings("UnstableApiUsage")
public class FactoriesClientGameTest implements FabricClientGameTest {
	private static final int DEPTH_Y = -38;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("factories")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
					s.setSeed("20261116");
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set midnight");
			server.runCommand("gamemode spectator @p");
			BlockPos spawn = context.computeOnClient(mc -> mc.player.blockPosition());

			String[] mobs = { "zombie", "creeper", "spider", "skeleton", "enderman" };
			for (int i = 0; i < mobs.length; i++) {
				BlockPos o = new BlockPos(spawn.getX() + i * 160, DEPTH_Y, spawn.getZ());
				tp(context, server, o.getX() + 0.5, o.getY() + 6, o.getZ() + 16.5, 180.0F, 8.0F);
				server.runCommand("bubbas_reworked_mobs ancient_fabricator build %d %d %d %s %d 0".formatted(o.getX(), o.getY(), o.getZ(), mobs[i], 1000L + i));
				context.waitTicks(30);
				sp.getConnection().waitForChunksRender();
				context.waitTicks(20);
				if (i == 0) {
					fullTour(context, sp, server, o);
				} else {
					faceShot(context, server, o, "factory-face-" + mobs[i]);
					shot(context, server, o, 0.5, 9.0, 18.5, 180.0F, 12.0F, "factory-overview-" + mobs[i]);
				}
			}
		}
	}

	private static void fullTour(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, BlockPos o) {
		shot(context, server, o, 0.5, 9.0, 18.5, 180.0F, 12.0F, "factory-overview");
		shot(context, server, o, 6.5, 6.0, 12.5, 150.0F, -8.0F, "factory-side");
		shot(context, server, o, 0.5, 1.8, 3.5, 180.0F, -89.0F, "factory-dripper-below");
		shot(context, server, o, -13.5, 9.0, 3.5, -90.0F, -6.0F, "factory-dripper-side");
		shot(context, server, o, 0.5, 1.5, 10.5, 180.0F, -38.0F, "factory-dripper-pad");
		shot(context, server, o, 0.5, 5.0, 12.5, 180.0F, 30.0F, "factory-platform");
		shot(context, server, o, -3.5, 2.6, -3.5, 150.0F, 38.0F, "factory-sculk-deck");
		shot(context, server, o, 6.5, 3.0, 11.5, -140.0F, 18.0F, "factory-sculk-stairs");
		shot(context, server, o, -9.5, -2.0, 13.5, 140.0F, 25.0F, "factory-sculk-floor");
		// The teleporter pad mid-count, its particles running (they run even with nothing on it: a player could ride it).
		tp(context, server, o.getX() + 0.5, o.getY() + 3.5, o.getZ() + 9.5, 180.0F, 35.0F);
		context.waitTicks(10);
		for (int i = 0; i < 4; i++) {
			server.runCommand("particle minecraft:reverse_portal %d.5 %d.2 %d.5 0.7 1.5 0.7 0.02 120 force".formatted(o.getX(), o.getY(), o.getZ() + 3));
			server.runCommand("particle minecraft:portal %d.5 %d.5 %d.5 1.0 0.2 1.0 0.4 80 force".formatted(o.getX(), o.getY(), o.getZ() + 3));
			context.waitTicks(4);
		}
		context.takeScreenshot("factory-pad");
		faceShot(context, server, o, "factory-face");
		shot(context, server, o, 8.5, 8.0, 0.5, -90.0F, 4.0F, "factory-warden");
		shot(context, server, o, -3.5, 7.0, 7.5, -125.0F, 8.0F, "factory-warden-wide");
		shot(context, server, o, 15.3, 8.6, -2.6, -42.0F, 32.0F, "factory-office");
		shot(context, server, o, 2.5, 3.0, 4.5, 122.0F, -35.0F, "factory-beam");
		shot(context, server, o, -6.5, 8.0, 10.5, 240.0F, 10.0F, "factory-tower");
		server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
		shot(context, server, o, 0.5, 9.0, 18.5, 180.0F, 12.0F, "factory-overview-lit");
		shot(context, server, o, 6.5, 6.0, 12.5, 150.0F, -8.0F, "factory-side-lit");
		shot(context, server, o, 3.5, 3.0, 9.5, 130.0F, -28.0F, "factory-chains-lit");
		shot(context, server, o, 8.5, 6.0, 8.5, 90.0F, -12.0F, "factory-chains-lit-2");
		server.runCommand("effect clear @p");
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, BlockPos o, double x, double y, double z, float yaw,
			float pitch, String name) {
		tp(context, server, o.getX() + x, o.getY() + y, o.getZ() + z, yaw, pitch);
		context.waitTicks(10);
		context.takeScreenshot(name);
	}

	/** The face wall square on, from close in front (under the short spikes) with a wide field of view. */
	private static void faceShot(ClientGameTestContext context, TestServerContext server, BlockPos o, String name) {
		context.runOnClient(mc -> mc.options.fov().set(95));
		shot(context, server, o, 0.5, 8.5, -2.5, 180.0F, 0.0F, name);
		context.runOnClient(mc -> mc.options.fov().set(70));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}

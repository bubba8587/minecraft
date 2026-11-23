// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.Locale;

import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand;
import io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Every kind of crypt yard by eye (specs/graveyards.md; client part {@code yards}): each state of breakdown, a lone
 * family plot, a small yard over a pauper's ossuary, a medium one over a family vault and a large one over a noble
 * crypt, each built on its own flat platform of grass on stone high in the sky (so the crypts fit and nothing hides
 * them). Shots: yard-<kind>-above and yard-<kind>-gate for each, and crypt-<wealth> (with night vision) and
 * crypt-<wealth>-dark (candlelight only) inside each crypt.
 */
@SuppressWarnings("UnstableApiUsage")
public class YardKindsClientGameTest implements FabricClientGameTest {
	private record Kind(String name, YardKinds.Size size, YardKinds.State state, YardKinds.Wealth wealth) {
	}

	private static final List<Kind> KINDS = List.of(
			new Kind("kept", YardKinds.Size.MEDIUM, YardKinds.State.KEPT, null),
			new Kind("overgrown", YardKinds.Size.MEDIUM, YardKinds.State.OVERGROWN, null),
			new Kind("sunken", YardKinds.Size.MEDIUM, YardKinds.State.SUNKEN, null),
			new Kind("collapsed", YardKinds.Size.MEDIUM, YardKinds.State.COLLAPSED, null),
			new Kind("nearly-gone", YardKinds.Size.MEDIUM, YardKinds.State.NEARLY_GONE, null),
			new Kind("lone", YardKinds.Size.LONE, YardKinds.State.KEPT, null),
			new Kind("pauper", YardKinds.Size.SMALL, YardKinds.State.OVERGROWN, YardKinds.Wealth.PAUPER),
			new Kind("family", YardKinds.Size.MEDIUM, YardKinds.State.OVERGROWN, YardKinds.Wealth.FAMILY),
			new Kind("noble", YardKinds.Size.LARGE, YardKinds.State.KEPT, YardKinds.Wealth.NOBLE));
	private static final int SPACING = 64;
	private static final int PLATFORM_Y = 150;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("yards")) {
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
			for (int i = 0; i < KINDS.size(); i++) {
				Kind kind = KINDS.get(i);
				int x = base.getX() + i * SPACING;
				int z = base.getZ();
				tp(context, server, x + 0.5, PLATFORM_Y + 30, z - 20.5, 0, 50);
				context.waitTicks(10);
				// The platform: 12 blocks of stone under a layer of grass, wide enough for the largest yard and crypt.
				server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 20, PLATFORM_Y - 12, z - 4, x + 20, PLATFORM_Y - 1, z + 50));
				server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 20, PLATFORM_Y, z - 4, x + 20, PLATFORM_Y, z + 50));
				BlockPos gate = new BlockPos(x, PLATFORM_Y + 1, z);
				CryptYardPiece piece = server.computeOnServer(s -> GraveyardCommand.build(s.overworld(), gate, 1000L + kind.hashCode(), kind.size().maxSize(),
						kind.size().maxSize(), kind.wealth() != null, kind.state(), kind.wealth() == null ? YardKinds.Wealth.FAMILY : kind.wealth()));
				System.out.println("[yards] " + kind.name() + ": " + piece.width() + " x " + piece.depth() + ", " + piece.state() + (piece.hasCrypt() ? ", " + piece.wealth() + " crypt" : ""));
				context.waitTicks(10);
				BlockPos g = piece.gate();
				tp(context, server, g.getX() + 0.5, g.getY() + 18 + piece.depth() / 2.0, g.getZ() - 8.5, 0, 55);
				sp.getConnection().waitForChunksRender();
				context.takeScreenshot("yard-" + kind.name() + "-above");
				tp(context, server, g.getX() + 0.5, g.getY() + 1.6, g.getZ() - 3.5, 0, 18);
				context.takeScreenshot("yard-" + kind.name() + "-gate");
				if (piece.hasCrypt()) {
					BlockPos v = piece.vaultCentre();
					String wealth = piece.wealth().name().toLowerCase(Locale.ROOT);
					server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
					tp(context, server, v.getX() + 0.5, v.getY() + 0.3, v.getZ() - 3.5, 0, 8);
					context.takeScreenshot("crypt-" + wealth);
					if (piece.wealth() == YardKinds.Wealth.NOBLE) {
						tp(context, server, v.getX() + 4.5, v.getY() + 0.3, v.getZ() + 0.5, -90, 5);
						context.takeScreenshot("crypt-noble-hall");
					}
					server.runCommand("effect clear @p");
					tp(context, server, v.getX() + 0.5, v.getY() + 0.3, v.getZ() - 3.5, 0, 8);
					context.takeScreenshot("crypt-" + wealth + "-dark");
				}
			}
		}
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}

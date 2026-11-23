// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnDesign;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Cairn variety by eye (specs/endermen.md, "Cairn designs"; client part {@code cairnforms}): one finished cairn of every
 * form and a few more seeds, built side by side on a flat world, each photographed from the same angle
 * (cairn-form-<form>-<seed>), plus an overview of the row (cairn-forms-row). Prints each one's form, block count and
 * End-block count.
 */
@SuppressWarnings("UnstableApiUsage")
public class CairnFormsClientGameTest implements FabricClientGameTest {
	private static final int GAP = 24;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("cairnforms")) {
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
			server.runCommand("gamerule bubbas_reworked_mobs:cairn_endermen false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");

			// One seed per form (the first that picks it), then three more seeds of the common forms.
			List<Long> seeds = new ArrayList<>();
			for (CairnDesign.Form form : CairnDesign.Form.values()) {
				for (long seed = 1; seed < 10_000; seed++) {
					if (CairnDesign.formOf(seed * 104729L) == form) {
						seeds.add(seed * 104729L);
						break;
					}
				}
			}
			seeds.add(2026L);
			seeds.add(1116L);
			seeds.add(777L);

			for (int i = 0; i < seeds.size(); i++) {
				long seed = seeds.get(i);
				BlockPos origin = base.offset(i * GAP, 0, 0);
				// Make the spot flat grass first, and load it.
				server.runCommand("tp @p %d %d %d".formatted(origin.getX(), origin.getY() + 20, origin.getZ() - 12));
				context.waitTicks(10);
				server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(origin.getX() - 10, origin.getY(), origin.getZ() - 10,
						origin.getX() + 10, origin.getY() + 24, origin.getZ() + 10));
				server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(origin.getX() - 10, origin.getY() - 1, origin.getZ() - 10,
						origin.getX() + 10, origin.getY() - 1, origin.getZ() + 10));
				String info = server.computeOnServer(s -> {
					CairnSite site = CairnSites.addSite(s.overworld(), origin, seed);
					site.buildAll(s.overworld());
					long end = site.order().stream().filter(p -> !site.at(p).is(net.minecraft.tags.BlockTags.ENDERMAN_HOLDABLE)).count();
					return CairnDesign.formOf(seed) + " seed " + seed + ": " + site.size() + " blocks, " + end + " End blocks, "
							+ (site.box().maxY() - origin.getY() + 1) + " tall, reach " + site.radius();
				});
				System.out.println("[cairnforms] " + info);
			}
			for (int i = 0; i < seeds.size(); i++) {
				long seed = seeds.get(i);
				BlockPos origin = base.offset(i * GAP, 0, 0);
				tp(context, server, origin.getX() - 9.5, origin.getY() + 7, origin.getZ() - 9.5, -45, 14); // from the north-west, so an arch never hides its legs
				sp.getConnection().waitForChunksRender();
				context.takeScreenshot("cairn-form-" + CairnDesign.formOf(seed).name().toLowerCase(Locale.ROOT) + "-" + seed);
			}
			tp(context, server, base.getX() + GAP * (seeds.size() - 1) / 2.0, base.getY() + 30, base.getZ() - 40.5, 0, 28);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("cairn-forms-row");
		}
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(20);
	}
}

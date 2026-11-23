// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Endermen in a real (default-preset, fixed-seed) world, specs/endermen.md: the three natural cairn sites nearest spawn
 * as they generate (the footing stub), and an enderman carrying a cairn block. Screenshots: cairn-N-stub,
 * enderman-carrying. Built cairns of every form are the {@code cairnforms} part (CairnFormsClientGameTest).
 */
@SuppressWarnings("UnstableApiUsage")
public class EndermenClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("endermen")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
					s.setSeed("20261116");
					s.setGenerateStructures(true);
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");

			// The three sites nearest spawn (working out every region within 1500 blocks first).
			List<CairnSite> sites = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos spawn = level.getRespawnData().pos();
				CairnSites.nearestNow(level, spawn, 1500);
				List<CairnSite> found = new ArrayList<>(CairnSites.near(level, spawn, 1500));
				found.sort(java.util.Comparator.comparingDouble(c -> c.origin().distSqr(spawn)));
				System.out.println("[endermen] " + found.size() + " cairn sites within 1500 blocks of spawn " + spawn.toShortString());
				return found;
			});
			if (sites.size() < 3) {
				throw new AssertionError("expected at least 3 cairn sites within 1500 blocks, found " + sites.size());
			}
			List<BlockPos> visited = new ArrayList<>();
			for (int n = 1; n <= 3; n++) {
				CairnSite site = sites.get(n - 1);
				visited.add(site.origin());
				shootSite(context, sp, server, n, site);
			}

			// An enderman carrying a cairn block, beside the first (open ground) cairn.
			BlockPos o = visited.getFirst();
			tp(context, server, o.getX() + 4.5, o.getY() + 6.0, o.getZ() + 1.5, 0.0F, 12.0F);
			sp.getConnection().waitForChunksRender();
			int ey = groundY(server, o.getX() + 4, o.getZ() + 5);
			server.runOnServer(s -> {
				net.minecraft.world.entity.monster.Enderman e = net.minecraft.world.entity.EntityTypes.ENDERMAN.create(s.overworld(),
						net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				e.snapTo(o.getX() + 4.5, ey, o.getZ() + 5.5, 180.0F, 0.0F);
				e.setYHeadRot(180.0F);
				e.setYBodyRot(180.0F);
				e.setNoAi(true);
				e.setPersistenceRequired();
				e.setCarriedBlock(Blocks.PURPUR_PILLAR.defaultBlockState());
				s.overworld().addFreshEntity(e);
			});
			tp(context, server, o.getX() + 4.5, ey + 2.2, o.getZ() + 1.5, 0.0F, 12.0F);
			context.waitTicks(10);
			context.takeScreenshot("enderman-carrying");
		}
	}

	private static void shootSite(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, int n, CairnSite site) {
		BlockPos o = site.origin();
		System.out.println("[endermen] site " + n + ": " + site);
		// Stand off to the south-east, looking at the spire.
		tp(context, server, o.getX() + 7.5, o.getY() + 4.0, o.getZ() + 7.5, 135.0F, 20.0F);
		sp.getConnection().waitForChunksRender();
		context.waitTicks(20);
		context.takeScreenshot("cairn-" + n + "-stub");
	}

	private static int groundY(TestServerContext server, int x, int z) {
		return server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> mc.player != null && Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01, 400);
		context.waitTicks(2);
	}
}

// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.Arrays;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanMind;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanSpawning;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * How long does a cairn take to build in real play? (owner, 2026-11-16; specs/endermen.md). Opt-in only, never part of a
 * suite: `-PclientTests=cairnsim` (`-Dbubba.cairnSimDays=N` caps the days, default 40; `-Dbubba.cairnSimDistance=D`
 * where the player stands, default 40 blocks from the cairn). A default world (seed 20261116) with normal spawning,
 * day and night and weather; a creative player stands still at that distance from the nearest natural site (its
 * worldgen stub), and the server sprints one game day at a time (/tick sprint). After each day it prints the cairn's
 * progress and its endermen ("[cairnsim]" lines in the log), and stops when the cairn is complete.
 */
@SuppressWarnings("UnstableApiUsage")
public class CairnSimClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		boolean asked = Arrays.stream(System.getProperty("bubba.clientTests", "").split(",")).map(String::trim).anyMatch("cairnsim"::equals);
		if (!asked) {
			return;
		}
		int maxDays = Integer.getInteger("bubba.cairnSimDays", 40);
		int distance = Integer.getInteger("bubba.cairnSimDistance", 40);
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
			server.runCommand("difficulty normal");
			CairnSite site = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos spawn = level.getRespawnData().pos();
				CairnSites.nearestNow(level, spawn, 1500);
				List<CairnSite> found = new java.util.ArrayList<>(CairnSites.near(level, spawn, 1500));
				found.sort(java.util.Comparator.comparingDouble(c -> c.origin().distSqr(spawn)));
				return found.getFirst();
			});
			BlockPos o = site.origin();
			// Load the site (generates its stub), then stand still `distance` blocks east of it.
			server.runCommand("tp @p %d %d %d".formatted(o.getX(), o.getY() + 2, o.getZ()));
			context.waitTicks(40);
			int px = o.getX() + distance;
			int py = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, o.getZ()));
			server.runCommand("tp @p %d.5 %d %d.5 90 0".formatted(px, py, o.getZ()));
			server.runCommand("gamerule spawn_mobs true");
			server.runCommand("gamerule advance_time true");
			server.runCommand("gamerule advance_weather true");
			server.runCommand("time set 0");
			context.waitTicks(20);
			System.out.println("[cairnsim] site " + site + ", player " + distance + " blocks east; start: " + report(server, site));
			long wallStart = System.nanoTime();
			for (int day = 1; day <= maxDays; day++) {
				server.runCommand("tick sprint 24000");
				context.waitTicks(5);
				while (server.computeOnServer(s -> s.tickRateManager().isSprinting())) {
					context.waitTicks(20);
				}
				String line = report(server, site);
				System.out.println("[cairnsim] day %d (%.0f s real): %s".formatted(day, (System.nanoTime() - wallStart) / 1e9, line));
				if (server.computeOnServer(s -> site.complete(s.overworld()))) {
					System.out.println("[cairnsim] COMPLETE after " + day + " game days");
					break;
				}
			}
		}
	}

	private static String report(TestServerContext server, CairnSite site) {
		return server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			List<Enderman> own = EndermanSpawning.endermenAt(level, site);
			long carrying = own.stream().filter(e -> e.getCarriedBlock() != null).count();
			long vigil = own.stream().filter(e -> {
				EndermanMind mind = EndermanMind.of(e);
				return mind != null && mind.job() == EndermanMind.Job.VIGIL;
			}).count();
			return "%d of %d standing (%.0f %%), %d endermen about (%d carrying, %d on vigil)".formatted(site.standing(level), site.size(),
					site.finished(level) * 100.0F, own.size(), carrying, vigil);
		});
	}
}

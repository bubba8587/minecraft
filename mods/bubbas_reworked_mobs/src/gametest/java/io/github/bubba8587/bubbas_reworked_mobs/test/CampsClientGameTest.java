// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.Camps;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Monster camps by eye (specs/monster-camps.md; client part {@code camps}).
 * <ul>
 * <li>Built on a flat world, one of each kind and size: camp-&lt;kind&gt;-above and camp-&lt;kind&gt;-close, then a
 * skeleton camp with its roster out (camp-roster).</li>
 * <li>In a real world (seed 20261116): how often camps generate (printed: camps per 1000 x 1000 blocks by kind,
 * typical distance to the nearest of each kind), and the nearest natural camps (camp-natural-N-above / -close).</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CampsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("camps")) {
			return;
		}
		built(context);
		natural(context);
	}

	private static void built(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			setup(server);
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			CampKind[] kinds = CampKind.values();
			CampTuning.Size[] sizes = { CampTuning.Size.LARGE, CampTuning.Size.MEDIUM, CampTuning.Size.SMALL, CampTuning.Size.MEDIUM, CampTuning.Size.MEDIUM };
			List<BlockPos> centres = new ArrayList<>();
			for (int i = 0; i < kinds.length; i++) {
				BlockPos at = base.offset(i * 40, 0, 0);
				int k = i;
				tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				CampPiece piece = server.computeOnServer(s -> CampCommand.build(s.overworld(), at, kinds[k], sizes[k], 100L + k));
				centres.add(piece.centre());
			}
			for (int i = 0; i < kinds.length; i++) {
				BlockPos c = centres.get(i);
				tp(context, server, c.getX() + 0.5, c.getY() + 22, c.getZ() - 20.5, 0, 45);
				context.takeScreenshot("camp-" + kinds[i].id() + "-above");
				tp(context, server, c.getX() + 9.5, c.getY() + 3.5, c.getZ() - 9.5, 45, 12);
				context.takeScreenshot("camp-" + kinds[i].id() + "-close");
			}
			// The skeleton camp's roster: a creative (not spectating) player nearby makes it appear.
			BlockPos c = centres.getFirst();
			server.runCommand("gamemode creative @p");
			tp(context, server, c.getX() + 12.5, c.getY() + 5.0, c.getZ() - 12.5, 45, 15);
			context.waitTicks(40);
			context.takeScreenshot("camp-roster");
		}
	}

	private static void natural(ClientGameTestContext context) {
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
			setup(server);
			List<BlockPos> found = server.computeOnServer(s -> survey(s.overworld()));
			for (int i = 0; i < Math.min(3, found.size()); i++) {
				BlockPos c = found.get(i);
				tp(context, server, c.getX() + 0.5, c.getY() + 40, c.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				tp(context, server, c.getX() + 0.5, c.getY() + 22, c.getZ() - 20.5, 0, 45);
				context.takeScreenshot("camp-natural-" + i + "-above");
				tp(context, server, c.getX() + 9.5, c.getY() + 9.0, c.getZ() - 9.5, 45, 32);
				context.takeScreenshot("camp-natural-" + i + "-close");
			}
		}
	}

	/**
	 * Counts the camps whose start chunk is within 2048 blocks of spawn (structure starts only: cheap), prints the
	 * density and nearest distances, and returns the nearest few camps of different kinds (centres).
	 */
	private static List<BlockPos> survey(ServerLevel level) {
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Camps.CAMP_KEY);
		var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(
				net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET, io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs.id("monster_camps")));
		RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
		long seed = level.getSeed();
		BlockPos spawn = level.getRespawnData().pos();
		int half = 2048;
		int cell = placement.spacing() * 16;
		Map<CampKind, List<BlockPos>> byKind = new EnumMap<>(CampKind.class);
		int cells = 0;
		for (int gx = Math.floorDiv(spawn.getX() - half, cell); gx <= Math.floorDiv(spawn.getX() + half, cell); gx++) {
			for (int gz = Math.floorDiv(spawn.getZ() - half, cell); gz <= Math.floorDiv(spawn.getZ() + half, cell); gz++) {
				var chunk = placement.getPotentialStructureChunk(seed, gx * placement.spacing(), gz * placement.spacing());
				cells++;
				var access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS, true);
				StructureStart start = access == null ? null : access.getStartForStructure(structure);
				if (start != null && start.isValid() && start.getPieces().getFirst() instanceof CampPiece piece) {
					byKind.computeIfAbsent(piece.kind(), k -> new ArrayList<>()).add(piece.centre());
				}
			}
		}
		double area = (2.0 * half / 1000.0) * (2.0 * half / 1000.0);
		int total = byKind.values().stream().mapToInt(List::size).sum();
		StringBuilder report = new StringBuilder("[camps] survey of %d cells over %.1f km2 round spawn: %d camps (%.1f per 1000x1000)"
				.formatted(cells, area, total, total / area));
		// Typical distance to the nearest of each kind, from a grid of points.
		for (CampKind kind : CampKind.values()) {
			List<BlockPos> camps = byKind.getOrDefault(kind, List.of());
			List<Double> nearest = new ArrayList<>();
			for (int px = -1536; px <= 1536; px += 256) {
				for (int pz = -1536; pz <= 1536; pz += 256) {
					double best = Double.MAX_VALUE;
					for (BlockPos c : camps) {
						best = Math.min(best, Math.hypot(c.getX() - (spawn.getX() + px), c.getZ() - (spawn.getZ() + pz)));
					}
					nearest.add(best);
				}
			}
			nearest.sort(Double::compare);
			double median = nearest.get(nearest.size() / 2);
			report.append("; %s %d (%.1f/km2, median nearest %s)".formatted(kind.id(), camps.size(), camps.size() / area,
					median == Double.MAX_VALUE ? "none" : Math.round(median) + " blocks"));
		}
		System.out.println(report);
		// The nearest camp of each kind, nearest first.
		List<BlockPos> picks = new ArrayList<>();
		for (CampKind kind : CampKind.values()) {
			byKind.getOrDefault(kind, List.of()).stream().min((a, b) -> Double.compare(a.distSqr(spawn), b.distSqr(spawn))).ifPresent(picks::add);
		}
		picks.sort((a, b) -> Double.compare(a.distSqr(spawn), b.distSqr(spawn)));
		System.out.println("[camps] nearest of each kind: " + picks);
		return picks;
	}

	private static void setup(TestServerContext server) {
		server.runCommand("gamerule send_command_feedback false");
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamerule advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode spectator @p");
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}

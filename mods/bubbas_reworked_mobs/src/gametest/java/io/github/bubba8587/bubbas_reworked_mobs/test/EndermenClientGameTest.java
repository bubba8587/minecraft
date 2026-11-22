// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanWork;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TargetWorld;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TerrainKinds;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.TerrainOracle;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Endermen in a real (default-preset, fixed-seed) world, specs/endermen.md. First it checks the target world against
 * the real terrain around spawn: regenerating chunks must agree with what generated, and an untouched world must give
 * endermen (almost) nothing to repair (prints the numbers). Then it visits three natural cairn sites: each as it
 * generates (the footing stub), and built in full, close and far. Last, an enderman carrying a cairn block.
 * Screenshots: cairn-N-stub, cairn-N-close, cairn-N-far, cairn-N-top, enderman-carrying.
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

			System.out.println("[endermen] " + server.computeOnServer(s -> oracleReport(s.overworld())));
			System.out.println("[endermen] " + server.computeOnServer(s -> rippleReport(s.overworld(), s.overworld().getRespawnData().pos(), "spawn")));
			// Control: a 3x3x3 hole and a 3-high dirt pillar near spawn must show up (27 fills and 3 removals).
			System.out.println("[endermen] " + server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(8, 0, 8)).below();
				for (BlockPos p : BlockPos.betweenClosed(top.offset(-1, -2, -1), top.offset(1, 0, 1))) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				}
				BlockPos pillar = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(-8, 0, -8));
				for (int y = 0; y < 3; y++) {
					level.setBlock(pillar.above(y), Blocks.DIRT.defaultBlockState(), 2);
				}
				return rippleReport(level, level.getRespawnData().pos(), "spawn after digging a hole and stacking a pillar");
			}));

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
				BlockPos beside = site.origin().offset(40, 0, 0);
				System.out.println("[endermen] " + server.computeOnServer(s -> rippleReport(s.overworld(), beside, "40 east of site " + site.origin().toShortString())));
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
			System.out.println("[endermen] " + server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				TerrainOracle.clear(level);
				TerrainOracle.PROFILE = true;
				long before = TerrainOracle.NANOS_BUILDING.get();
				long count = TerrainOracle.CHUNKS_BUILT.get();
				long wall = System.nanoTime();
				int n = 0;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (level.hasChunk((o.getX() >> 4) + dx, (o.getZ() >> 4) + dz)) {
							TerrainOracle.getNow(level, (o.getX() >> 4) + dx, (o.getZ() >> 4) + dz);
							n++;
						}
					}
				}
				return "quiet regeneration: %d chunks, %.1f ms each (worker time), %.1f ms each (wall, one at a time)".formatted(n,
						(TerrainOracle.NANOS_BUILDING.get() - before) / 1e6 / Math.max(1, TerrainOracle.CHUNKS_BUILT.get() - count),
						(System.nanoTime() - wall) / 1e6 / Math.max(1, n));
			}));
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
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			System.out.println("[endermen] site " + n + " as generated: " + site.standing(level) + " of " + site.size() + " blocks standing");
			site.buildAll(level);
		});
		context.waitTicks(5);
		int height = site.box().maxY() - o.getY();
		tp(context, server, o.getX() + 8.5, o.getY() + height * 0.45, o.getZ() + 8.5, 135.0F, 6.0F);
		context.waitTicks(10);
		context.takeScreenshot("cairn-" + n + "-close");
		tp(context, server, o.getX() + 20.5, o.getY() + height * 0.6 + 4, o.getZ() - 14.5, 55.0F, 12.0F);
		context.waitTicks(10);
		context.takeScreenshot("cairn-" + n + "-far");
		tp(context, server, o.getX() - 4.5, o.getY() + height + 3.0, o.getZ() - 4.5, -45.0F, 40.0F);
		context.waitTicks(10);
		context.takeScreenshot("cairn-" + n + "-top");
	}

	/** Regenerates the chunks around spawn and compares them with the real world; also asks the repair rules about them. */
	private static String oracleReport(ServerLevel level) {
		BlockPos centre = level.getRespawnData().pos();
		int cx = centre.getX() >> 4;
		int cz = centre.getZ() >> 4;
		long started = System.nanoTime();
		int chunks = 0;
		long compared = 0;
		long solidAgree = 0;
		long sameBlock = 0;
		long takes = 0;
		long tramples = 0;
		long holes = 0;
		Map<String, Integer> takeSamples = new java.util.TreeMap<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = cx - 2; x <= cx + 2; x++) {
			for (int z = cz - 2; z <= cz + 2; z++) {
				if (!level.hasChunk(x, z)) {
					continue;
				}
				TerrainOracle.TerrainChunk chunk = TerrainOracle.getNow(level, x, z);
				chunks++;
				for (int bx = 0; bx < 16; bx++) {
					for (int bz = 0; bz < 16; bz++) {
						int wx = (x << 4) + bx;
						int wz = (z << 4) + bz;
						int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
						for (int y = top - 24; y <= top + 2; y++) {
							pos.set(wx, y, wz);
							BlockState real = level.getBlockState(pos);
							BlockState target = chunk.state(pos);
							compared++;
							boolean realSolid = !real.isAir() && real.getFluidState().isEmpty();
							boolean targetSolid = !target.isAir() && target.getFluidState().isEmpty();
							solidAgree += realSolid == targetSolid ? 1 : 0;
							sameBlock += real.getBlock() == target.getBlock() ? 1 : 0;
						}
						// What the jobs would do here, in a world nobody has touched.
						int surface = chunk.surface(wx, wz);
						for (int y = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz) - 1, surface + 8); y >= surface - 1; y--) {
							pos.set(wx, y, wz);
							EndermanWork.Kind kind = EndermanWork.judge(level, pos, level.getRandom(), Set.of());
							if (kind == EndermanWork.Kind.TAKE) {
								takes++;
								takeSamples.merge(level.getBlockState(pos).getBlock().getDescriptionId() + " (target " + TargetWorld.target(level, pos).getBlock().getDescriptionId() + ")", 1, Integer::sum);
							} else if (kind == EndermanWork.Kind.TRAMPLE) {
								tramples++;
							}
						}
						BlockState surfaceTarget = chunk.state(pos.set(wx, surface, wz));
						if (TerrainKinds.family(surfaceTarget) != null && EndermanWork.holeInColumn(level, wx, wz, surfaceTarget, new BlockPos.MutableBlockPos()) != null) {
							holes++;
						}
					}
				}
			}
		}
		double ms = (System.nanoTime() - started) / 1e6;
		double perChunk = TerrainOracle.NANOS_BUILDING.get() / 1e6 / Math.max(1, TerrainOracle.CHUNKS_BUILT.get());
		Set<String> top = new HashSet<>();
		takeSamples.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(12).forEach(e -> top.add(e.getKey() + " x" + e.getValue()));
		return "oracle over %d chunks: %.1f ms per regenerated chunk (worker), %.0f ms in all; surface band: solid/air agree %.2f %%, same block %.2f %%;"
				.formatted(chunks, perChunk, ms, 100.0 * solidAgree / compared, 100.0 * sameBlock / compared)
				+ " untouched world: %d takes, %d tramples, %d holes in %d columns; takes: %s".formatted(takes, tramples, holes, chunks * 256, top);
	}

	private static int groundY(TestServerContext server, int x, int z) {
		return server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> mc.player != null && Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01, 400);
		context.waitTicks(2);
	}

	@SuppressWarnings("unused")
	private static boolean isCairnBlock(BlockState state) {
		return state.is(Blocks.PURPUR_BLOCK) || state.is(Blocks.PURPUR_PILLAR) || state.is(Blocks.END_ROD);
	}

	/**
	 * The Totem of the Beginning's ripple on untouched ground (a dry run: nothing changes): what it would change round
	 * {@code centre}, radius 24. On a world nobody has touched it should be close to nothing.
	 */
	static String rippleReport(ServerLevel level, BlockPos centre, String where) {
		BlockPos at = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre);
		java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> changes = io.github.bubba8587.bubbas_reworked_mobs.enderman.Ripples.dryRun(level, at, 24);
		java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
		for (var e : changes.entrySet()) {
			String from = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(e.getKey()).getBlock()).getPath();
			String to = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(e.getValue().getBlock()).getPath();
			kinds.merge(from + "->" + to, 1, Integer::sum);
		}
		return "ripple dry run at " + where + " (" + at.toShortString() + "): " + changes.size() + " changes " + kinds
				+ (changes.isEmpty() ? "" : ", first: " + changes.keySet().stream().limit(8).map(BlockPos::toShortString).toList());
	}
}

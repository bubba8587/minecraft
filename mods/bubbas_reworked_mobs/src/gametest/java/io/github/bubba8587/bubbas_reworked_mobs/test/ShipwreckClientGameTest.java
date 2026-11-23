// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Shipwreck crew by eye (client part {@code shipwrecks}): checks the nearest natural shipwrecks (every crew sleeper between
 * decks) and shoots up to three sleepers of the first wreck with a crew of three or more (shipwreck-sleeper-N).
 */
@SuppressWarnings("UnstableApiUsage")
public class ShipwreckClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("shipwrecks")) {
			return;
		}
		compare(context);
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
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("effect give @p minecraft:water_breathing infinite 0 true");
			// The nearest wrecks round spawn; the first one with a crew (broken fragments often have no room inside).
			java.util.List<BlockPos> wrecks = server.computeOnServer(s -> {
				var level = s.overworld();
				java.util.List<BlockPos> found = new java.util.ArrayList<>();
				BlockPos spawn = level.getRespawnData().pos();
				for (int[] d : new int[][] { { 0, 0 }, { 600, 0 }, { -600, 0 }, { 0, 600 }, { 0, -600 }, { 600, 600 }, { -600, -600 } }) {
					BlockPos w = level.findNearestMapStructure(StructureTags.SHIPWRECK, spawn.offset(d[0], 0, d[1]), 100, false);
					if (w != null && !found.contains(w)) {
						found.add(w);
					}
				}
				return found;
			});
			BlockPos sleeper = null;
			for (BlockPos wreck : wrecks) {
				tp(context, server, wreck.getX() + 0.5, 70, wreck.getZ() + 0.5, 0, 90);
				sp.getConnection().waitForChunksRender();
				int[] counts = new int[3];
				BlockPos one = server.computeOnServer(s -> {
					var level = s.overworld();
					BlockPos first = null;
					for (BlockPos pos : BlockPos.betweenClosed(wreck.atY(20).offset(-24, 0, -24), wreck.atY(70).offset(24, 0, 24))) {
						if (io.github.bubba8587.bubbas_reworked_mobs.undead.ShipwreckCrew.inHull(level, pos)) {
							counts[2]++;
						}
						if (level.getBlockState(pos).is(ModBlocks.sleeper(UndeadVariant.DROWNED))) {
							counts[0]++;
							if (!io.github.bubba8587.bubbas_reworked_mobs.undead.ShipwreckCrew.inHullAround(level, pos)) {
								counts[1]++;
							}
							if (first == null) {
								first = pos.immutable();
							}
						}
					}
					return first;
				});
				System.out.println("[shipwrecks] wreck at " + wreck.toShortString() + ": crew " + counts[0] + ", outside a hull " + counts[1] + ", empty inside spots " + counts[2]);
				if (counts[1] > 0) {
					throw new AssertionError("a drowned sleeper outside the hull at the wreck at " + wreck.toShortString());
				}
				if (one != null && sleeper == null) {
					sleeper = one;
				}
				if (counts[0] >= 3) {
					// A look at this wreck's crew from above and the side.
					java.util.List<BlockPos> crew = server.computeOnServer(srv -> {
						java.util.List<BlockPos> list = new java.util.ArrayList<>();
						for (BlockPos pos : BlockPos.betweenClosed(wreck.atY(20).offset(-24, 0, -24), wreck.atY(70).offset(24, 0, 24))) {
							if (srv.overworld().getBlockState(pos).is(ModBlocks.sleeper(UndeadVariant.DROWNED))) {
								list.add(pos.immutable());
							}
						}
						return list;
					});
					System.out.println("[shipwrecks] crew at " + crew);
					for (int k = 0; k < Math.min(3, crew.size()); k++) {
						BlockPos p = crew.get(k);
						// From an open cell beside it, looking at it.
						net.minecraft.core.Direction open = server.computeOnServer(srv -> {
							for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
								for (int far = 2; far >= 1; far--) {
									BlockPos q = p.relative(d, far);
									if (srv.overworld().getBlockState(q).getCollisionShape(srv.overworld(), q).isEmpty()
											&& srv.overworld().getBlockState(q.above()).getCollisionShape(srv.overworld(), q.above()).isEmpty()) {
										return d;
									}
								}
							}
							return net.minecraft.core.Direction.NORTH;
						});
						float yaw = open.getOpposite().toYRot();
						tp(context, server, p.getX() + 0.5 + open.getStepX() * 1.8, p.getY() + 1.0, p.getZ() + 0.5 + open.getStepZ() * 1.8, yaw, 35);
						context.takeScreenshot("shipwreck-sleeper-" + k);
					}
					break;
				}
			}
			if (sleeper == null) {
				throw new AssertionError("none of the nearest shipwrecks has drowned sleepers");
			}
		}
	}

	/** A drowned sleeper sitting and lying beside a vanilla drowned, on a flat world (drowned-compare). */
	private static void compare(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY();
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @p");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 6, x + 6, y + 4, z + 6));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:drowned_sleeper[pose=sitting,facing=south]".formatted(x - 2, y, z - 3));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:drowned_sleeper[pose=lying,facing=east]".formatted(x, y, z - 3));
			server.runCommand("summon minecraft:drowned %d %d %d {NoAI:1b,Rotation:[0f,0f],PersistenceRequired:1b}".formatted(x + 3, y, z - 3));
			context.waitTicks(10);
			tp(context, server, x + 0.5, y + 1.6, z + 1.5, 180, 15);
			context.takeScreenshot("drowned-compare");
			tp(context, server, x - 1.5, y + 1.0, z - 1.0, 180, 20);
			context.takeScreenshot("drowned-compare-close");
		}
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}

// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.grave.GravestoneBlockEntity;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Gravestones by eye (specs/graveyards.md; client part {@code graves}): four stones on grass, turned 0, 22.5 and -22.5
 * degrees and square again, carrying a player's epitaph, a line of enchanting-table glyphs (what worldgen carves),
 * nothing, and glow-ink text. Shots: graves-front (straight on), graves-angle (from the side, showing depth and the
 * plinth), graves-close (the epitaph stone close up), graves-close-glow (the glow-ink stone), graves-night (all, at night by soul lanterns), and the same with the 32x pack (-32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class GravesClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("graves")) {
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
			server.runCommand("time set 13000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 8, x + 8, y + 6, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 1, z - 8, x + 8, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:coarse_dirt".formatted(x - 4, y - 1, z - 2, x + 4, y - 1, z - 1));
			int[] rotations = { 0, 1, 15, 0 };
			for (int i = 0; i < 4; i++) {
				server.runCommand("setblock %d %d %d bubbas_reworked_mobs:gravestone[rotation=%d]".formatted(x - 3 + 2 * i, y, z - 3, rotations[i]));
			}
			// Soul lanterns either side, for the night light.
			server.runCommand("setblock %d %d %d minecraft:soul_lantern".formatted(x - 5, y, z - 3));
			server.runCommand("setblock %d %d %d minecraft:soul_lantern".formatted(x + 5, y, z - 3));
			context.waitTicks(5);
			server.runOnServer(s -> {
				var level = s.overworld();
				text(level, new BlockPos(x - 3, y, z - 3), lines(Component.literal("Here lies"), Component.literal("Old Bramble"),
						Component.literal("a good dog"), Component.empty()), DyeColor.BLACK, false);
				Component glyphs = Component.literal("vgerl naruth").withStyle(st -> st.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt"))));
				Component glyphs2 = Component.literal("ozkat").withStyle(st -> st.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("alt"))));
				text(level, new BlockPos(x - 1, y, z - 3), lines(Component.empty(), glyphs, glyphs2, Component.empty()), DyeColor.BLACK, false);
				text(level, new BlockPos(x + 3, y, z - 3), lines(Component.literal("R.I.P."), Component.literal("Steve"), Component.empty(),
						Component.literal("1 2 3")), DyeColor.LIGHT_BLUE, true);
			});
			context.waitTicks(10);

			shots(context, server, x, y, z, "");
			setPack(context, true);
			shots(context, server, x, y, z, "-32x");
			setPack(context, false);
		}
		yards(context);
	}

	/**
	 * Yards in a real world (default preset, seed 20261116): the nearest naturally generated crypt yard from above and
	 * from its gate (yard-natural-*), then a yard with a crypt built by the command beside spawn (yard-built-*) and the
	 * inside of its vault (crypt-vault and crypt-vault-back with night vision, crypt-vault-dark without: candlelight only).
	 */
	private static void yards(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
									.getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)));
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

			// The nearest natural yard.
			BlockPos found = server.computeOnServer(s -> {
				var level = s.overworld();
				var holder = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
						.getOrThrow(io.github.bubba8587.bubbas_reworked_mobs.grave.Graveyards.CRYPT_YARD_KEY);
				var result = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder),
						level.getRespawnData().pos(), 100, false);
				return result == null ? null : result.getFirst();
			});
			if (found == null) {
				throw new AssertionError("no crypt yard generated within 100 spacings of spawn");
			}
			System.out.println("[graves] nearest natural crypt yard: " + found.toShortString());
			tp(context, server, found.getX() + 0.5, 140, found.getZ() - 6.5, 0, 45);
			sp.getConnection().waitForChunksRender();
			int ground = server.computeOnServer(s -> s.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					found.getX() + 8, found.getZ() + 8));
			// The yard's north-west corner is the start chunk's corner (found is about the chunk's middle).
			tp(context, server, found.getX() + 0.5, ground + 22, found.getZ() - 22.5, 0, 50);
			context.takeScreenshot("yard-natural-above");
			tp(context, server, found.getX() + 0.5, ground + 4, found.getZ() - 14.5, 0, 20);
			context.takeScreenshot("yard-natural-gate");

			// A yard with a crypt, built on the nearest plains (open ground, so it shows from above).
			BlockPos gate = server.computeOnServer(s -> {
				var level = s.overworld();
				// The flattest of a few plains spots near spawn (the command builds anywhere; natural yards need flat ground).
				BlockPos best = null;
				int bestSlope = Integer.MAX_VALUE;
				for (int i = 0; i < 8; i++) {
					BlockPos from = level.getRespawnData().pos().offset(i * 160 - 560, 0, (i % 3) * 200 - 200);
					var plains = level.findClosestBiome3d(b -> b.is(net.minecraft.world.level.biome.Biomes.PLAINS), from, 1600, 32, 64);
					if (plains == null) {
						continue;
					}
					BlockPos p = plains.getFirst();
					int slope = slope(level, p.getX(), p.getZ());
					if (slope < bestSlope) {
						bestSlope = slope;
						best = p;
					}
				}
				if (best == null) {
					throw new AssertionError("no plains near spawn");
				}
				return best;
			});
			System.out.println("[graves] built yard on plains at " + gate.toShortString());
			tp(context, server, gate.getX() + 0.5, 140, gate.getZ() - 10.5, 0, 45);
			sp.getConnection().waitForChunksRender();
			var piece = server.computeOnServer(s -> io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand.build(s.overworld(), gate, true, 77L));
			BlockPos g = piece.gate();
			context.waitTicks(20);
			tp(context, server, g.getX() + 0.5, g.getY() + 20, g.getZ() - 9.5, 0, 55);
			context.takeScreenshot("yard-built-above");
			tp(context, server, g.getX() + 0.5, g.getY() + 3, g.getZ() - 4.5, 0, 15);
			context.takeScreenshot("yard-built-gate");
			tp(context, server, g.getX() + 0.5, g.getY() + 1.2, g.getZ() + 2.5, 0, 15);
			context.takeScreenshot("yard-built-inside");
			tp(context, server, g.getX() - 6.5, g.getY() + 1.6, g.getZ() + 5.5, 225, 20);
			context.takeScreenshot("yard-built-rows");
			BlockPos vault = piece.vaultCentre();
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			tp(context, server, vault.getX() + 0.5, vault.getY() + 0.2, vault.getZ() - 3.5, 0, 10);
			context.takeScreenshot("crypt-vault");
			tp(context, server, vault.getX() + 0.5, vault.getY() + 0.2, vault.getZ() + 3.5, 180, 10);
			context.takeScreenshot("crypt-vault-back");
			server.runCommand("effect clear @p");
			tp(context, server, vault.getX() + 0.5, vault.getY() + 0.2, vault.getZ() - 3.5, 0, 10);
			context.takeScreenshot("crypt-vault-dark");
		}
	}

	private static void shots(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String suffix) {
		server.runCommand("time set noon");
		tp(context, server, x + 0.5, y + 0.4, z + 2.5, 180, 15);
		context.takeScreenshot("graves-front" + suffix);
		tp(context, server, x - 5.5, y + 0.6, z + 1.0, 225, 20);
		context.takeScreenshot("graves-angle" + suffix);
		tp(context, server, x - 2.5, y - 0.1, z - 0.3, 180, 25);
		context.takeScreenshot("graves-close" + suffix);
		tp(context, server, x + 3.5, y - 0.1, z - 0.3, 180, 25);
		context.takeScreenshot("graves-close-glow" + suffix);
		server.runCommand("time set 18000");
		tp(context, server, x + 0.5, y + 0.4, z + 2.5, 180, 15);
		context.takeScreenshot("graves-night" + suffix);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}

	/** Height difference over a yard-sized square south of (x, z) (generator heights, no chunks loaded). */
	private static int slope(net.minecraft.server.level.ServerLevel level, int x, int z) {
		var gen = level.getChunkSource().getGenerator();
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		for (int dx = -9; dx <= 9; dx += 6) {
			for (int dz = 0; dz <= 36; dz += 6) {
				int h = gen.getBaseHeight(x + dx, z + dz, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level,
						level.getChunkSource().randomState());
				int floor = gen.getBaseHeight(x + dx, z + dz, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level,
						level.getChunkSource().randomState());
				if (floor != h) {
					return Integer.MAX_VALUE; // water there
				}
				low = Math.min(low, h);
				high = Math.max(high, h);
			}
		}
		return high - low;
	}

	private static List<Component> lines(Component a, Component b, Component c, Component d) {
		return List.of(a, b, c, d);
	}

	private static void text(net.minecraft.server.level.ServerLevel level, BlockPos pos, List<Component> lines, DyeColor color, boolean glow) {
		if (!(level.getBlockEntity(pos) instanceof GravestoneBlockEntity stone)) {
			throw new AssertionError("no gravestone at " + pos);
		}
		Component[] array = lines.toArray(Component[]::new);
		stone.setText(new SignText(List.of(array), List.of(array), color, glow), SignTextSlot.FRONT);
		stone.setChanged();
		level.sendBlockUpdated(pos, stone.getBlockState(), stone.getBlockState(), 3);
	}

	private static void setPack(ClientGameTestContext context, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (!repo.getAvailableIds().contains(PACK_32X)) {
				throw new AssertionError("no resource pack " + PACK_32X + "; available: " + repo.getAvailableIds());
			}
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

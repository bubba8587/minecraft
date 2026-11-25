// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.Locale;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.look.CreeperSkins;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Biome creeper skins by eye (client test part {@code biomecreepers}): every skin in a row with a vanilla creeper
 * first, four at a time close up, left to right (biome-creepers-0..4), and a charged birch creeper beside a charged vanilla one
 * (biome-creepers-charged), a birch creeper half behind a birch trunk and a sand creeper against sand
 * (biome-creepers-camo-birch, -camo-sand); all again with the 32x pack (-32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class BiomeCreepersClientGameTest implements FabricClientGameTest {
	private static final String TAG = "biome_creeper_shot";
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";
	static final List<String> SKINS = List.of("vanilla", "oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak",
			"poplar", "dirt", "sand", "red_sand", "snow", "stone", "mushroom_stem", "dripstone", "sulfur");
	private static final double STEP = 1.25;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("biomecreepers")) {
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
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @p");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 6, x + 30, y + 5, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 4, y - 1, z - 6, x + 30, y - 1, z + 8));
			for (int i = 0; i < SKINS.size(); i++) {
				summon(server, x + 24.5 - i * STEP, y, z + 0.5, "%s_%d".formatted(TAG, i), "");
			}
			summon(server, x + 0.5, y, z + 4.5, TAG + "_cv", ",powered:1b");
			summon(server, x + 2.0, y, z + 4.5, TAG + "_cb", ",powered:1b");
			// Camouflage: a birch creeper half behind a birch trunk, a sand creeper against sand.
			server.runCommand("fill %d %d %d %d %d %d minecraft:birch_log".formatted(x + 10, y, z + 6, x + 10, y + 3, z + 6));
			summon(server, x + 11.35, y, z + 7.4, TAG + "_camo_birch", "");
			server.runCommand("fill %d %d %d %d %d %d minecraft:sand".formatted(x + 14, y - 1, z + 4, x + 18, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:sand".formatted(x + 14, y, z + 8, x + 18, y + 2, z + 8));
			summon(server, x + 16.5, y, z + 7.2, TAG + "_camo_sand", "");
			context.waitTicks(3);
			server.runOnServer(s -> {
				for (Entity e : s.overworld().getAllEntities()) {
					if (!(e instanceof Creeper c)) {
						continue;
					}
					for (int i = 0; i < SKINS.size(); i++) {
						if (e.entityTags().contains("%s_%d".formatted(TAG, i))) {
							c.setAttached(CreeperSkins.SKIN, BubbasReworkedMobs.id(SKINS.get(i)));
						}
					}
					if (e.entityTags().contains(TAG + "_cv")) {
						c.setAttached(CreeperSkins.SKIN, CreeperSkins.VANILLA);
					} else if (e.entityTags().contains(TAG + "_cb") || e.entityTags().contains(TAG + "_camo_birch")) {
						c.setAttached(CreeperSkins.SKIN, BubbasReworkedMobs.id("birch"));
					} else if (e.entityTags().contains(TAG + "_camo_sand")) {
						c.setAttached(CreeperSkins.SKIN, BubbasReworkedMobs.id("sand"));
					}
				}
			});
			context.waitTicks(10);
			shots(context, server, x, y, z, "");
			setPack(context, true);
			shots(context, server, x, y, z, "-32x");
			setPack(context, false);
		}
	}

	private static void summon(TestServerContext server, double x, int y, double z, String tag, String extra) {
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:creeper %.2f %d %.2f {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:[\"%s\"]%s}",
				x, y, z, tag, extra));
	}

	private static void shots(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String suffix) {
		for (int group = 0; group < 5; group++) {
			double cx = x + 24.5 - (group * 4 + 1.5) * STEP;
			tp(context, server, cx, y + 0.5, z - 2.4, 0, 12);
			context.takeScreenshot("biome-creepers-" + group + suffix);
		}
		tp(context, server, x + 1.25, y + 0.6, z + 2.4, 0, 15);
		context.takeScreenshot("biome-creepers-charged" + suffix);
		tp(context, server, x + 10.5, y + 0.8, z + 4.4, 0, 12);
		context.takeScreenshot("biome-creepers-camo-birch" + suffix);
		tp(context, server, x + 16.5, y + 0.6, z + 4.4, 0, 10);
		context.takeScreenshot("biome-creepers-camo-sand" + suffix);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(5);
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

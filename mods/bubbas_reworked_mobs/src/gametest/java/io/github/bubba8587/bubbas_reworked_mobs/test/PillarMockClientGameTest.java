// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.Locale;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperTuning;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.look.CreeperSkins;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Mock-up of a planned creeper feature, static scenes only (client test part {@code pillarmock}, run only when named):
 * on flat ground of six biomes a 2-block pillar of the ground block, two 1-deep holes beside it the blocks came from,
 * and a NoAI creeper with that biome's skin half behind the pillar, facing the camera. Screenshots pillarmock-*.
 */
@SuppressWarnings("UnstableApiUsage")
public class PillarMockClientGameTest implements FabricClientGameTest {
	private static final String TAG = "pillar_mock";
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";
	private static final int SPACING = 100;

	/** One scene: biome, ground block, block under it, creeper skin. */
	private record Station(String name, String biome, String ground, String under, String skin) {
	}

	private static final List<Station> STATIONS = List.of(
			new Station("plains", "plains", "grass_block", "dirt", "plains"),
			new Station("desert", "desert", "sand", "sandstone", "sand"),
			new Station("badlands", "badlands", "red_sand", "orange_terracotta", "red_sand"),
			new Station("snowy", "snowy_plains", "snow_block", "dirt", "snow"),
			new Station("stony", "stony_shore", "stone", "stone", "stone"),
			new Station("mushroom", "mushroom_fields", "mycelium", "dirt", "mushroom_stem"));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.named("pillarmock")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int y = base.getY();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule max_block_modifications 1000000");
			server.runCommand("gamerule advance_time false");
			server.runCommand("weather clear");
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			for (int i = 0; i < STATIONS.size(); i++) {
				build(context, sp, server, STATIONS.get(i), cx(base, i), y, cz(base));
			}
			context.waitTicks(10);

			for (int i = 0; i < STATIONS.size(); i++) {
				Station s = STATIONS.get(i);
				int cx = cx(base, i);
				int cz = cz(base);
				if (i == 0 || i == 1 || i == 3) {
					shot(context, sp, server, cx + 0.5, y, cz - 15.0, 0.0F, 2.0F, "pillarmock-" + s.name() + "-wide");
				}
				close(context, sp, server, cx, y, cz, "pillarmock-" + s.name() + "-close");
			}
			// Plains from the side: the creeper behind the pillar, the holes in front.
			shot(context, sp, server, cx(base, 0) - 4.5, y + 0.6, cz(base) + 1.2, -90.0F, 14.0F, "pillarmock-plains-side");
			server.runCommand("time set 12600");
			shot(context, sp, server, cx(base, 0) + 0.5, y, cz(base) - 15.0, 0.0F, 2.0F, "pillarmock-plains-dusk");
			server.runCommand("time set noon");

			setPack(context, true);
			close(context, sp, server, cx(base, 0), y, cz(base), "pillarmock-plains-close-32x");
			close(context, sp, server, cx(base, 1), y, cz(base), "pillarmock-desert-close-32x");
			setPack(context, false);
			context.runOnClient(mc -> {
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
	}

	private static int cx(BlockPos base, int i) {
		return base.getX() + (i - STATIONS.size() / 2) * SPACING;
	}

	private static int cz(BlockPos base) {
		return base.getZ() + 16;
	}

	/** Ground, biome, pillar at (cx, cz), 1-deep holes at (cx + 1, cz - 1) and (cx - 1, cz - 2), creeper half behind the pillar's left edge. */
	private static void build(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, Station s, int cx, int y, int cz) {
		tp(context, sp, server, cx + 0.5, y + 1, cz - 15.0, 0.0F, 0.0F);
		int x0 = cx - SPACING / 2 + 1;
		int x1 = cx + SPACING / 2 - 1;
		int z0 = cz - 24;
		int z1 = cz + 80;
		server.runCommand("fillbiome %d %d %d %d %d %d minecraft:%s".formatted(x0, y - 4, z0, x1, y + 8, z1, s.biome()));
		server.runCommand("fill %d %d %d %d %d %d minecraft:%s".formatted(x0, y - 3, z0, x1, y - 2, z1, s.under()));
		server.runCommand("fill %d %d %d %d %d %d minecraft:%s".formatted(x0, y - 1, z0, x1, y - 1, z1, s.ground()));
		server.runCommand("fill %d %d %d %d %d %d minecraft:%s".formatted(cx, y, cz, cx, y + 1, cz, s.ground()));
		server.runCommand("setblock %d %d %d minecraft:air".formatted(cx + 1, y - 1, cz - 1));
		server.runCommand("setblock %d %d %d minecraft:air".formatted(cx - 1, y - 1, cz - 2));
		// Camera at (cx + 0.5, cz - 15): the pillar's near left corner, projected to the creeper's depth, is the edge.
		double depth = 1.3;
		double camX = cx + 0.5;
		double edge = camX + (cx - camX) * (15.0 + depth) / 15.0;
		double creeperX = edge - CreeperTuning.HALF_HIDE_INSET;
		double creeperZ = cz + depth;
		String tag = TAG + "_" + s.name();
		server.runCommand(String.format(Locale.ROOT,
				"summon minecraft:creeper %.3f %d %.3f {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[%.1ff,0f],Tags:[\"%s\"]}",
				creeperX, y, creeperZ, yawToward(creeperX, creeperZ, camX, cz - 15.0), tag));
		context.waitTicks(2);
		Identifier skin = BubbasReworkedMobs.id(s.skin());
		server.runOnServer(srv -> {
			for (Entity e : srv.overworld().getAllEntities()) {
				if (e instanceof Creeper c && e.entityTags().contains(tag)) {
					c.setAttached(CreeperSkins.SKIN, skin);
					float yaw = c.getYRot();
					c.setYHeadRot(yaw);
					c.setYBodyRot(yaw);
				}
			}
		});
	}

	/** From the creeper's open side, a few blocks out: pillar, hole and peeking creeper. */
	private static void close(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, int cx, int y, int cz, String name) {
		double camX = cx - 1.6;
		double camZ = cz - 4.6;
		shot(context, sp, server, camX, y + 0.2, camZ, yawToward(camX, camZ, cx + 0.4, cz + 0.6), 14.0F, name);
	}

	/** Minecraft yaw for looking from (x, z) toward (tx, tz). */
	private static float yawToward(double x, double z, double tx, double tz) {
		return (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, double x, double y, double z, float yaw, float pitch, String name) {
		tp(context, sp, server, x, y, z, yaw, pitch);
		context.waitTicks(10);
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(name);
	}

	private static void tp(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		sp.getConnection().waitForChunksRender();
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

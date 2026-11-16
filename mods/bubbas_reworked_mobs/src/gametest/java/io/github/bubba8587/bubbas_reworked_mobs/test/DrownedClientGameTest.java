// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedAccess;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedMind;
import io.github.bubba8587.bubbas_reworked_mobs.drowned.LurkSpots;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.npc.villager.Villager;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at the lurking drowned (specs/drowned.md; client part {@code drowned}). A stone block in the sky with a grass
 * shore, 2-deep shallows and a deep pit. Upright: drowned-lurk and drowned-lurk-low (head above the water),
 * drowned-sunk (a villager on the shore stares at it), drowned-grab (it has surged and grabbed a villager with its
 * back turned), drowned-drag-deep (upright, under water, holding it), drowned-held-player (the camera player held,
 * from the front). Lying: drowned-swim (swimming to its spot), drowned-crawl-side and drowned-crawl-front (crawling
 * on land).
 */
@SuppressWarnings("UnstableApiUsage")
public class DrownedClientGameTest implements FabricClientGameTest {
	private static final String TAG = "drowned_shot";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("drowned")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY() + 14;
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 4, z));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 11, y, z - 11, x + 11, y + 10, z + 20));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 11, y - 11, z - 11, x + 11, y - 1, z + 20));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 11, y - 1, z - 11, x + 11, y - 1, z + 3));
			server.runCommand("fill %d %d %d %d %d %d minecraft:sand".formatted(x - 10, y - 3, z + 4, x + 10, y - 3, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(x - 10, y - 2, z + 4, x + 10, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:gravel".formatted(x - 10, y - 10, z + 9, x + 10, y - 10, z + 19));
			server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(x - 10, y - 9, z + 9, x + 10, y - 1, z + 19));
			server.runCommand("fill %d %d %d %d %d %d minecraft:seagrass".formatted(x - 10, y - 9, z + 9, x + 10, y - 9, z + 9));
			context.waitTicks(10);

			// --- upright: lurking at the shallows' first column, facing the shore -------------------------
			BlockPos spotColumn = new BlockPos(x, y - 1, z + 4);
			server.runCommand("summon minecraft:drowned %d.5 %d %d.5 {PersistenceRequired:1b,Silent:1b,Tags:[\"%s\"]}".formatted(x, y - 2, z + 6, TAG));
			context.waitTicks(2);
			onServer(server, level -> {
				Drowned drowned = drowned(level);
				DrownedAccess.mind(drowned).pinTo(drowned, LurkSpots.at(level, spotColumn));
				return null;
			});
			waitFor(context, server, "lurking", (level) -> {
				Drowned d = drowned(level);
				DrownedMind mind = DrownedAccess.mind(d);
				return mind.state() == DrownedMind.State.LURK && Math.abs(d.getDeltaMovement().y) < 0.005 && !mind.isSunk();
			});
			context.waitTicks(15);
			tp(context, server, x + 0.5, y + 0.4, z + 0.5, 0.0F, 20.0F);
			context.takeScreenshot("drowned-lurk");
			tp(context, server, x + 3.5, y - 0.05, z + 4.5, 90.0F, 2.0F);
			context.takeScreenshot("drowned-lurk-low");

			// --- sunk: a villager on the shore stares at it ------------------------------------------
			server.runCommand("summon minecraft:villager %d.5 %d %d.5 {NoAI:1b,Silent:1b,Rotation:[0f,25f],Tags:[\"%s_v\"]}".formatted(x, y, z + 1, TAG));
			context.waitTicks(2);
			onServer(server, level -> {
				villager(level).setYHeadRot(0.0F);
				return null;
			});
			waitFor(context, server, "sunk", level -> DrownedAccess.mind(drowned(level)).isSunk() && Math.abs(drowned(level).getDeltaMovement().y) < 0.005);
			context.waitTicks(10);
			tp(context, server, x + 3.5, y + 0.9, z + 2.0, 60.0F, 20.0F);
			context.takeScreenshot("drowned-sunk");

			// --- the grab: the villager turns its back at the edge -----------------------------------------
			server.runCommand("tp @e[tag=%s_v] %d.5 %d %d.3 180 0".formatted(TAG, x, y, z + 3));
			onServer(server, level -> {
				villager(level).setYHeadRot(180.0F);
				return null;
			});
			waitFor(context, server, "grab", level -> DrownedAccess.mind(drowned(level)).state() == DrownedMind.State.DRAG);
			context.waitTicks(6);
			tp(context, server, x + 4.5, y + 0.6, z + 3.5, 90.0F, 15.0F);
			context.takeScreenshot("drowned-grab");
			waitFor(context, server, "dragged deep", level -> drowned(level).getZ() > z + 9.0 && drowned(level).getY() < y - 8.5);
			context.waitTicks(5);
			double dz = onServer(server, level -> drowned(level).getZ());
			double dy = onServer(server, level -> drowned(level).getY());
			tp(context, server, x + 4.0, dy + 1.0, dz, 90.0F, 5.0F);
			context.takeScreenshot("drowned-drag-deep");
			server.runCommand("kill @e[tag=%s]".formatted(TAG));
			server.runCommand("kill @e[tag=%s_v]".formatted(TAG));
			context.waitTicks(30);

			// --- lying: swimming from the pit to its spot ---------------------------------------------
			server.runCommand("summon minecraft:drowned %d.5 %d %d.5 {PersistenceRequired:1b,Silent:1b,Tags:[\"%s\"]}".formatted(x - 6, y - 7, z + 17, TAG));
			context.waitTicks(2);
			onServer(server, level -> {
				Drowned drowned = drowned(level);
				DrownedAccess.mind(drowned).pinTo(drowned, LurkSpots.at(level, spotColumn));
				return null;
			});
			waitFor(context, server, "swimming", level -> drowned(level).isSwimming() && drowned(level).getDeltaMovement().horizontalDistance() > 0.03);
			context.waitTicks(15);
			double sx = onServer(server, level -> drowned(level).getX());
			double sy = onServer(server, level -> drowned(level).getY());
			double sz = onServer(server, level -> drowned(level).getZ());
			tp(context, server, sx + 4.0, sy + 0.8, sz, 90.0F, 5.0F);
			context.takeScreenshot("drowned-swim");
			server.runCommand("kill @e[tag=%s]".formatted(TAG));
			context.waitTicks(30);

			// --- lying: crawling on land toward the water (at night: in daylight it burns on land, vanilla) ----
			server.runCommand("time set midnight");
			server.runCommand("summon minecraft:drowned %d.5 %d %d.5 {PersistenceRequired:1b,Silent:1b,Rotation:[0f,0f],Tags:[\"%s\"]}".formatted(x, y, z - 9, TAG));
			context.waitTicks(2);
			for (int i = 0; i < 25; i++) {
				onServer(server, level -> {
					drowned(level).getNavigation().moveTo(x + 0.5, y, z + 2.5, 1.0);
					return null;
				});
				context.waitTick();
			}
			double cz = onServer(server, level -> drowned(level).getZ());
			tp(context, server, x + 3.0, y + 0.5, cz + 0.5, 90.0F, 10.0F);
			for (int i = 0; i < 3; i++) {
				onServer(server, level -> {
					drowned(level).getNavigation().moveTo(x + 0.5, y, z + 2.5, 1.0);
					return null;
				});
				context.waitTick();
			}
			context.takeScreenshot("drowned-crawl-side");
			double cz2 = onServer(server, level -> drowned(level).getZ());
			tp(context, server, x + 1.3, y + 1.2, cz2 + 2.6, 160.0F, 25.0F);
			for (int i = 0; i < 6; i++) {
				onServer(server, level -> {
					drowned(level).getNavigation().moveTo(x + 0.5, y, z + 2.5, 1.0);
					return null;
				});
				context.waitTick();
			}
			context.takeScreenshot("drowned-crawl-front");
			server.runCommand("kill @e[tag=%s]".formatted(TAG));
			server.runCommand("time set noon");
			context.waitTicks(30);

			// --- upright: holding the camera player (survival), from the front --------------------------
			server.runCommand("gamemode survival @p");
			server.runCommand("effect give @p minecraft:resistance infinite 4 true");
			server.runCommand("tp @p %d.5 %d %d.5 90 10".formatted(x, y - 2, z + 6));
			server.runCommand("summon minecraft:drowned %d.5 %d %d.5 {PersistenceRequired:1b,Silent:1b,Rotation:[180f,0f],Tags:[\"%s\"]}".formatted(x, y - 2, z + 7, TAG));
			context.waitTicks(5);
			onServer(server, level -> {
				Drowned drowned = drowned(level);
				drowned.setYRot(180.0F);
				drowned.setYBodyRot(180.0F);
				DrownedAccess.mind(drowned).grab(drowned, level, level.players().getFirst());
				return null;
			});
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(25);
			context.takeScreenshot("drowned-held-player");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("kill @e[tag=%s]".formatted(TAG));
			context.waitTicks(2);
		}
	}

	private static Drowned drowned(ServerLevel level) {
		for (Entity e : level.getAllEntities()) {
			if (e instanceof Drowned d && e.entityTags().contains(TAG) && d.isAlive()) {
				return d;
			}
		}
		throw new AssertionError("no drowned");
	}

	private static Villager villager(ServerLevel level) {
		for (Entity e : level.getAllEntities()) {
			if (e instanceof Villager v && e.entityTags().contains(TAG + "_v")) {
				return v;
			}
		}
		throw new AssertionError("no villager");
	}

	private static <T> T onServer(TestServerContext server, Function<ServerLevel, T> action) {
		return server.computeOnServer(s -> action.apply(s.overworld()));
	}

	private static void waitFor(ClientGameTestContext context, TestServerContext server, String what, Function<ServerLevel, Boolean> condition) {
		for (int i = 0; i < 400; i++) {
			if (onServer(server, condition)) {
				System.out.println("[drowned-shot] " + what + " after " + i + " ticks");
				return;
			}
			context.waitTick();
		}
		String state = onServer(server, level -> {
			Drowned d = drowned(level);
			DrownedMind mind = DrownedAccess.mind(d);
			return "state %s sunk %s pos %s spot %s".formatted(mind.state(), mind.isSunk(), d.position(), mind.spot());
		});
		throw new AssertionError("drowned never got to: " + what + " (" + state + ")");
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}

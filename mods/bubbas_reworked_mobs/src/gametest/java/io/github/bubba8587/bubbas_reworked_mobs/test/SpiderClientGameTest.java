// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mobs.spider.NestSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderNest;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at a cave nest (specs/spiders.md; client part {@code spiders}). A room in the sky: stone floor, a stone ceiling
 * 14 blocks up, open sides so the camera (a spectator) sees in. Shots: spider-nest (from the floor looking up at the
 * sleepers hanging upside down in the cobwebs), spider-nest-close, spider-drop (one falling head first on its thread), spider-dangle (a miss hanging on its thread before it snaps),
 * spider-latch (one reeling a pig up its thread), spider-held (the pig held in the nest).
 */
@SuppressWarnings("UnstableApiUsage")
public class SpiderClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("spiders")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY() + 20;
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 4, z));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 10, y - 1, z - 10, x + 10, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 10, y + 15, z - 10, x + 10, y + 16, z + 10));
			int top = y + 14;
			int[][] webs = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {2, 0}, {-2, 1}, {1, -2}, {-1, 2}, {2, 2}};
			for (int[] w : webs) {
				server.runCommand("setblock %d %d %d minecraft:cobweb".formatted(x + w[0], top, z + w[1]));
			}
			server.runCommand("setblock %d %d %d minecraft:cobweb".formatted(x, top - 1, z));
			context.waitTicks(5);
			onServer(server, level -> {
				SpiderNest nest = Spiders.SPIDER_NEST.create(level, EntitySpawnReason.STRUCTURE);
				nest.setPos(x + 0.5, top + 0.5, z + 0.5);
				nest.setDepth(15);
				level.addFreshEntity(nest);
				int[][] spots = {{1, 0}, {-1, -1}, {-1, 2}};
				float yaw = 20.0F;
				for (int[] s : spots) {
					NestSpider spider = Spiders.NEST_SPIDER.create(level, EntitySpawnReason.STRUCTURE);
					spider.setYRot(yaw);
					spider.setYBodyRot(yaw);
					spider.setYHeadRot(yaw);
					spider.setSilent(true);
					spider.hangFrom(new Vec3(x + s[0] + 0.5, top + 1.0, z + s[1] + 0.5));
					level.addFreshEntity(spider);
					yaw += 110.0F;
				}
				return null;
			});
			context.waitTicks(10);

			// --- the nest from below and from the side -------------------------------------------------
			tp(context, server, x + 0.5, y + 0.5, z - 6.5, 0.0F, -55.0F);
			context.takeScreenshot("spider-nest");
			tp(context, server, x + 0.5, top - 3.0, z - 4.5, 0.0F, -25.0F);
			context.takeScreenshot("spider-nest-close");

			// --- the drop: one lets go on nobody and falls head first on its thread ----------------------
			tp(context, server, x - 7.5, y + 7.0, z + 0.5, -90.0F, 5.0F);
			onServer(server, level -> {
				sleeper(level, x + 1.5, z + 0.5).drop(null);
				return null;
			});
			context.waitTicks(7);
			context.takeScreenshot("spider-drop");
			waitFor(context, server, "dangling after a miss", level -> level.getEntitiesOfClass(NestSpider.class,
					new AABB(x - 3, y - 1, z - 3, x + 4, y + 16, z + 4), sp2 -> sp2.phase() == NestSpider.Phase.DANGLING).size() == 1);
			context.waitTicks(4);
			context.takeScreenshot("spider-dangle");
			context.waitTicks(30);

			// --- the latch: a pig under another one is dropped on, latched and reeled up -------------------
			server.runCommand("summon minecraft:pig %d.5 %d %d.5 {NoAI:1b,Silent:1b,Tags:[\"spider_shot_pig\"]}".formatted(x - 1, y, z - 1));
			server.runCommand("effect give @e[tag=spider_shot_pig] minecraft:resistance infinite 4 true");
			context.waitTicks(3);
			onServer(server, level -> {
				Pig pig = level.getEntitiesOfClass(Pig.class, new AABB(x - 3, y - 1, z - 3, x + 3, y + 3, z + 3)).getFirst();
				sleeper(level, x - 0.5, z - 0.5).drop(pig);
				return null;
			});
			waitFor(context, server, "reeling up", level -> holder(level) != null && holder(level).getY() > y + 5.0);
			tp(context, server, x - 7.5, y + 6.0, z - 0.5, -90.0F, -5.0F);
			context.takeScreenshot("spider-latch");
			waitFor(context, server, "held in the nest", level -> holder(level) != null && holder(level).phase() == NestSpider.Phase.HOLDING);
			context.waitTicks(5);
			tp(context, server, x - 6.5, top - 2.0, z - 0.5, -90.0F, 10.0F);
			context.takeScreenshot("spider-held");
			tp(context, server, x - 0.5, y + 0.5, z - 6.5, 0.0F, -50.0F);
			context.takeScreenshot("spider-held-below");
		}
	}

	private static NestSpider sleeper(ServerLevel level, double x, double z) {
		List<NestSpider> list = level.getEntitiesOfClass(NestSpider.class, new AABB(x - 0.4, -1000, z - 0.4, x + 0.4, 1000, z + 0.4), NestSpider::sleeping);
		if (list.isEmpty()) {
			throw new AssertionError("no sleeper at " + x + ", " + z);
		}
		return list.getFirst();
	}

	private static NestSpider holder(ServerLevel level) {
		for (NestSpider s : level.getEntitiesOfClass(NestSpider.class, new AABB(-3.0E7, -1000, -3.0E7, 3.0E7, 1000, 3.0E7))) {
			if (s.victim() != null) {
				return s;
			}
		}
		return null;
	}

	private static <T> T onServer(TestServerContext server, Function<ServerLevel, T> action) {
		return server.computeOnServer(s -> action.apply(s.overworld()));
	}

	private static void waitFor(ClientGameTestContext context, TestServerContext server, String what, Function<ServerLevel, Boolean> condition) {
		for (int i = 0; i < 400; i++) {
			if (onServer(server, condition)) {
				System.out.println("[spider-shot] " + what + " after " + i + " ticks");
				return;
			}
			context.waitTick();
		}
		throw new AssertionError("never got to: " + what);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}

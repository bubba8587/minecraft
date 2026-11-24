// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderNestFeature;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The spun webs by eye (specs/spiders.md "Spun webs"; client part {@code webs}). Two spruce trunks in a grass clearing
 * with a giant spider that has spun an orb web between them (daylight: close, far, at an angle, and a torn one), and a
 * cave nest in a closed stone room (dark: by torchlight and with night vision). Each shot at 16x and 32x.
 */
@SuppressWarnings("UnstableApiUsage")
public class WebsClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("webs")) {
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
			server.runCommand("time set 2000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			context.waitTicks(5);
			// --- the clearing: two spruce trunks 6 apart, leaves on top --------------------------------------
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 12, x + 12, y + 14, z + 12));
			server.runCommand("fill %d %d %d %d %d %d minecraft:podzol".formatted(x - 12, y - 1, z - 12, x + 12, y - 1, z + 12));
			int ax = x - 3;
			int bx = x + 3;
			int tz = z + 6;
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_log".formatted(ax, y, tz, ax, y + 11, tz));
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_log".formatted(bx, y, tz, bx, y + 11, tz));
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_leaves[persistent=true]".formatted(ax - 2, y + 10, tz - 2, ax + 2, y + 12, tz + 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_leaves[persistent=true]".formatted(bx - 2, y + 10, tz - 2, bx + 2, y + 12, tz + 2));
			context.waitTicks(5);
			// More of the forest: three more trunks behind and to the sides.
			int[][] more = {{x, tz + 5}, {x - 7, tz + 3}, {x + 7, tz + 4}};
			for (int[] t : more) {
				server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_log".formatted(t[0], y, t[1], t[0], y + 12, t[1]));
				server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_leaves[persistent=true]".formatted(t[0] - 2, y + 10, t[1] - 2, t[0] + 2, y + 13, t[1] + 2));
			}
			context.waitTicks(5);
			int anchors = onServer(server, level -> {
				GiantSpider giant = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.COMMAND);
				giant.setNoAi(true);
				level.addFreshEntity(giant);
				// It spins where it stands: between the front pair first, then deeper in.
				double[][] stands = {{x + 0.5, tz + 2.5}, {x - 4.5, tz + 4.5}, {x + 4.5, tz + 5.5}};
				int total = 0;
				for (double[] st : stands) {
					giant.snapTo(st[0], y, st[1], 160.0F, 0.0F);
					total += giant.spin(level);
					io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTerritories.release(level, giant.getUUID()); // these scenes predate territories
				}
				giant.snapTo(x + 0.5, y, tz - 2.5, 160.0F, 0.0F);
				return total;
			});
			if (anchors == 0) {
				throw new AssertionError("the giant spun nothing between the trunks");
			}
			context.waitTicks(10);
			hud(context, false);
			for (boolean x32 : new boolean[] {false, true}) {
				setPack(context, x32);
				String sfx = x32 ? "-32x" : "";
				tp(context, server, x + 0.5, y + 4.5, tz - 4.0, 0.0F, 8.0F);
				context.takeScreenshot("web-trees-close" + sfx);
				tp(context, server, x + 0.5, y + 6.0, tz - 16.0, 0.0F, 6.0F);
				context.takeScreenshot("web-trees-far" + sfx);
				tp(context, server, x - 9.5, y + 2.0, tz - 6.0, -35.0F, -12.0F);
				context.takeScreenshot("web-forest-under" + sfx);
				tp(context, server, x - 5.5, y + 3.0, tz - 5.0, -40.0F, -8.0F);
				context.takeScreenshot("web-trees-angle" + sfx);
			}
			setPack(context, false);
			// A giant hauling a pig up a trunk rear first, the thread from its abdomen to the web (owner).
			server.runCommand("summon minecraft:sheep %.2f %d %.2f {NoAI:1b,Tags:[\"web_shot_sheep\"]}".formatted(ax + 2.5, y, tz - 1.5));
			context.waitTicks(3);
			onServer(server, level -> {
				GiantSpider hauler = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.COMMAND);
				hauler.snapTo(ax + 2.0, y, tz + 0.5, 90.0F, 0.0F);
				hauler.setSilent(true);
				level.addFreshEntity(hauler);
				net.minecraft.world.entity.animal.sheep.Sheep pig = level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class,
						new AABB(ax - 2, y - 1, tz - 4, ax + 6, y + 3, tz + 3)).getFirst();
				if (!hauler.snatch(level, pig)) {
					throw new AssertionError("the giant didn't snatch the pig");
				}
				return null;
			});
			for (int i = 0; i < 200; i++) {
				boolean up = onServer(server, level -> level.getEntitiesOfClass(GiantSpider.class, new AABB(ax - 3, y - 1, tz - 4, ax + 6, y + 12, tz + 4),
						g -> g.haulingUp() && g.getY() > y + 3.2).size() == 1);
				if (up) {
					break;
				}
				context.waitTick();
			}
			System.out.println("[web-shot] " + onServer(server, level -> level.getEntitiesOfClass(GiantSpider.class, new AABB(ax - 3, y - 1, tz - 4, ax + 6, y + 12, tz + 4))
					.stream().map(g -> g.syncedTask() + "@" + g.position() + " larder " + g.syncedLarder() + " noAi " + g.isNoAi()).toList()));
			tp(context, server, ax - 4.5, y + 4.0, tz - 4.0, -45.0F, 0.0F);
			context.takeScreenshot("giant-haul-rear-first");
			// Up under the canopy: it wraps the sheep, backs off along the underside, upside down (owner), and waits.
			for (int i = 0; i < 300; i++) {
				boolean waiting = onServer(server, level -> level.getEntitiesOfClass(GiantSpider.class, new AABB(ax - 4, y - 1, tz - 5, ax + 6, y + 14, tz + 5),
						g -> g.task() == GiantSpider.Task.WAIT).size() == 1);
				if (waiting) {
					break;
				}
				context.waitTick();
			}
			context.waitTicks(30);
			System.out.println("[web-shot] perched " + onServer(server, level -> level.getEntitiesOfClass(GiantSpider.class,
					new AABB(ax - 4, y - 1, tz - 5, ax + 6, y + 14, tz + 5)).stream().map(g -> g.task() + "@" + g.position() + " under " + g.perchedUnder()).toList()));
			tp(context, server, ax + 5.5, y + 6.5, tz - 4.5, 50.0F, -10.0F);
			context.takeScreenshot("giant-cocoon-sheep-under-canopy");
			tp(context, server, ax + 0.5, y + 2.0, tz - 3.5, 0.0F, -60.0F);
			context.takeScreenshot("giant-cocoon-sheep-from-below");
			// The spinner takes a sheep to the centre of its own web, then comes down and prowls below it.
			server.runCommand("summon minecraft:sheep %.2f %d %.2f {NoAI:1b,Tags:[\"web_shot_sheep2\"]}".formatted(x + 0.5, y, tz - 2.5));
			context.waitTicks(3);
			Vec3 hub = onServer(server, level -> {
				GiantSpider spinner = level.getEntitiesOfClass(GiantSpider.class, new AABB(x - 3, y - 1, tz - 5, x + 4, y + 3, tz)).stream()
						.filter(GiantSpider::isNoAi).findFirst().orElseThrow();
				spinner.setNoAi(false);
				spinner.setSilent(true);
				net.minecraft.world.entity.animal.sheep.Sheep sheep = level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class,
						new AABB(x - 1, y - 1, tz - 4, x + 2, y + 3, tz - 1), s -> s.entityTags().contains("web_shot_sheep2")).getFirst();
				if (!spinner.snatch(level, sheep) || !spinner.webLarder()) {
					throw new AssertionError("the spinner didn't take the sheep to its own web");
				}
				return Vec3.atCenterOf(spinner.larder());
			});
			GiantSpider.Task[] seen = {null};
			for (int i = 0; i < 400 && seen[0] != GiantSpider.Task.WAIT; i++) {
				seen[0] = onServer(server, level -> level.getEntitiesOfClass(GiantSpider.class, new AABB(hub, hub).inflate(3.0), GiantSpider::webLarder)
						.stream().map(GiantSpider::task).findFirst().orElse(null));
				context.waitTick();
			}
			context.waitTicks(20);
			tp(context, server, hub.x, hub.y - 1.0, hub.z - 6.0, 0.0F, 5.0F);
			context.takeScreenshot("giant-cocoon-own-web-hub");
			onServer(server, level -> {
				level.getEntitiesOfClass(GiantSpider.class, new AABB(hub, hub).inflate(3.0), GiantSpider::webLarder).forEach(GiantSpider::endWait);
				return null;
			});
			for (int i = 0; i < 300 && seen[0] != GiantSpider.Task.PROWL; i++) {
				seen[0] = onServer(server, level -> level.getEntitiesOfClass(GiantSpider.class, new AABB(hub, hub).inflate(12.0), GiantSpider::webLarder)
						.stream().map(GiantSpider::task).findFirst().orElse(null));
				context.waitTick();
			}
			context.waitTicks(80);
			tp(context, server, hub.x, hub.y + 1.0, hub.z - 9.0, 0.0F, 20.0F);
			context.takeScreenshot("giant-prowls-below-its-web");
			// A torn, old web.
			onServer(server, level -> {
				webs(level, x, y, z).forEach(w -> w.setTear(0.6F));
				return null;
			});
			context.waitTicks(5);
			tp(context, server, x + 0.5, y + 4.5, tz - 4.0, 0.0F, 8.0F);
			context.takeScreenshot("web-trees-torn");

			// --- a dark cave nest: a closed stone room 14 high, far below -----------------------------------
			int cy = y + 30;
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 9, cy - 1, z - 9, x + 9, cy + 16, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, cy, z - 8, x + 8, cy + 14, z + 8));
			context.waitTicks(10);
			onServer(server, level -> {
				BlockPos top = new BlockPos(x, cy + 14, z);
				int drop = SpiderNestFeature.findCeiling(level, x, z, cy + 15, cy, RandomSource.create(11));
				if (drop == Integer.MIN_VALUE) {
					throw new AssertionError("no nest in the test room");
				}
				return null;
			});
			server.runCommand("setblock %d %d %d minecraft:torch".formatted(x + 2, cy, z - 3));
			context.waitTicks(20);
			tp(context, server, x + 0.5, cy + 2.0, z - 6.5, 0.0F, -35.0F);
			context.takeScreenshot("web-cave-torch");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			context.waitTicks(5);
			context.takeScreenshot("web-cave-nightvision");
			setPack(context, true);
			context.takeScreenshot("web-cave-nightvision-32x");
			tp(context, server, x + 0.5, cy + 9.0, z - 4.5, 0.0F, -20.0F);
			context.takeScreenshot("web-cave-close-32x");
			setPack(context, false);
			hud(context, true);
		}
	}

	private static List<SpunWeb> webs(ServerLevel level, int x, int y, int z) {
		return level.getEntitiesOfClass(SpunWeb.class, new AABB(x - 30, y - 60, z - 30, x + 30, y + 30, z + 30));
	}

	private static void hud(ClientGameTestContext context, boolean shown) {
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden() == shown) {
				mc.gui.hud.toggle();
			}
		});
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

	private static <T> T onServer(TestServerContext server, Function<ServerLevel, T> action) {
		return server.computeOnServer(s -> action.apply(s.overworld()));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(3);
	}
}

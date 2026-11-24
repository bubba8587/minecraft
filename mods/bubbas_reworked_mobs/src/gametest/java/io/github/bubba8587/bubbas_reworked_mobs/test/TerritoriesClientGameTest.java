// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpiderTerritories;
import io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders;
import io.github.bubba8587.bubbas_reworked_mobs.spider.SpunWeb;
import io.github.bubba8587.bubbas_reworked_mobs.spider.WebSettleGoal;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * A giant's territory by eye (specs/spiders.md "Territories"; client part {@code territories}): a spruce grove where a
 * giant has spun its home web and subsidiary webs, and a vanilla spider that settled at a subsidiary web resting up in
 * it. Shots: territory-overview, territory-home-web, territory-spider-resting (close, from below, 32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class TerritoriesClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("territories")) {
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
			// --- a spruce grove: trunks about 5 apart, a small crown on each ---------------------------------
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 22, y, z - 22, x + 22, y + 16, z + 22));
			server.runCommand("fill %d %d %d %d %d %d minecraft:podzol".formatted(x - 22, y - 1, z - 22, x + 22, y - 1, z + 22));
			context.waitTicks(5);
			for (int gx = -20; gx <= 20; gx += 5) {
				for (int gz = -20; gz <= 20; gz += 5) {
					int tx = x + gx + Math.floorMod(gx * 7 + gz * 3, 3) - 1;
					int tz = z + gz + Math.floorMod(gx * 5 + gz * 11, 3) - 1;
					int top = y + 11 + Math.floorMod(gx + gz, 3);
					server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_log".formatted(tx, y, tz, tx, top, tz));
					server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_leaves[persistent=true]".formatted(tx - 1, top - 1, tz - 1, tx + 1, top + 1, tz + 1));
				}
			}
			context.waitTicks(10);
			// --- the giant spins its home web, then subsidiary webs round its territory ----------------------
			String spun = onServer(server, level -> {
				GiantSpider giant = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.COMMAND);
				giant.setNoAi(true);
				giant.setSilent(true);
				giant.snapTo(x + 0.5, y, z + 0.5, 160.0F, 0.0F);
				level.addFreshEntity(giant);
				giant.spin(level);
				SpiderTerritories.Territory t = giant.territory();
				if (t == null) {
					throw new AssertionError("no territory claimed");
				}
				for (int i = 0; i < 80 && t.webs().size() < 5; i++) {
					double sx = (t.minX << 4) + 3 + (i * 7) % 26;
					double sz = (t.minZ << 4) + 3 + (i * 11) % 26;
					giant.snapTo(sx + 0.5, y, sz + 0.5, 160.0F, 0.0F);
					giant.spin(level);
				}
				SpunWeb home = SpiderTerritories.home(level, t);
				giant.snapTo(home.getX() + 2.0, y, home.getZ() - 2.0, 160.0F, 0.0F);
				return t.minX + "," + t.minZ + " webs " + t.webs().stream().map(w -> w.hub().toShortString()).toList();
			});
			System.out.println("[territory-shot] territory " + spun);
			// --- a vanilla spider in the territory settles at a subsidiary web and rests in it ---------------
			Vec3 sub = onServer(server, level -> {
				SpiderTerritories.Territory t = SpiderTerritories.all(level).getFirst();
				SpunWeb w = SpiderTerritories.subsidiaryWebs(level, t).getFirst();
				Spider spider = EntityTypes.SPIDER.create(level, EntitySpawnReason.COMMAND);
				spider.snapTo(w.getX() + 3.0, y, w.getZ() + 1.0, 0.0F, 0.0F);
				spider.setPersistenceRequired();
				spider.setSilent(true);
				level.addFreshEntity(spider);
				return w.position();
			});
			boolean resting = false;
			for (int i = 0; i < 2400 && !resting; i++) {
				context.waitTick();
				if (i % 10 == 0) {
					resting = onServer(server, level -> !level.getEntitiesOfClass(Spider.class, new net.minecraft.world.phys.AABB(sub, sub).inflate(10.0),
							s -> !(s instanceof GiantSpider) && WebSettleGoal.resting(s) && s.position().y > y + 2.0).isEmpty());
				}
			}
			System.out.println("[territory-shot] spider resting " + resting + " " + onServer(server, level -> level.getEntitiesOfClass(Spider.class,
					new net.minecraft.world.phys.AABB(sub, sub).inflate(12.0), s -> !(s instanceof GiantSpider)).stream()
					.map(s -> s.position() + " tags " + s.entityTags() + " home " + s.getHomePosition()).toList()));
			if (!resting) {
				throw new AssertionError("the spider never rested in its web");
			}
			context.waitTicks(60);
			hud(context, false);
			Vec3 home = onServer(server, level -> SpiderTerritories.home(level, SpiderTerritories.all(level).getFirst()).position());
			Vec3 centre = onServer(server, level -> {
				SpiderTerritories.Territory t = SpiderTerritories.all(level).getFirst();
				return new Vec3((t.minX << 4) + 16.0, y + 5.0, (t.minZ << 4) + 16.0);
			});
			view(context, server, centre, 13.0, 3.0, false);
			context.takeScreenshot("territory-overview");
			view(context, server, home, 9.0, -2.0, true);
			context.takeScreenshot("territory-home-web");
			view(context, server, sub, 5.0, -0.5, true);
			context.takeScreenshot("territory-spider-resting");
			tp(context, server, sub.x + 2.0, y + 1.0, sub.z - 2.0, 45.0F, -50.0F);
			context.takeScreenshot("territory-spider-from-below");
			setPack(context, true);
			view(context, server, sub, 5.0, -0.5, true);
			context.takeScreenshot("territory-spider-resting-32x");
			setPack(context, false);
			hud(context, true);
		}
	}

	/**
	 * Puts the camera {@code dist} from {@code target} and {@code rise} above it, on the first of 24 bearings with a clear
	 * line to it (with {@code acrossWeb}, bearings across the web at the target first), looking at it.
	 */
	private static void view(ClientGameTestContext context, TestServerContext server, Vec3 target, double dist, double rise, boolean acrossWeb) {
		Vec3 cam = onServer(server, level -> {
			double prefer = acrossWeb ? 0.0 : Math.PI * 0.25;
			if (acrossWeb) {
				// The web spans between two trunks: find the nearer one at the hub's height, and look across that line.
				for (int r = 1; r <= 4 && prefer == 0.0; r++) {
					for (int s = 0; s < 16; s++) {
						double b = s * Math.PI / 8.0;
						if (level.getBlockState(BlockPos.containing(target.add(-Math.sin(b) * r, 0.0, -Math.cos(b) * r))).is(net.minecraft.tags.BlockTags.LOGS)) {
							prefer = b + Math.PI * 0.5;
							break;
						}
					}
				}
			}
			Vec3 fallback = null;
			for (int i = 0; i < 24; i++) {
				int k = i / 4;
				double a = prefer + (i % 2 == 0 ? k : -k) * Math.PI / 12.0 + (i % 4 >= 2 ? Math.PI : 0.0);
				Vec3 c = target.add(-Math.sin(a) * dist, rise, -Math.cos(a) * dist);
				if (!level.getBlockState(BlockPos.containing(c)).isAir()) {
					continue;
				}
				if (fallback == null) {
					fallback = c;
				}
				var hit = level.clip(new net.minecraft.world.level.ClipContext(c, target, net.minecraft.world.level.ClipContext.Block.COLLIDER,
						net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
				if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
					return c;
				}
			}
			return fallback != null ? fallback : target.add(0, rise, -dist);
		});
		Vec3 d = target.subtract(cam);
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
		tp(context, server, cam.x, cam.y, cam.z, yaw, pitch);
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

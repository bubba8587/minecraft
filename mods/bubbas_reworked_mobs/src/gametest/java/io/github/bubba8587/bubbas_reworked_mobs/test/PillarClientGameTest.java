// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;
import java.util.Locale;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.CoverFinder;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperMind;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperPillar;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Pillar creepers for real (client test part {@code pillar}; specs/creepers.md, "Making its own cover"): on open flat
 * plains and desert a creeper watching an armor stand finds no cover, stacks a 2-high pillar of the ground and peeks
 * from behind it. Screenshots pillar-&lt;biome&gt;-carry (first block over its head), -second (one block down, carrying
 * the next), -peek (from the stand), -peek-near (halfway along the stand's view) and -side.
 */
@SuppressWarnings("UnstableApiUsage")
public class PillarClientGameTest implements FabricClientGameTest {
	private static final String TAG = "pillar_shot";
	private static final int SPACING = 100;

	/** One scene: biome, ground block, block under it. */
	private record Station(String name, String biome, String ground, String under) {
	}

	private static final List<Station> STATIONS = List.of(
			new Station("plains", "plains", "grass_block", "dirt"),
			new Station("desert", "desert", "sand", "sandstone"));

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("pillar")) {
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
			server.runCommand("gamerule mob_griefing true");
			server.runCommand("weather clear");
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @p");
			context.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			for (int i = 0; i < STATIONS.size(); i++) {
				run(context, sp, server, STATIONS.get(i), base.getX() + i * SPACING, y, base.getZ());
			}
			context.runOnClient(mc -> {
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
	}

	private static void run(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, Station s, int x, int y, int z) {
		tp(context, sp, server, x + 0.5, y + 4, z - 4.0, 0.0F, 20.0F);
		server.runCommand("fillbiome %d %d %d %d %d %d minecraft:%s".formatted(x - 30, y - 4, z - 20, x + 30, y + 12, z + 40, s.biome()));
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 30, y, z - 20, x + 30, y + 12, z + 40));
		server.runCommand("fill %d %d %d %d %d %d minecraft:%s".formatted(x - 30, y - 3, z - 20, x + 30, y - 2, z + 40, s.under()));
		server.runCommand("fill %d %d %d %d %d %d minecraft:%s".formatted(x - 30, y - 1, z - 20, x + 30, y - 1, z + 40, s.ground()));
		String tag = TAG + "_" + s.name();
		server.runCommand("summon minecraft:armor_stand %d.5 %d %d.5 {Rotation:[0f,0f],Tags:[\"%s_stand\"]}".formatted(x, y, z, tag));
		server.runCommand("summon minecraft:creeper %d.5 %d %d.5 {Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:[\"%s\"]}".formatted(x, y, z + 14, tag));
		context.waitTicks(2);
		sp.getConnection().waitForChunksRender();

		// Mid-carry: the first block over its head, seen from the side.
		waitFor(context, server, tag, (c, mind, stand) -> c.hasAttached(CreeperPillar.CARRIED) && blocksUp(c, y) == 0 && hold(c), "first carry");
		sideShot(context, sp, server, tag, "pillar-" + s.name() + "-carry");
		release(server, tag);
		waitFor(context, server, tag, (c, mind, stand) -> c.hasAttached(CreeperPillar.CARRIED) && blocksUp(c, y) == 1 && hold(c), "second carry");
		sideShot(context, sp, server, tag, "pillar-" + s.name() + "-second");
		release(server, tag);

		// Built: peeking from behind it, from the stand's eyes, then from the side.
		waitFor(context, server, tag, (c, mind, stand) -> {
			CoverFinder.PeekSpot spot = mind.spot();
			return mind.pillar() != null && spot != null && mind.spotReached() && c.position().distanceTo(spot.half()) < 0.05 && hold(c);
		}, "peek");
		context.waitTicks(10);
		tp(context, sp, server, x + 0.5, y + 0.16, z + 0.9, 0.0F, 3.0F);
		context.takeScreenshot("pillar-" + s.name() + "-peek");
		Vec3 pillar = server.computeOnServer(srv -> {
			Creeper c = creeper(srv, tag);
			BlockPos p = c == null ? null : CreeperAccess.mind(c).pillar();
			return p == null ? null : Vec3.atBottomCenterOf(p);
		});
		if (pillar != null) {
			// Halfway along the stand's line of sight to the pillar.
			double nearX = x + 0.5 + (pillar.x - x - 0.5) * 0.55;
			double nearZ = z + 0.5 + (pillar.z - z - 0.5) * 0.55;
			tp(context, sp, server, nearX, y + 0.16, nearZ, yawToward(x + 0.5, z + 0.5, pillar.x, pillar.z), 3.0F);
			context.takeScreenshot("pillar-" + s.name() + "-peek-near");
			double camX = pillar.x + 5.0;
			double camZ = pillar.z - 1.0;
			tp(context, sp, server, camX, y + 1.0, camZ, yawToward(camX, camZ, pillar.x, pillar.z + 1.0), 12.0F);
			context.takeScreenshot("pillar-" + s.name() + "-side");
		}
		release(server, tag);
		server.runCommand("kill @e[tag=%s]".formatted(tag));
		server.runCommand("kill @e[tag=%s_stand]".formatted(tag));
	}

	/** Holds the creeper still for a shot (no AI: its goals pause where they are). */
	private static boolean hold(Creeper creeper) {
		creeper.setNoAi(true);
		return true;
	}

	private static void release(TestServerContext server, String tag) {
		server.runOnServer(srv -> {
			Creeper c = creeper(srv, tag);
			if (c != null) {
				c.setNoAi(false);
			}
		});
	}

	/** Blocks standing on the ground (y) within 6 of the creeper: its pillar so far. */
	private static int blocksUp(Creeper creeper, int y) {
		int n = 0;
		BlockPos at = creeper.blockPosition();
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				if (!creeper.level().getBlockState(new BlockPos(at.getX() + dx, y, at.getZ() + dz)).isAir()) {
					n++;
				}
			}
		}
		return n;
	}

	private interface Check {
		boolean test(Creeper creeper, CreeperMind mind, LivingEntity stand);
	}

	/** Ticks until {@code check} holds (the creeper keeps watching the stand meanwhile). */
	private static void waitFor(ClientGameTestContext context, TestServerContext server, String tag, Check check, String what) {
		for (int i = 0; i < 1200; i++) {
			boolean ok = server.computeOnServer(srv -> {
				Creeper creeper = creeper(srv, tag);
				LivingEntity stand = null;
				for (Entity e : srv.overworld().getAllEntities()) {
					if (e instanceof LivingEntity l && e.entityTags().contains(tag + "_stand")) {
						stand = l;
					}
				}
				if (creeper == null || stand == null) {
					return false;
				}
				CreeperMind mind = CreeperAccess.mind(creeper);
				mind.watch(stand, creeper.level().getGameTime());
				return check.test(creeper, mind, stand);
			});
			if (ok) {
				System.out.println("[pillar-shot] " + what + " after " + i + " ticks");
				return;
			}
			context.waitTick();
		}
		throw new AssertionError("pillar creeper never reached: " + what);
	}

	private static Creeper creeper(net.minecraft.server.MinecraftServer srv, String tag) {
		for (Entity e : srv.overworld().getAllEntities()) {
			if (e instanceof Creeper c && e.entityTags().contains(tag)) {
				return c;
			}
		}
		return null;
	}

	/** From 4 blocks to its side, a little above, facing it. */
	private static void sideShot(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, String tag, String name) {
		Vec3 at = server.computeOnServer(srv -> {
			Creeper c = creeper(srv, tag);
			return c == null ? Vec3.ZERO : c.position();
		});
		double camX = at.x + 4.0;
		double camZ = at.z - 1.5;
		tp(context, sp, server, camX, at.y + 1.2, camZ, yawToward(camX, camZ, at.x, at.z), 14.0F);
		context.takeScreenshot(name);
	}

	private static float yawToward(double x, double z, double tx, double tz) {
		return (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
	}

	private static void tp(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		sp.getConnection().waitForChunksRender();
		context.waitTicks(2);
	}
}

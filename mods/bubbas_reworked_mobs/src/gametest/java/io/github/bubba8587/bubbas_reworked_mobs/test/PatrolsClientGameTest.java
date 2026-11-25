// dte:B15,B17,B21,C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPatrols;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSite;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Camp patrols by eye (specs/monster-camps.md "Camp patrols"; client part {@code patrols}): at night, a foot patrol
 * (patrol-foot-&lt;kind&gt;) and mounted ones (patrol-mounted-&lt;kind&gt;), close enough to see the leader's banner and the
 * riders' spears. Also prints how far a mounted patrol's leader rides in 100 ticks (riders drive their mounts).
 */
@SuppressWarnings("UnstableApiUsage")
public class PatrolsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("patrols")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 18000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			Object[][] shots = { { CampKind.ZOMBIE, false }, { CampKind.SKELETON, false }, { CampKind.ZOMBIE, true }, { CampKind.SKELETON, true },
					{ CampKind.HUSK, true } };
			// Riders drive their mounts: a mounted patrol with its AI on.
			String ride = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				CampSite site = camp(level, base.offset(0, 0, -60), CampKind.ZOMBIE);
				BlockPos at = ground(level, base.offset(0, 0, -30));
				List<Mob> patrol = CampPatrols.spawn(level, site, at, at.offset(40, 0, 0), true, RandomSource.create(2));
				Mob leader = patrol.getFirst();
				return leader.getUUID() + " " + leader.position().x;
			});
			context.waitTicks(100);
			String moved = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				String[] parts = ride.split(" ");
				Entity leader = level.getEntity(java.util.UUID.fromString(parts[0]));
				double dx = leader == null ? Double.NaN : leader.position().x - Double.parseDouble(parts[1]);
				String riding = leader == null ? "gone" : String.valueOf(leader.getVehicle());
				for (Entity e : level.getAllEntities()) {
					if (e.entityTags().contains(CampTuning.PATROL_TAG) || e.entityTags().contains(CampTuning.PATROL_MOUNT_TAG)) {
						e.discard();
					}
				}
				return "%.1f blocks, riding %s".formatted(dx, riding);
			});
			System.out.println("[patrols] mounted leader moved " + moved + " in 100 ticks");
			for (int i = 0; i < shots.length; i++) {
				CampKind kind = (CampKind) shots[i][0];
				boolean mounted = (boolean) shots[i][1];
				BlockPos spot = base.offset(i * 40, 0, 20);
				tp(context, server, spot.getX() + 0.5, spot.getY() + 10, spot.getZ() - 6.5, 0, 30);
				sp.getConnection().waitForChunksRender();
				BlockPos at = server.computeOnServer(s -> {
					ServerLevel level = s.overworld();
					CampSite site = camp(level, spot.offset(0, 0, 40), kind);
					BlockPos g = ground(level, spot);
					List<Mob> patrol = CampPatrols.spawn(level, site, g, g.offset(0, 0, 20), mounted, RandomSource.create(7L + kind.ordinal()));
					for (Mob mob : patrol) {
						freeze(mob, 180.0F);
						if (mob.getVehicle() instanceof Mob mount) {
							freeze(mount, 180.0F);
						}
					}
					return g;
				});
				context.waitTicks(10);
				double eye = mounted ? 2.6 : 1.9;
				String name = "patrol-" + (mounted ? "mounted-" : "foot-") + kind.id();
				tp(context, server, at.getX() + 0.5, at.getY() + eye - 1.6, at.getZ() - 4.5, 0, 8);
				context.takeScreenshot(name);
				tp(context, server, at.getX() + 3.5, at.getY() + eye - 1.0, at.getZ() - 3.5, 45, 15);
				context.takeScreenshot(name + "-side");
			}
		}
	}

	private static CampSite camp(ServerLevel level, BlockPos at, CampKind kind) {
		CampPiece piece = CampCommand.build(level, at, kind, CampTuning.Size.LARGE, 40L + kind.ordinal());
		return CampSites.get(level, net.minecraft.world.level.ChunkPos.containing(piece.centre()).pack());
	}

	private static BlockPos ground(ServerLevel level, BlockPos pos) {
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
	}

	private static void freeze(Mob mob, float yaw) {
		mob.setNoAi(true);
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}

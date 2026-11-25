// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglin;
import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglins;
import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.ThroneDesign;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The Boss Piglin by eye (specs/boss-piglin.md; client part {@code bosspiglin}). On a blackstone platform in the sky,
 * night vision on. Shots: bosspiglin-seated-front, -seated-34, -throne (empty), -waddle-side (walking off), -armour-close,
 * -cape-back, and -seated-front-32x, -cape-back-32x with the 32x pack.
 */
@SuppressWarnings("UnstableApiUsage")
public class BossPiglinClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("bosspiglin")) {
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
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 2, z - 8));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:polished_blackstone".formatted(x - 16, y - 1, z - 12, x + 24, y - 1, z + 12));
			server.runCommand("gamemode spectator @p");
			context.waitTicks(5);

			// --- a seated boss on its throne (facing north, toward the camera), an empty throne beside it -----
			BlockPos anchor = new BlockPos(x, y, z);
			BlockPos empty = new BlockPos(x + 8, y, z);
			Vec3 seat = onServer(server, level -> {
				throne(level, anchor);
				throne(level, empty);
				Vec3 at = Vec3.atLowerCornerOf(anchor).add(ThroneDesign.seat(Direction.NORTH));
				BossPiglin boss = boss(level, at, 180.0F);
				boss.seatAt(at, 180.0F);
				boss.setNoAi(true);
				level.addFreshEntity(boss);
				return at;
			});
			context.waitTicks(30);
			tp(context, server, seat.x, y + 1.2, z - 5.5, 0.0F, 5.0F);
			context.takeScreenshot("bosspiglin-seated-front");
			tp(context, server, seat.x - 4.0, y + 1.6, z - 4.5, -40.0F, 10.0F);
			context.takeScreenshot("bosspiglin-seated-34");
			tp(context, server, empty.getX() - 0.5, y + 1.2, z - 5.0, 0.0F, 10.0F);
			context.takeScreenshot("bosspiglin-throne");
			tp(context, server, seat.x - 0.2, y + 1.6, z - 2.6, 0.0F, 18.0F);
			context.takeScreenshot("bosspiglin-armour-close");

			// --- a standing boss seen from behind (the cape), and one waddling off to the east ---------------
			onServer(server, level -> {
				BossPiglin back = boss(level, new Vec3(x - 8.5, y, z + 0.5), 0.0F);
				back.setNoAi(true);
				level.addFreshEntity(back);
				BossPiglin walker = boss(level, new Vec3(x + 4.5, y, z + 8.5), -90.0F);
				level.addFreshEntity(walker);
				return null;
			});
			context.waitTicks(20);
			tp(context, server, x - 8.5, y + 1.6, z - 3.5, 0.0F, 12.0F);
			context.takeScreenshot("bosspiglin-cape-back");
			onServer(server, level -> {
				for (BossPiglin b : level.getEntitiesOfClass(BossPiglin.class, new net.minecraft.world.phys.AABB(x - 30, y - 5, z - 30, x + 40, y + 10, z + 30),
						b -> !b.isNoAi())) {
					b.getNavigation().moveTo(x + 20.5, y, z + 8.5, 1.0);
				}
				return null;
			});
			context.waitTicks(14);
			double wx = context.computeOnClient(mc -> mc.level.getEntitiesOfClass(BossPiglin.class,
					new net.minecraft.world.phys.AABB(x - 30, y - 5, z - 30, x + 40, y + 10, z + 30), b -> b.getZ() > z + 5).stream()
					.findFirst().map(b -> b.getX()).orElse(x + 8.0));
			tp(context, server, wx, y + 1.2, z + 2.5, 0.0F, 5.0F);
			context.takeScreenshot("bosspiglin-waddle-side");

			// --- the 32x pack ----------------------------------------------------------------------------
			setPack(context, true);
			tp(context, server, seat.x, y + 1.2, z - 5.5, 0.0F, 5.0F);
			context.takeScreenshot("bosspiglin-seated-front-32x");
			tp(context, server, x - 8.5, y + 1.6, z - 3.5, 0.0F, 12.0F);
			context.takeScreenshot("bosspiglin-cape-back-32x");
			setPack(context, false);
		}
	}

	private static void throne(ServerLevel level, BlockPos anchor) {
		for (ThroneDesign.Placed placed : ThroneDesign.blocks(Direction.NORTH)) {
			level.setBlock(anchor.offset(placed.offset()), placed.state(), 2);
		}
		BossPiglins.placeCushions(level, anchor, Direction.NORTH, p -> true);
	}

	private static BossPiglin boss(ServerLevel level, Vec3 at, float yaw) {
		BossPiglin boss = BossPiglins.BOSS_PIGLIN.create(level, EntitySpawnReason.COMMAND);
		boss.snapTo(at.x, at.y, at.z, yaw, 0.0F);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), EntitySpawnReason.COMMAND, null);
		boss.setImmuneToZombification(true);
		boss.setYHeadRot(yaw);
		boss.setYBodyRot(yaw);
		boss.setSilent(true);
		boss.setPersistenceRequired();
		return boss;
	}

	private static <T> T onServer(TestServerContext server, Function<ServerLevel, T> action) {
		return server.computeOnServer(s -> action.apply(s.overworld()));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(3);
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

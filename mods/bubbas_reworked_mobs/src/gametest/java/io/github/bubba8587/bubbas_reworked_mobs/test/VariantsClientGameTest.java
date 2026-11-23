// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.bogged.BoggedLurk;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Bogged;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Husk and stray sleepers and the lurking bogged by eye (specs/undead.md "Undead variants"; client part
 * {@code variants}). On a sand and snow strip: a husk and a stray sleeper in the open (variants-sleepers), the stray
 * frozen in clear ice and a husk under sand (variants-buried), the husk bursting out (variants-husk-burst) and the
 * stray's ice cracking then breaking (variants-stray-crack, variants-stray-free), the woken pair (variants-woken), and
 * a bogged crouched in a reedy pool beside a standing one (variants-bogged).
 */
@SuppressWarnings("UnstableApiUsage")
public class VariantsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("variants")) {
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
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 10, x + 10, y + 6, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:sand".formatted(x - 10, y - 1, z - 10, x - 1, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:snow_block".formatted(x, y - 1, z - 10, x + 10, y - 1, z + 10));

			// Row 1 (z - 6): in the open. Row 2 (z - 2): buried / frozen.
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:husk_sleeper[pose=lying,facing=east]".formatted(x - 3, y, z - 6));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:stray_sleeper[pose=sitting,facing=south]".formatted(x + 2, y, z - 6));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:husk_sleeper[pose=sitting,facing=south]".formatted(x - 5, y, z - 6));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:stray_sleeper[pose=lying,facing=east]".formatted(x + 4, y, z - 6));
			// The buried husk lies in a trench under sand; the stray sits in a block of clear ice.
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:husk_sleeper[pose=lying,facing=east]".formatted(x - 3, y - 1, z - 2));
			server.runCommand("setblock %d %d %d minecraft:sand".formatted(x - 3, y, z - 2));
			server.runCommand("setblock %d %d %d minecraft:sand".formatted(x - 2, y, z - 2));
			server.runCommand("setblock %d %d %d bubbas_reworked_mobs:stray_sleeper[pose=lying,facing=east]".formatted(x + 3, y - 1, z - 2));
			server.runCommand("setblock %d %d %d minecraft:ice".formatted(x + 3, y, z - 2));
			server.runCommand("setblock %d %d %d minecraft:ice".formatted(x + 4, y, z - 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:ice".formatted(x + 2, y - 1, z - 2, x + 2, y - 1, z - 2));
			context.waitTicks(10);

			tp(context, server, x + 0.5, y + 2.0, z + 1.5, 180, 25);
			context.takeScreenshot("variants-sleepers");
			tp(context, server, x + 0.5, y + 2.5, z + 3.5, 180, 30);
			context.takeScreenshot("variants-buried");

			// The husk bursts out (shot a few ticks in, in the cloud).
			wake(server, new BlockPos(x - 3, y - 1, z - 2));
			context.waitTicks(3);
			context.takeScreenshot("variants-husk-burst");
			// The stray: its ice cracks (the warning), then it breaks free.
			wake(server, new BlockPos(x + 3, y - 1, z - 2));
			context.waitTicks(4);
			context.takeScreenshot("variants-stray-crack");
			context.waitTicks(30);
			context.takeScreenshot("variants-stray-free");
			server.runCommand("time set 13000"); // not burning in the shot
			context.waitTicks(20);
			tp(context, server, x + 0.5, y + 1.5, z + 4.5, 180, 15);
			context.takeScreenshot("variants-woken");
			server.runCommand("kill @e[type=!player]");

			// The bogged: a reedy pool two deep, one lurking crouched, one standing on the bank.
			int bx = x;
			int bz = z + 8;
			server.runCommand("fill %d %d %d %d %d %d minecraft:mud".formatted(bx - 5, y - 1, bz - 3, bx + 5, y - 1, bz + 3));
			server.runCommand("fill %d %d %d %d %d %d minecraft:mud".formatted(bx - 2, y - 3, bz - 2, bx + 2, y - 3, bz + 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(bx - 2, y - 2, bz - 2, bx + 2, y - 1, bz + 2));
			server.runCommand("setblock %d %d %d minecraft:sugar_cane".formatted(bx - 3, y, bz));
			server.runCommand("setblock %d %d %d minecraft:sugar_cane".formatted(bx - 3, y + 1, bz));
			server.runCommand("setblock %d %d %d minecraft:tall_grass".formatted(bx + 3, y, bz - 1));
			server.runCommand("setblock %d %d %d minecraft:lily_pad".formatted(bx + 1, y, bz + 1));
			server.runOnServer(s -> {
				var level = s.overworld();
				Bogged lurker = EntityTypes.BOGGED.create(level, EntitySpawnReason.COMMAND);
				lurker.snapTo(bx + 0.5, y - 2, bz + 0.5, 0, 0);
				level.addFreshEntity(lurker);
				BoggedLurk.lurkAt(lurker, new BlockPos(bx, y - 2, bz));
				Bogged stander = EntityTypes.BOGGED.create(level, EntitySpawnReason.COMMAND);
				stander.snapTo(bx + 4.5, y, bz + 2.5, 0, 0);
				stander.setNoAi(true);
				level.addFreshEntity(stander);
			});
			context.waitTicks(20);
			tp(context, server, bx + 0.5, y + 2.5, bz - 6.5, 0, 25);
			context.takeScreenshot("variants-bogged");
		}
	}

	private static void wake(TestServerContext server, BlockPos pos) {
		server.runOnServer(s -> {
			if (s.overworld().getBlockEntity(pos) instanceof SleeperBlockEntity sleeper) {
				sleeper.scheduleWake(s.overworld(), null, null, 1);
			} else {
				throw new AssertionError("no sleeper at " + pos);
			}
		});
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import java.util.stream.Stream;

import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolem;
import io.github.bubba8587.bubbas_world_tweaks.golem.AmethystGolems;
import io.github.bubba8587.bubbas_world_tweaks.golem.GolemSong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Amethyst Golem and its spawn egg by eye, at 16x and 32x (client test part "golem"). */
@SuppressWarnings("UnstableApiUsage")
public class AmethystGolemClientGameTest implements FabricClientGameTest {
	/** Ticks between the notes of the singing test song. */
	private static final int NOTE_SPACING = 16;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("golem")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY() + 12;
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 2, z - 3));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 6, x + 6, y + 6, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:polished_andesite".formatted(x - 5, y - 1, z - 5, x + 5, y - 1, z + 5));

			// Built from the blocks the vanilla way, then held still facing the camera
			server.runCommand("setblock %d %d %d minecraft:amethyst_block".formatted(x, y, z));
			server.runCommand("setblock %d %d %d minecraft:carved_pumpkin".formatted(x, y + 1, z));
			context.waitTicks(5);
			int built = server.computeOnServer(s -> s.overworld().getEntities(AmethystGolems.AMETHYST_GOLEM, e -> true).size());
			if (built != 1) {
				throw new AssertionError("expected 1 built amethyst golem, found " + built);
			}
			server.runCommand("execute as @e[type=bubbas_world_tweaks:amethyst_golem] run data merge entity @s {NoAI:1b}");
			server.runCommand("tp @e[type=bubbas_world_tweaks:amethyst_golem] %.1f %d %.1f 180 0".formatted(x + 0.5, y, z + 0.5));
			context.waitTicks(60);

			double gx = x + 0.5;
			double gz = z + 0.5;
			for (boolean x32 : new boolean[] {false, true}) {
				BubbasWorldTweaksClientGameTest.setPack(context, x32);
				String sfx = x32 ? "-32x" : "";
				BubbasWorldTweaksClientGameTest.hud(context, false);
				shot(context, gx, y, gz, 0.0F, 2.4, 0.75, 8.0F, "golem-front" + sfx);
				shot(context, gx, y, gz, -40.0F, 2.4, 0.95, 12.0F, "golem-side" + sfx);
				shot(context, gx, y, gz, 25.0F, 1.3, 1.05, 18.0F, "golem-close" + sfx);
				shot(context, gx, y, gz, 160.0F, 2.4, 0.95, 12.0F, "golem-back" + sfx);
				BubbasWorldTweaksClientGameTest.hud(context, true);
			}
			BubbasWorldTweaksClientGameTest.setPack(context, false);

			// Art checks: next to a vanilla copper golem and an amethyst cluster, from afar, from above, face close up
			server.runCommand("summon minecraft:copper_golem %.1f %d %.1f {NoAI:1b,Rotation:[180f,0f]}".formatted(gx + 1.5, y, gz));
			server.runCommand("setblock %d %d %d minecraft:amethyst_cluster".formatted(x - 2, y, z));
			server.runCommand("setblock %d %d %d minecraft:amethyst_block".formatted(x - 2, y, z + 1));
			context.waitTicks(10);
			for (boolean x32 : new boolean[] {false, true}) {
				BubbasWorldTweaksClientGameTest.setPack(context, x32);
				String sfx = x32 ? "-32x" : "";
				BubbasWorldTweaksClientGameTest.hud(context, false);
				shot(context, gx, y, gz, 0.0F, 3.6, 0.9, 10.0F, "golem-compare" + sfx);
				shot(context, gx, y, gz, -20.0F, 9.0, 2.0, 10.0F, "golem-far" + sfx);
				shot(context, gx, y, gz, 30.0F, 1.1, 2.4, 60.0F, "golem-top" + sfx);
				shot(context, gx, y, gz, 0.0F, 0.9, 0.85, 8.0F, "golem-face" + sfx);
				BubbasWorldTweaksClientGameTest.hud(context, true);
			}
			BubbasWorldTweaksClientGameTest.setPack(context, false);

			// Spawn egg in hand and in the hotbar
			server.runCommand("gamemode creative @p");
			server.runCommand("item replace entity @p hotbar.0 with bubbas_world_tweaks:amethyst_golem_spawn_egg");
			server.runCommand("tp @p %.1f %d %.1f 0 20".formatted(gx, y, gz - 3.0));
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			for (boolean x32 : new boolean[] {false, true}) {
				BubbasWorldTweaksClientGameTest.setPack(context, x32);
				context.waitTicks(10);
				context.takeScreenshot("golem-egg" + (x32 ? "-32x" : ""));
			}
			BubbasWorldTweaksClientGameTest.setPack(context, false);

			states(context, server, x, y, z);
		}
	}

	/** Behaviour poses: recording, singing on its jukebox (on a note, between notes), waiting for the beat, dancing (16x only). */
	private static void states(ClientGameTestContext context, TestServerContext server, int x, int y, int z) {
		double gx = x + 0.5;
		double gz = z + 0.5;
		server.runCommand("item replace entity @p hotbar.0 with minecraft:air");
		server.runCommand("gamemode spectator @p");
		BubbasWorldTweaksClientGameTest.hud(context, false);

		server.runOnServer(s -> golem(s).toggleRecording(s.overworld()));
		context.waitTicks(10);
		shot(context, gx, y, gz, 0.0F, 2.4, 0.75, 8.0F, "golem-recording");
		shot(context, gx, y, gz, -40.0F, 2.4, 0.95, 12.0F, "golem-recording-side");
		server.runOnServer(s -> golem(s).toggleRecording(s.overworld()));

		// Its own jukebox under it, a song of spaced notes, the day clock running up to a beat
		server.runCommand("setblock %d %d %d minecraft:jukebox".formatted(x, y - 1, z));
		context.waitTicks(5);
		int[] tune = {4, 12, 20, 9, 16, 24, 7, 14, 22, 11};
		server.runOnServer(s -> {
			AmethystGolem golem = golem(s);
			int[] song = new int[tune.length * 3];
			for (int i = 0; i < song.length; i++) {
				song[i] = GolemSong.pack(i * NOTE_SPACING, tune[i % tune.length]);
			}
			golem.setSong(new GolemSong(song));
			if (!golem.claimJukebox(s.overworld())) {
				throw new AssertionError("golem could not claim the jukebox under it");
			}
		});
		server.runCommand("time set 5990");
		server.runCommand("gamerule advance_time true");
		context.waitTicks(30);
		if (!context.computeOnClient(mc -> clientGolems(mc).anyMatch(AmethystGolem::isSinging))) {
			throw new AssertionError("golem on its jukebox is not singing after the beat");
		}

		// The moment of a note (mouth open, crystals flicked), then halfway between two notes
		freezeAfterNote(context, server, 1);
		shot(context, gx, y, gz, 0.0F, 2.4, 0.75, 8.0F, "golem-singing-note");
		shot(context, gx, y, gz, -40.0F, 2.4, 0.95, 12.0F, "golem-singing-note-side");
		shot(context, gx, y, gz, 0.0F, 1.3, 1.05, 18.0F, "golem-singing-note-close");
		server.runOnServer(s -> s.tickRateManager().setFrozen(false));
		freezeAfterNote(context, server, NOTE_SPACING / 2);
		shot(context, gx, y, gz, 0.0F, 2.4, 0.75, 8.0F, "golem-singing-between");
		shot(context, gx, y, gz, -40.0F, 2.4, 0.95, 12.0F, "golem-singing-between-side");
		server.runOnServer(s -> s.tickRateManager().setFrozen(false));

		// Song over, the next beat far off: it waits on the jukebox
		server.runOnServer(s -> golem(s).setSong(golem(s).song()));
		server.runCommand("time set 6200");
		context.waitTicks(20);
		if (!context.computeOnClient(mc -> clientGolems(mc).anyMatch(g -> g.isWaiting() && !g.isSinging()))) {
			throw new AssertionError("golem between songs is not waiting for the beat");
		}
		shot(context, gx, y, gz, 0.0F, 2.4, 0.75, 8.0F, "golem-waiting");
		shot(context, gx, y, gz, -40.0F, 2.4, 0.95, 12.0F, "golem-waiting-side");
		server.runCommand("gamerule advance_time false");

		// A disc in the jukebox: it dances
		server.runOnServer(s -> {
			if (s.overworld().getBlockEntity(new BlockPos(x, y - 1, z)) instanceof JukeboxBlockEntity jukebox) {
				jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
			}
		});
		context.waitTicks(10);
		if (!context.computeOnClient(mc -> clientGolems(mc).anyMatch(AmethystGolem::isDancing))) {
			throw new AssertionError("golem is not dancing to the disc");
		}
		shot(context, gx, y, gz, 0.0F, 2.4, 0.75, 8.0F, "golem-dancing");
		context.waitTicks(7);
		shot(context, gx, y, gz, -40.0F, 2.4, 0.95, 12.0F, "golem-dancing-side");
		BubbasWorldTweaksClientGameTest.hud(context, true);
	}

	/** Freezes the server (and the golem with it) the given number of ticks after one of its notes. */
	private static void freezeAfterNote(ClientGameTestContext context, TestServerContext server, int since) {
		for (int i = 0; i < 400; i++) {
			if (server.computeOnServer(s -> {
				AmethystGolem golem = golem(s);
				if (golem.tickCount - golem.noteTick() != since) {
					return false;
				}
				s.tickRateManager().setFrozen(true);
				return true;
			})) {
				context.waitTicks(3);
				return;
			}
			context.waitTick();
		}
		throw new AssertionError("golem sang no note to freeze on");
	}

	private static Stream<AmethystGolem> clientGolems(Minecraft mc) {
		return mc.level.getEntitiesOfClass(AmethystGolem.class, mc.player.getBoundingBox().inflate(16)).stream();
	}

	private static AmethystGolem golem(MinecraftServer server) {
		return server.overworld().getEntities(AmethystGolems.AMETHYST_GOLEM, e -> true).getFirst();
	}

	/** Camera at a yaw around the golem, dist blocks away, eye height above its feet. */
	private static void shot(ClientGameTestContext context, double gx, int gy, double gz, float yaw, double dist, double eye, float pitch,
			String name) {
		double rad = Math.toRadians(yaw);
		double cx = gx + Math.sin(rad) * dist;
		double cz = gz - Math.cos(rad) * dist;
		context.runOnClient(mc -> mc.player.snapTo(cx, gy + eye - mc.player.getEyeHeight(), cz, yaw, pitch));
		context.waitTicks(4);
		context.takeScreenshot(name);
	}
}

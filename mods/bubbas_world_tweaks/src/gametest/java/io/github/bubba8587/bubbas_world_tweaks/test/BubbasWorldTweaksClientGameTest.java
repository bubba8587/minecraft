// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import io.github.bubba8587.bubbas_world_tweaks.lava.ModParticles;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.packs.repository.PackRepository;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Lava bubbles and a lava geyser by eye, at 16x and 32x. */
@SuppressWarnings("UnstableApiUsage")
public class BubbasWorldTweaksClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_world_tweaks:bubbas_world_tweaks_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("lava")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY() + 10;
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 13000");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 6, z));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:netherrack".formatted(x - 6, y - 3, z - 6, x + 12, y, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:magma_block".formatted(x - 4, y - 3, z - 4, x + 4, y - 3, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:lava".formatted(x - 4, y - 2, z - 4, x + 4, y, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y + 1, z - 6, x + 12, y + 16, z + 6));
			// Vanilla's sulfur geyser beside the pool, erupting through a water pit
			server.runCommand("setblock %d %d %d minecraft:potent_sulfur[state=continuous]".formatted(x + 8, y - 3, z));
			server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(x + 8, y - 2, z, x + 8, y, z));
			context.waitTicks(40);

			for (boolean x32 : new boolean[] {false, true}) {
				setPack(context, x32);
				String sfx = x32 ? "-32x" : "";
				hud(context, false);

				// One bubble close up, from above and from the side
				context.runOnClient(mc -> mc.player.snapTo(x + 0.5, y + 1.6, z - 0.5, 0.0F, 40.0F));
				bubbles(context, x + 0.1, x + 0.9, y, z, z + 0.7, 3, 4);
				context.waitTicks(40);
				context.takeScreenshot("lava-bubble-close" + sfx);
				context.runOnClient(mc -> mc.player.snapTo(x + 0.5, y + 1.05, z - 0.8, 0.0F, 8.0F));
				bubbles(context, x - 0.6, x + 1.6, y, z + 0.6, z + 1.6, 3, 4);
				context.waitTicks(40);
				context.takeScreenshot("lava-bubble-side" + sfx);

				// A bubbling magma patch, medium distance
				context.runOnClient(mc -> mc.player.snapTo(x + 0.5, y + 4.0, z - 5.5, 0.0F, 38.0F));
				for (int wave = 0; wave < 3; wave++) {
					bubbles(context, x - 3.5, x + 3.5, y, z - 3.0, z + 3.0, 2, 20);
				}
				context.takeScreenshot("lava-bubbles-patch" + sfx);

				// Our lava geyser next to vanilla's sulfur geyser
				server.runOnServer(s -> LavaGeysers.erupt(s.overworld(), new BlockPos(x + 2, y, z)));
				for (int puff = 0; puff < 3; puff++) {
					context.runOnClient(mc -> mc.level.addParticle(new GeyserParticleOptions(ParticleTypes.GEYSER, 3), x + 8.5, y + 1.0, z + 0.5,
							0.0, 0.0, 0.0));
					context.waitTicks(15);
				}
				context.runOnClient(mc -> mc.player.snapTo(x + 5.0, y + 5.0, z - 14.0, 0.0F, -8.0F));
				context.waitTicks(3);
				context.takeScreenshot("lava-geyser-vs-sulfur" + sfx);
				context.waitTicks(80);
				hud(context, true);
			}
			setPack(context, false);
		}
	}

	private static void bubbles(ClientGameTestContext context, double x0, double x1, int y, double z0, double z1, int count, int wait) {
		context.runOnClient(mc -> {
			var random = mc.level.getRandom();
			for (int i = 0; i < count; i++) {
				mc.level.addParticle(ModParticles.LAVA_BUBBLE, x0 + random.nextDouble() * (x1 - x0), y + 0.9,
						z0 + random.nextDouble() * (z1 - z0), 0.0, 0.0, 0.0);
			}
		});
		context.waitTicks(wait);
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
}

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import io.github.bubba8587.bubbas_world_tweaks.lava.ModParticles;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Lava bubbles and a lava geyser by eye. */
@SuppressWarnings("UnstableApiUsage")
public class BubbasWorldTweaksClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_world_tweaks:bubbas_world_tweaks_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:netherrack".formatted(x - 6, y - 3, z - 6, x + 6, y, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:magma_block".formatted(x - 4, y - 3, z - 4, x + 4, y - 3, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:lava".formatted(x - 4, y - 2, z - 4, x + 4, y, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y + 1, z - 6, x + 6, y + 14, z + 6));
			context.waitTicks(40);

			for (boolean x32 : new boolean[] {false, true}) {
				setPack(context, x32);
				String sfx = x32 ? "-32x" : "";
				hud(context, false);
				context.runOnClient(mc -> mc.player.snapTo(x + 0.5, y + 3.0, z - 4.5, 0.0F, 38.0F));
				for (int wave = 0; wave < 3; wave++) {
					context.runOnClient(mc -> {
						var random = mc.level.getRandom();
						for (int i = 0; i < 18; i++) {
							mc.level.addParticle(ModParticles.LAVA_BUBBLE, x - 3.5 + random.nextDouble() * 7.0, y + 0.9,
									z - 3.0 + random.nextDouble() * 6.0, 0.0, 0.0, 0.0);
						}
					});
					context.waitTicks(6);
				}
				context.takeScreenshot("lava-bubbles" + sfx);
				context.runOnClient(mc -> mc.player.snapTo(x + 0.5, y + 1.6, z - 1.8, 0.0F, 55.0F));
				context.waitTicks(4);
				context.takeScreenshot("lava-bubbles-close" + sfx);

				server.runOnServer(s -> LavaGeysers.erupt(s.overworld(), new BlockPos(x, y, z)));
				context.waitTicks(25);
				context.runOnClient(mc -> mc.player.snapTo(x + 0.5, y + 4.0, z - 9.5, 0.0F, -12.0F));
				context.waitTicks(3);
				context.takeScreenshot("lava-geyser" + sfx);
				context.waitTicks(110);
				hud(context, true);
			}
			setPack(context, false);
		}
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

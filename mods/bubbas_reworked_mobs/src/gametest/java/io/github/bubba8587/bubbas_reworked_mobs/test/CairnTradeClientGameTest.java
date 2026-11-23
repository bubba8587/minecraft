// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.Locale;

import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnTrades;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Trading at a cairn and sneak + middle click laying, by eye (client part trading). */
@SuppressWarnings("UnstableApiUsage")
public class CairnTradeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("trading")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos origin = context.computeOnClient(mc -> mc.player.blockPosition()).offset(0, 0, 20);
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule bubbas_reworked_mobs:cairn_endermen false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(origin.getX() - 14, origin.getY(), origin.getZ() - 14,
					origin.getX() + 14, origin.getY() + 24, origin.getZ() + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(origin.getX() - 14, origin.getY() - 1, origin.getZ() - 14,
					origin.getX() + 14, origin.getY() - 1, origin.getZ() + 14));
			BlockPos spot = server.computeOnServer(s -> {
				CairnSite site = CairnSites.addSite(s.overworld(), origin, 2026L);
				site.buildAll(s.overworld());
				return site.tradeSpot();
			});
			if (spot == null) {
				throw new AssertionError("the test cairn has no trade pad");
			}
			// A second cairn: its pad sits somewhere else around it
			BlockPos second = origin.offset(40, 0, 0);
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(second.getX() - 14, second.getY(), second.getZ() - 14,
					second.getX() + 14, second.getY() + 24, second.getZ() + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(second.getX() - 14, second.getY() - 1, second.getZ() - 14,
					second.getX() + 14, second.getY() - 1, second.getZ() + 14));
			BlockPos secondSpot = server.computeOnServer(s -> {
				CairnSite site = CairnSites.addSite(s.overworld(), second, 11L);
				site.buildAll(s.overworld());
				return site.tradeSpot();
			});
			System.out.println("[trading] pad offsets: " + spot.subtract(origin) + " and " + (secondSpot == null ? "none" : secondSpot.subtract(second)));
			view(context, server, Vec3.atBottomCenterOf(origin).add(0.01, 30, 0), Vec3.atBottomCenterOf(origin));
			context.takeScreenshot("trade-pad-cairn-a-above");
			view(context, server, Vec3.atBottomCenterOf(second).add(0.01, 30, 0), Vec3.atBottomCenterOf(second));
			context.takeScreenshot("trade-pad-cairn-b-above");
			Vec3 out = Vec3.atCenterOf(spot).subtract(Vec3.atCenterOf(origin)).multiply(1, 0, 1).normalize();
			Vec3 stand = Vec3.atBottomCenterOf(spot.below()).add(out.scale(2.5));

			// The pad, empty
			view(context, server, Vec3.atBottomCenterOf(spot).add(out.scale(5.0)).add(0, 2.5, 0), Vec3.atCenterOf(spot));
			context.takeScreenshot("trade-pad");

			// A survival player lays an item on the pad: sneak + middle click
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.getInventory().clearContent();
				p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.ROTTEN_FLESH, 5));
			});
			view(context, server, stand, new Vec3(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5));
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(5);
			context.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5)));
			context.waitTicks(2);
			context.getInput().pressKey(o -> o.keyPickItem);
			context.waitTicks(5);
			context.getInput().releaseKey(o -> o.keyShift);
			context.waitTicks(3);
			boolean laid = server.computeOnServer(s -> s.overworld().getBlockEntity(spot) instanceof LootDisplayBlockEntity d && d.getItem().is(Items.ROTTEN_FLESH));
			int left = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getMainHandItem().getCount());
			if (!laid || left != 4) {
				throw new AssertionError("sneak + middle click didn't lay one item on the pad (laid " + laid + ", left " + left + ")");
			}
			view(context, server, Vec3.atBottomCenterOf(spot).add(out.scale(3.0)).add(0, 1.6, 0), Vec3.atCenterOf(spot));
			context.takeScreenshot("trade-offered");

			// The player walks off; the endermen take it and answer
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p %d %d %d".formatted(origin.getX(), origin.getY() + 40, origin.getZ() - 30));
			context.waitTicks(5);
			String staring = server.computeOnServer(s -> CairnTrades.step(s.overworld(), CairnSites.covering(s.overworld(), origin)).toString());
			if (!staring.equals("STARING")) {
				throw new AssertionError("no enderman came to stare: " + staring);
			}
			context.waitTicks(10);
			view(context, server, Vec3.atBottomCenterOf(spot).add(out.scale(-3.0)).add(0, 2.2, 0).add(out.cross(new Vec3(0, 1, 0)).scale(3.0)), Vec3.atCenterOf(spot).add(out.scale(1.0)).add(0, 1.0, 0));
			context.takeScreenshot("trade-enderman-staring");
			String step = server.computeOnServer(s -> {
				CairnSite site = CairnSites.covering(s.overworld(), origin);
				CairnTrades.hurryStare(s.overworld(), site);
				CairnTrades.Step taken = CairnTrades.step(s.overworld(), site);
				CairnTrades.hurry(s.overworld(), site);
				return taken + " " + CairnTrades.step(s.overworld(), site);
			});
			if (!step.equals("TAKEN ANSWERED")) {
				throw new AssertionError("trade steps: " + step);
			}
			String answer = server.computeOnServer(s -> s.overworld().getBlockEntity(spot) instanceof LootDisplayBlockEntity d ? d.getItem().toString() : "nothing");
			System.out.println("[trading] answer: " + answer);
			context.waitTicks(10);
			view(context, server, Vec3.atBottomCenterOf(spot).add(out.scale(4.0)).add(0, 2.0, 0), Vec3.atCenterOf(spot));
			context.takeScreenshot("trade-answered");

			// Anywhere: an apple laid on the grass
			BlockPos grass = origin.offset(0, 0, -10);
			server.runCommand("gamemode survival @p");
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE)));
			view(context, server, Vec3.atBottomCenterOf(grass).add(0, 0, -2.5), new Vec3(grass.getX() + 0.5, grass.getY(), grass.getZ() + 0.5));
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(5);
			context.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(grass.getX() + 0.5, grass.getY(), grass.getZ() + 0.5)));
			context.waitTicks(2);
			context.getInput().pressKey(o -> o.keyPickItem);
			context.waitTicks(5);
			context.getInput().releaseKey(o -> o.keyShift);
			boolean onGrass = server.computeOnServer(s -> LootDisplays.hasItemAt(s.overworld(), grass));
			if (!onGrass) {
				throw new AssertionError("sneak + middle click didn't lay the apple on the grass");
			}
			// Plain middle click is still pick block
			context.getInput().pressKey(o -> o.keyPickItem);
			context.waitTicks(5);
			if (server.computeOnServer(s -> LootDisplays.hasItemAt(s.overworld(), grass.above()))) {
				throw new AssertionError("a plain middle click laid an item");
			}
			view(context, server, Vec3.atBottomCenterOf(grass).add(0, 1.2, -2.0), Vec3.atCenterOf(grass));
			context.takeScreenshot("laid-on-grass");
		}
	}

	private static void view(ClientGameTestContext context, TestServerContext server, Vec3 eyeAt, Vec3 target) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f", eyeAt.x, eyeAt.y, eyeAt.z));
		context.waitTicks(10);
		context.runOnClient(mc -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, target));
		context.waitTicks(5);
	}
}

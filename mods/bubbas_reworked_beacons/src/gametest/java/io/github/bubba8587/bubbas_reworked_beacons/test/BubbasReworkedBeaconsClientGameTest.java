// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.test;

import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import io.github.bubba8587.bubbas_reworked_beacons.client.BeaconItemScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Client game test. */
@SuppressWarnings("UnstableApiUsage")
public class BubbasReworkedBeaconsClientGameTest implements FabricClientGameTest {
	private static final int Y = 150;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("beacons")) {
			return;
		}
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(mc -> mc.player.blockPosition());
			BlockPos sky = new BlockPos(origin.getX(), Y, origin.getZ());
			BlockPos pair = sky.east(40);
			BlockPos pairFloor = pair.below(2);
			build(sp, sky, pair);

			// Sky beacon: stand 8 blocks south, look up the beam
			stand(sp, sky.south(8));
			context.waitFor(mc -> beamUp(mc, sky), 600);
			context.getInput().lookAt(180.0F, -35.0F);
			context.waitTicks(10);
			context.takeScreenshot("bubbas_reworked_beacons-sky-beam");

			openScreen(context, sp, sky, "bubbas_reworked_beacons-screen-sky-amethyst");

			stand(sp, pair.south(7).below(3));
			context.waitFor(mc -> beamUp(mc, pairFloor), 600);
			context.getInput().lookAt(180.0F, -25.0F);
			context.waitTicks(10);
			context.takeScreenshot("bubbas_reworked_beacons-paired-beam");

			openScreen(context, sp, pairFloor, "bubbas_reworked_beacons-screen-paired-capped");

			// Paired corners: the box's bottom and top corners, seen from outside one corner
			sp.getServer().runOnServer(server -> setItem(server, pairFloor, new ItemStack(Items.AMETHYST_SHARD)));
			stand(sp, pairFloor.offset(-90, 4, -90));
			sp.getServer().waitFor(server -> server.overworld().getBlockEntity(pairFloor) instanceof BeaconBlockEntity be
					&& ((ReworkedBeacon) be).bubba$edgeFlash() > 0, 200);
			context.getInput().lookAt(-45.0F, 0.0F);
			context.waitTicks(12);
			context.takeScreenshot("bubbas_reworked_beacons-edge-paired");

			// Corner particles: a ground sky beacon changes item; shots of one corner and the whole square
			BlockPos column = sky.west(40);
			stand(sp, column.atY(Y - 60));
			context.waitTicks(40);
			BlockPos ground = sp.getServer().computeOnServer(server ->
					server.overworld().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, column));
			cmd(sp, "fill %d %d %d %d %d %d minecraft:iron_block", ground.getX() - 1, ground.getY(), ground.getZ() - 1, ground.getX() + 1, ground.getY(), ground.getZ() + 1);
			cmd(sp, "setblock %d %d %d minecraft:beacon", ground.getX(), ground.getY() + 1, ground.getZ());
			BlockPos beacon = ground.above();
			sp.getServer().runOnServer(server -> setItem(server, beacon, new ItemStack(Items.AMETHYST_SHARD)));
			stand(sp, beacon.offset(-27, 3, -27));
			context.waitTicks(200);
			sp.getServer().runOnServer(server -> setItem(server, beacon, new ItemStack(Items.QUARTZ)));
			sp.getServer().waitFor(server -> server.overworld().getBlockEntity(beacon) instanceof BeaconBlockEntity be
					&& ((ReworkedBeacon) be).bubba$edgeFlash() > 0, 200);
			context.getInput().lookAt(-45.0F, 20.0F);
			context.waitTicks(12);
			context.takeScreenshot("bubbas_reworked_beacons-edge-corner");
			sp.getServer().runOnServer(server -> setItem(server, beacon, new ItemStack(Items.AMETHYST_SHARD)));
			stand(sp, beacon.offset(0, 40, -45));
			sp.getServer().waitFor(server -> server.overworld().getBlockEntity(beacon) instanceof BeaconBlockEntity be
					&& ((ReworkedBeacon) be).bubba$edgeFlash() > 0, 200);
			context.getInput().lookAt(0.0F, 40.0F);
			context.waitTicks(12);
			context.takeScreenshot("bubbas_reworked_beacons-edge-square");
		}
	}

	private static void build(TestSingleplayerContext sp, BlockPos sky, BlockPos pair) {
		cmd(sp, "fill %d %d %d %d %d %d minecraft:iron_block", sky.getX() - 1, Y - 1, sky.getZ() - 1, sky.getX() + 1, Y - 1, sky.getZ() + 1);
		cmd(sp, "setblock %d %d %d minecraft:beacon", sky.getX(), Y, sky.getZ());
		cmd(sp, "fill %d %d %d %d %d %d minecraft:glass", sky.getX() - 1, Y - 1, sky.getZ() + 6, sky.getX() + 1, Y - 1, sky.getZ() + 9);

		// A closed stone room around the pair: floor 146, ceiling 158
		int x = pair.getX(), z = pair.getZ();
		cmd(sp, "fill %d %d %d %d %d %d minecraft:stone hollow", x - 9, Y - 4, z - 9, x + 9, Y + 8, z + 9);
		cmd(sp, "fill %d %d %d %d %d %d minecraft:iron_block", x - 1, Y - 3, z - 1, x + 1, Y - 3, z + 1);
		cmd(sp, "fill %d %d %d %d %d %d minecraft:gold_block", x - 1, Y + 7, z - 1, x + 1, Y + 7, z + 1);
		cmd(sp, "setblock %d %d %d minecraft:beacon", x, Y - 2, z);
		cmd(sp, "setblock %d %d %d minecraft:beacon[upside_down=true]", x, Y + 6, z);
		sp.getServer().runOnServer(server -> {
			setItem(server, sky, new ItemStack(Items.AMETHYST_SHARD));
			setItem(server, new BlockPos(x, Y - 2, z), new ItemStack(Items.ECHO_SHARD));
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.CREATIVE);
			player.getInventory().clearContent();
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
		});
	}

	private static void setItem(MinecraftServer server, BlockPos pos, ItemStack stack) {
		if (server.overworld().getBlockEntity(pos) instanceof BeaconBlockEntity be) {
			((ReworkedBeacon) be).bubba$setItem(stack);
		} else {
			throw new AssertionError("no beacon at " + pos);
		}
	}

	private static void stand(TestSingleplayerContext sp, BlockPos feet) {
		sp.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		});
		sp.getConnection().waitForChunksRender();
	}

	private static boolean beamUp(Minecraft mc, BlockPos beacon) {
		return mc.level.getBlockEntity(beacon) instanceof BeaconBlockEntity be && !be.getBeamSections().isEmpty();
	}

	private static void openScreen(ClientGameTestContext context, TestSingleplayerContext sp, BlockPos beacon, String shot) {
		sp.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			if (server.overworld().getBlockEntity(beacon) instanceof BeaconBlockEntity be) {
				player.openMenu(be);
			}
		});
		context.waitForScreen(BeaconItemScreen.class);
		context.waitTicks(5);
		context.takeScreenshot(shot);
		context.setScreen(() -> null);
	}

	private static void cmd(TestSingleplayerContext sp, String format, Object... args) {
		sp.getServer().runCommand(format.formatted(args));
	}
}

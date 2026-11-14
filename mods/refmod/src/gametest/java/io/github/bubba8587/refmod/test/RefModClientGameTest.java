// dte:A2,A4
package io.github.bubba8587.refmod.test;

import io.github.bubba8587.refmod.block.SignalLampBlock;
import io.github.bubba8587.refmod.client.RefModClient;
import io.github.bubba8587.refmod.registry.ModAttachments;
import io.github.bubba8587.refmod.registry.ModBlocks;
import io.github.bubba8587.refmod.registry.ModItems;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client game test (refmod requirement 16): place a signal lamp, hold the tally stick, toggle the lamp
 * (S2C payload to the action bar, synced attachment), press the stats key, and take a screenshot. Then place a
 * lantern charm pseudo-block (requirement 17a) next to it and take a second screenshot.
 */
@SuppressWarnings("UnstableApiUsage")
public class RefModClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			singleplayer.getConnection().waitForChunksRender();

			BlockPos lamp = context.computeOnClient(mc -> mc.player.blockPosition().north(2).above());
			singleplayer.getServer().runCommand("setblock %d %d %d refmod:signal_lamp".formatted(lamp.getX(), lamp.getY(), lamp.getZ()));
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with refmod:tally_stick");
			context.waitFor(mc -> mc.level.getBlockState(lamp).is(ModBlocks.SIGNAL_LAMP)
					&& mc.player.getMainHandItem().is(ModItems.TALLY_STICK));

			// Toggle the lamp with the use key; the server sends LampTogglePayload and syncs the attachment.
			context.getInput().lookAt(lamp);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitFor(mc -> mc.level.getBlockState(lamp).getValue(SignalLampBlock.LIT)
					&& ModAttachments.getLampToggles(mc.player) == 1);

			// Key mapping prints the synced count to chat.
			context.getInput().pressKey(RefModClient.SHOW_STATS_KEY);
			context.waitTicks(5);

			context.takeScreenshot("refmod-signal-lamp");

			// No-new-blocks pseudo-block (requirement 17a): use the lantern charm on a stone block next to the lamp,
			// which spawns an item_display (the look) and an interaction (the hitbox) on top of it.
			BlockPos stone = lamp.below().east();
			singleplayer.getServer().runCommand("setblock %d %d %d minecraft:stone".formatted(stone.getX(), stone.getY(), stone.getZ()));
			singleplayer.getServer().runCommand("item replace entity @a hotbar.0 with refmod:lantern_charm");
			context.waitFor(mc -> mc.level.getBlockState(stone).is(Blocks.STONE) && mc.player.getMainHandItem().is(ModItems.LANTERN_CHARM));
			context.getInput().lookAt(stone);
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitFor(mc -> !mc.level.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(stone.above()).inflate(0.5)).isEmpty()); // display entities have a zero-size bounding box
			context.getInput().lookAt(lamp.east());
			context.waitTicks(5);
			context.takeScreenshot("refmod-lantern-charm");
		}
	}
}

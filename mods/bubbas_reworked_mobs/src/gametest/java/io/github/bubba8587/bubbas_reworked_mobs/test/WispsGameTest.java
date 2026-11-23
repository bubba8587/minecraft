// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand;
import io.github.bubba8587.bubbas_reworked_mobs.grave.Wisps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Will-o-wisps: a yard counts as found once a player walks into it. Milestone. */
public class WispsGameTest {
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 100)
	public void walkingIntoAYardFindsIt(GameTestHelper helper) {
		CryptYardPiece piece = GraveyardCommand.build(helper.getLevel(), helper.absolutePos(new BlockPos(16, 7, 2)), false, 51L, 15, 13);
		long id = ChunkPos.containing(piece.gate()).pack();
		helper.assertTrue(Wisps.yards(helper.getLevel()).containsKey(id), "the built yard is registered");
		ServerPlayer player = TestPlayers.survival(helper);
		BlockPos outside = helper.absolutePos(new BlockPos(16, 8, 40));
		player.snapTo(outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5, 180.0F, 0.0F);
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(!Wisps.hasFound(player, id), "not found from outside");
			BlockPos in = piece.getBoundingBox().getCenter();
			player.snapTo(in.getX() + 0.5, helper.absolutePos(new BlockPos(0, 8, 0)).getY(), in.getZ() + 0.5, 0.0F, 0.0F);
		});
		helper.runAfterDelay(70, () -> {
			helper.assertTrue(Wisps.hasFound(player, id), "walking in finds it");
			helper.getLevel().getServer().getPlayerList().remove(player);
			helper.succeed();
		});
	}
}

// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModBlocks;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Totem statues (statue/TotemStatueBlock): placing one from a totem, and getting the totem back. */
public class TotemStatueGameTest {
	/**
	 * Core. Sneak-using a Totem of Warding on the floor sets down a warding statue facing the player and uses the
	 * totem up; using it without sneaking places nothing; breaking the statue drops the Totem of Warding.
	 */
	public void sneakPlacesStatueAndBreakingGivesTotemBack(GameTestHelper helper) {
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		BlockPos floor = new BlockPos(2, 0, 2);
		helper.setBlock(floor, Blocks.STONE);
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 5.5)); // south of the floor block, looking north at it
		player.snapTo(at.x, at.y, at.z, 180.0F, 45.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TOTEM_OF_WARDING));
		BlockHitResult hit = new BlockHitResult(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)), Direction.UP, helper.absolutePos(floor), false);

		player.setShiftKeyDown(false);
		ItemStack held = player.getMainHandItem();
		held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		helper.assertBlockPresent(Blocks.AIR, floor.above());

		player.setShiftKeyDown(true);
		held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		BlockState placed = helper.getBlockState(floor.above());
		helper.assertTrue(placed.is(ModBlocks.TOTEM_STATUE), "no statue placed: " + placed);
		helper.assertTrue(TotemStatueBlock.type(placed) == TotemType.WARDING, "statue type " + TotemStatueBlock.type(placed));
		helper.assertTrue(placed.getValue(TotemStatueBlock.FACING) == Direction.SOUTH, "statue faces " + placed.getValue(TotemStatueBlock.FACING));
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the totem wasn't used up");

		List<ItemStack> drops = Block.getDrops(placed, helper.getLevel(), helper.absolutePos(floor.above()), null);
		helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.TOTEM_OF_WARDING) && drops.getFirst().getCount() == 1,
				"statue drops " + drops);
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}
}

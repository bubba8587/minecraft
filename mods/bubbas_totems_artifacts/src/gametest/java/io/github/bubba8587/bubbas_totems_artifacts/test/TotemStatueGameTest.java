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

import net.fabricmc.fabric.api.gametest.v1.GameTest;
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

		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	/**
	 * Core. Every totem has a statue now: sneak-using a Totem of the End on the floor sets down an End statue (and
	 * doesn't teleport the player), and breaking it gives the Totem of the End back.
	 */
	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void endStatuePlacesWithoutTeleportingAndDropsItsTotem(GameTestHelper helper) {
		ServerPlayer player = TestSupport.survivalPlayer(helper);
		BlockPos floor = new BlockPos(2, 0, 2);
		helper.setBlock(floor, Blocks.STONE);
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 5.5));
		player.snapTo(at.x, at.y, at.z, 180.0F, 45.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TOTEM_OF_THE_END));
		BlockHitResult hit = new BlockHitResult(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)), Direction.UP, helper.absolutePos(floor), false);
		player.setShiftKeyDown(true);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		BlockState placed = helper.getBlockState(floor.above());
		helper.assertTrue(placed.is(ModBlocks.TOTEM_STATUE) && TotemStatueBlock.type(placed) == TotemType.END, "no End statue placed: " + placed);
		helper.assertTrue(player.position().distanceTo(at) < 1.0E-6, "sneak-placing the statue teleported the player");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	/**
	 * Every totem's statue drops its own totem, and a statue saved before the {@code totem} property existed (only its
	 * facing) loads as a warding statue.
	 */
	@net.fabricmc.fabric.api.gametest.v1.GameTest(maxTicks = 5)
	public void everyStatueDropsItsTotemAndOldOnesLoadAsWarding(GameTestHelper helper) {
		for (TotemType type : TotemType.values()) {
			BlockState state = TotemStatueBlock.stateFor(type);
			List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), null);
			helper.assertTrue(drops.size() == 1 && drops.getFirst().is(type.item()), type + " statue drops " + drops);
		}
		net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
		old.putString("id", "bubbas_totems_artifacts:totem_statue");
		net.minecraft.nbt.CompoundTag props = new net.minecraft.nbt.CompoundTag();
		props.putString("facing", "east");
		old.put("properties", props);
		BlockState loaded = net.minecraft.nbt.NbtUtils.readBlockState(helper.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), old);
		helper.assertTrue(loaded.is(ModBlocks.TOTEM_STATUE) && TotemStatueBlock.type(loaded) == TotemType.WARDING
				&& loaded.getValue(TotemStatueBlock.FACING) == Direction.EAST, "an old statue loaded as " + loaded);
		helper.succeed();
	}
}

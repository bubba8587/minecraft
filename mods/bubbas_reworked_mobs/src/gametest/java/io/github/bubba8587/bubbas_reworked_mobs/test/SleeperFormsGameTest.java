// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBells;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Sleeper item forms (specs/undead.md "Sleeper items"): pick-block and Silk Touch keep pose and statue; placing restores them. Milestone. */
public class SleeperFormsGameTest {
	private static final BlockPos POS = new BlockPos(2, 1, 2);

	private static void place(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
		helper.setBlock(POS, Blocks.AIR);
		helper.setBlock(POS.below(), Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
		BlockPos abs = helper.absolutePos(POS.below());
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs).relative(Direction.UP, 0.5), Direction.UP, abs, false));
	}

	private static void assertForm(GameTestHelper helper, UndeadVariant variant, SleeperPose pose, boolean statue, String how) {
		BlockState state = helper.getBlockState(POS);
		helper.assertTrue(state.is(ModBlocks.sleeper(variant)) && state.getValue(SleeperBlock.POSE) == pose && SleeperBlock.isResting(state) == statue,
				how + ": expected " + variant + " " + pose + (statue ? " statue" : "") + ", got " + state);
	}

	private static ItemStack silkDrop(GameTestHelper helper, ServerPlayer player) {
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(POS);
		ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
		pick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		List<ItemStack> drops = Block.getDrops(level.getBlockState(abs), level, abs, level.getBlockEntity(abs), player, pick);
		return drops.stream().filter(d -> d.getItem() instanceof SleeperItem).findFirst().orElse(ItemStack.EMPTY);
	}

	/** Every kind, pose and statue: pick block and Silk Touch give an item of that form, and placing it gives the form back. */
	@GameTest
	public void everyFormSurvivesPickUpAndPlace(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.survival(helper);
		for (UndeadVariant variant : UndeadVariant.values()) {
			for (SleeperPose pose : variant.poses()) {
				for (boolean statue : new boolean[] {false, true}) {
					ItemStack form = SleeperItem.form(ModItems.sleeper(variant), pose, statue);
					helper.assertTrue(SleeperItem.isStatue(form) == statue && SleeperItem.pose(ModBlocks.sleeper(variant), form) == pose, "form stack " + form);
					place(helper, player, form);
					assertForm(helper, variant, pose, statue, "placed from the form stack");
					BlockPos abs = helper.absolutePos(POS);
					ItemStack picked = helper.getLevel().getBlockState(abs).getCloneItemStack(helper.getLevel(), abs, true);
					place(helper, player, picked);
					assertForm(helper, variant, pose, statue, "placed from the pick-block stack");
					ItemStack mined = silkDrop(helper, player);
					helper.assertFalse(mined.isEmpty(), "no Silk Touch drop for " + variant);
					place(helper, player, mined);
					assertForm(helper, variant, pose, statue, "placed from the Silk Touch drop");
					String name = mined.getHoverName().getString();
					helper.assertTrue(name.contains(statue ? "Statue" : "Sleeping"), "name of " + variant + " " + pose + ": " + name);
				}
			}
		}
		helper.succeed();
	}

	/** A statue picked up and placed again stays asleep through an explosion and a bell; a plain old-id item still places a wakeable sleeper. */
	@GameTest(maxTicks = 40)
	public void statueStaysInertAfterReplaceAndOldItemsPlace(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.survival(helper);
		helper.setBlock(POS, ModBlocks.sleeper(UndeadVariant.SKELETON).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.LYING)
				.setValue(SleeperBlock.RESTING, true));
		BlockPos abs = helper.absolutePos(POS);
		ItemStack picked = helper.getLevel().getBlockState(abs).getCloneItemStack(helper.getLevel(), abs, true);
		place(helper, player, picked);
		assertForm(helper, UndeadVariant.SKELETON, SleeperPose.LYING, true, "statue re-placed");
		SleeperBlockEntity statue = helper.getBlockEntity(POS, SleeperBlockEntity.class);
		statue.setSensitivity(10.0F);
		helper.getLevel().gameEvent(GameEvent.EXPLODE, helper.absoluteVec(Vec3.atCenterOf(POS.east(2))), GameEvent.Context.of((net.minecraft.world.entity.Entity) null));
		SleeperBells.rang(helper.getLevel(), helper.absolutePos(POS.east()), null);
		helper.assertFalse(statue.isWakePending(), "a re-placed statue woke");
		helper.assertTrue(statue.disturb(helper.getLevel().getGameTime(), 1000.0F) == 0.0F, "a re-placed statue hears noise");
		// Old item ids (no form component) place the kind's default form, not a statue.
		for (UndeadVariant variant : UndeadVariant.values()) {
			place(helper, player, new ItemStack(ModItems.sleeper(variant)));
			assertForm(helper, variant, variant.poses().getFirst(), false, "placed from a plain " + variant + " item");
		}
		helper.succeed();
	}
}

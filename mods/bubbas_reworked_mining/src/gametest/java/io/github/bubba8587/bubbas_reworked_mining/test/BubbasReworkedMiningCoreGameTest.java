// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import io.github.bubba8587.bubbas_reworked_mining.enchant.ModEnchantments;
import io.github.bubba8587.bubbas_reworked_mining.natural.NaturalStone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core smoke tests (test-lite, specs/quality.md "Test suites"): one per main mechanic of Reworked Mining. Everything
 * finer is a milestone objective in specs/reworked-mining.md.
 */
public class BubbasReworkedMiningCoreGameTest {
	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		Registry<Enchantment> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		return registry.getOrThrow(key);
	}

	private static ItemStack tool(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
		stack.enchant(enchantment(helper, key), level);
		return stack;
	}

	private static float speed(ServerPlayer player, ItemStack tool, BlockState state) {
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		return player.getDestroySpeed(state);
	}

	private static void near(GameTestHelper helper, float actual, float expected, String what) {
		helper.assertTrue(Math.abs(actual - expected) < 0.01F, what + ": expected " + expected + ", got " + actual);
	}

	/** Speed: the tier factors and the Efficiency multiplier reach the player's real dig speed. */
	@GameTest
	public void curveSlowsDeepslateAndEfficiencyMultiplies(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
		BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();

		float ironStone = speed(player, new ItemStack(Items.IRON_PICKAXE), stone);
		float ironDeepslate = speed(player, new ItemStack(Items.IRON_PICKAXE), deepslate);
		near(helper, ironDeepslate / ironStone, MiningTuning.IRON.deepslate() / MiningTuning.IRON.stone(), "iron deepslate/stone");
		float woodStone = speed(player, new ItemStack(Items.WOODEN_PICKAXE), stone);
		float diamondStone = speed(player, new ItemStack(Items.DIAMOND_PICKAXE), stone);
		near(helper, (woodStone / 2.0F) / (diamondStone / 8.0F), MiningTuning.WOOD.stone() / MiningTuning.DIAMOND.stone(), "wood vs diamond on stone");
		float woodAxe = speed(player, new ItemStack(Items.WOODEN_AXE), planks);
		float diamondAxe = speed(player, new ItemStack(Items.DIAMOND_AXE), planks);
		near(helper, (woodAxe / 2.0F) / (diamondAxe / 8.0F), MiningTuning.WOOD.other() / MiningTuning.DIAMOND.other(), "wood vs diamond axe on planks");

		float efficient = speed(player, tool(helper, new ItemStack(Items.IRON_PICKAXE), Enchantments.EFFICIENCY, 5), deepslate);
		near(helper, efficient / ironDeepslate, MiningTuning.efficiency(5), "Efficiency V multiplier");
		helper.succeed();
	}

	/** Wear: an iron pickaxe loses the tuned durability per deepslate block, and wood can't harvest deepslate. */
	@GameTest
	public void deepslateWearsToolsAndWoodCantHarvestIt(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
		iron.mineBlock(helper.getLevel(), Blocks.DEEPSLATE.defaultBlockState(), pos, player);
		helper.assertValueEqual(iron.getDamageValue(), MiningTuning.IRON.deepslateWear(), "iron wear on deepslate");
		ItemStack ironOnStone = new ItemStack(Items.IRON_PICKAXE);
		ironOnStone.mineBlock(helper.getLevel(), Blocks.STONE.defaultBlockState(), pos, player);
		helper.assertValueEqual(ironOnStone.getDamageValue(), MiningTuning.IRON.stoneWear(), "iron wear on stone");
		helper.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(Blocks.DEEPSLATE.defaultBlockState()), "wood harvests deepslate");
		helper.assertTrue(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(Blocks.DEEPSLATE.defaultBlockState()), "stone can't harvest deepslate");
		helper.succeed();
	}

	/** Finds: natural stone sometimes drops a find with Fortune III; placed stone and Silk Touch never do. */
	@GameTest
	public void findsOnlyFromNaturalStone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState natural = Blocks.STONE.defaultBlockState();
		helper.assertTrue(!NaturalStone.isPlaced(natural), "default stone counts as placed");
		BlockState placed = NaturalStone.placed(natural);
		helper.assertTrue(NaturalStone.isPlaced(placed), "placed mark missing");
		ItemStack fortune = tool(helper, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.FORTUNE, 3);
		ItemStack silk = tool(helper, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.SILK_TOUCH, 1);

		int rolls = 3000;
		int naturalFinds = 0;
		int placedFinds = 0;
		int silkFinds = 0;
		for (int i = 0; i < rolls; i++) {
			naturalFinds += finds(Block.getDrops(natural, level, pos, null, player, fortune));
			placedFinds += finds(Block.getDrops(placed, level, pos, null, player, fortune));
			silkFinds += finds(Block.getDrops(natural, level, pos, null, player, silk));
		}
		float expected = rolls * MiningTuning.STONE_FIND_CHANCE[3];
		helper.assertTrue(naturalFinds > expected * 0.5F && naturalFinds < expected * 1.5F,
				"natural finds " + naturalFinds + " out of " + rolls + ", expected about " + expected);
		helper.assertValueEqual(placedFinds, 0, "finds from placed stone");
		helper.assertValueEqual(silkFinds, 0, "finds with Silk Touch");
		helper.succeed();
	}

	private static int finds(List<ItemStack> drops) {
		int n = 0;
		for (ItemStack stack : drops) {
			if (!stack.is(Items.COBBLESTONE) && !stack.is(Items.STONE)) {
				n++;
			}
		}
		return n;
	}

	/** Unbreaking is stronger; True Unbreaking stops all wear, is found only, and can't join Mending. */
	@GameTest
	public void unbreakingStrongerAndTrueUnbreakingIsTotal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int points = 14000;
		int kept = EnchantmentHelper.processDurabilityChange(level, tool(helper, new ItemStack(Items.IRON_PICKAXE), Enchantments.UNBREAKING, 3), points);
		float expected = points / 7.0F; // 1/7 kept at III (vanilla 1/4)
		helper.assertTrue(kept > expected * 0.85F && kept < expected * 1.15F, "Unbreaking III kept " + kept + " of " + points + ", expected about " + expected);

		Holder<Enchantment> trueUnbreaking = enchantment(helper, ModEnchantments.TRUE_UNBREAKING);
		ItemStack elytra = new ItemStack(Items.ELYTRA);
		elytra.enchant(trueUnbreaking, 1);
		helper.assertValueEqual(EnchantmentHelper.processDurabilityChange(level, elytra, 500), 0, "True Unbreaking damage");
		helper.assertTrue(!Enchantment.areCompatible(trueUnbreaking, enchantment(helper, Enchantments.MENDING)), "True Unbreaking combines with Mending");
		helper.assertTrue(!trueUnbreaking.is(EnchantmentTags.IN_ENCHANTING_TABLE), "True Unbreaking in the enchanting table");
		helper.assertTrue(!trueUnbreaking.is(EnchantmentTags.TRADEABLE), "True Unbreaking tradeable");
		helper.assertTrue(!trueUnbreaking.is(EnchantmentTags.ON_RANDOM_LOOT), "True Unbreaking on random loot");
		helper.succeed();
	}
}

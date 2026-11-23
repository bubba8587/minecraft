// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.test;

import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import io.github.bubba8587.bubbas_reworked_mining.enchant.ModEnchantments;
import io.github.bubba8587.bubbas_reworked_mining.natural.NaturalStone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Milestone tests for Reworked Mining (run with -PmilestoneTests). Objectives in specs/reworked-mining.md. */
public class MiningMilestoneGameTest {
	/** A survival player placing stone and deepslate from items marks them placed. */
	@GameTest
	public void playerPlacedStoneIsMarked(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos floor = new BlockPos(1, 1, 1);
		helper.setBlock(floor, Blocks.STONE);
		Vec3 stand = helper.absoluteVec(Vec3.atBottomCenterOf(floor.above().north(2)));
		player.snapTo(stand.x, stand.y, stand.z, 0.0F, 30.0F);
		for (ItemStack stack : new ItemStack[] {new ItemStack(Items.STONE), new ItemStack(Items.DEEPSLATE)}) {
			helper.setBlock(floor.above(), Blocks.AIR);
			BlockPos abs = helper.absolutePos(floor);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0), Direction.UP, abs, false));
			BlockState placed = level.getBlockState(abs.above());
			helper.assertTrue(NaturalStone.isPlaced(placed), "not marked placed: " + placed);
		}
		helper.succeed();
	}

	/** Stone made by lava flowing onto water counts as placed (no stone-generator farming). */
	@GameTest(maxTicks = 300)
	public void lavaMadeStoneIsMarked(GameTestHelper helper) {
		BlockPos water = new BlockPos(1, 1, 1);
		helper.setBlock(water.below(), Blocks.STONE);
		helper.setBlock(water, Blocks.WATER);
		helper.setBlock(water.above(), Blocks.AIR);
		helper.setBlock(water.above(2), Blocks.LAVA);
		helper.succeedWhen(() -> {
			BlockState state = helper.getBlockState(water);
			helper.assertTrue(state.is(Blocks.STONE), "no stone yet: " + state);
			helper.assertTrue(NaturalStone.isPlaced(state), "lava stone counts as natural");
		});
	}

	/** True Unbreaking books turn up in the tuned chests at about the tuned rate, and work on armor too. */
	@GameTest
	public void trueUnbreakingInChests(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Holder<Enchantment> trueUnbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.TRUE_UNBREAKING);
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/bastion_treasure"));
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
		int rolls = 2000;
		int books = 0;
		for (int i = 0; i < rolls; i++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST);
			for (ItemStack stack : table.getRandomItems(params)) {
				if (stack.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(trueUnbreaking) > 0) {
					books++;
				}
			}
		}
		float expected = rolls * MiningTuning.TRUE_UNBREAKING_LOOT.get(key);
		helper.assertTrue(books > expected * 0.5F && books < expected * 1.5F, "books " + books + " of " + rolls + ", expected about " + expected);

		ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
		chestplate.enchant(trueUnbreaking, 1);
		helper.assertValueEqual(EnchantmentHelper.processDurabilityChange(level, chestplate, 200), 0, "armor damage with True Unbreaking");
		helper.succeed();
	}
}

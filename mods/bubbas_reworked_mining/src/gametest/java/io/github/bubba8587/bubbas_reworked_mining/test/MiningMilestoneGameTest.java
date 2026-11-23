// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.test;

import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import io.github.bubba8587.bubbas_reworked_mining.enchant.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Milestone tests for Reworked Mining. */
public class MiningMilestoneGameTest {
	/** True Unbreaking books turn up in the tuned chests at about the tuned rate. */
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

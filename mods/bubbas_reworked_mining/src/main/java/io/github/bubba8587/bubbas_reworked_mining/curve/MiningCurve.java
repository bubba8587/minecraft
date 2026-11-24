// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.curve;

import io.github.bubba8587.bubbas_reworked_mining.BubbasReworkedMining;
import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import io.github.bubba8587.bubbas_reworked_mining.MiningTuning.Tier;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The mining curve: how fast and how hard each tool tier works on each kind of block. */
public final class MiningCurve {
	public static final TagKey<Block> HARSH_STONE = TagKey.create(Registries.BLOCK, BubbasReworkedMining.id("harsh_stone"));
	public static final TagKey<Block> HARSH_DEEPSLATE = TagKey.create(Registries.BLOCK, BubbasReworkedMining.id("harsh_deepslate"));
	/** The Nether's natural blocks, where quartz tools are faster. */
	public static final TagKey<Block> NETHER_BLOCKS = TagKey.create(Registries.BLOCK, BubbasReworkedMining.id("nether_blocks"));

	/** Id vanilla gives Efficiency's mining_efficiency modifier on the main hand. */
	public static final Identifier VANILLA_EFFICIENCY_MODIFIER = Identifier.withDefaultNamespace("enchantment.efficiency/mainhand");

	private enum TierTag {
		WOOD("wood", MiningTuning.WOOD),
		STONE("stone", MiningTuning.STONE),
		COPPER("copper", MiningTuning.COPPER),
		IRON("iron", MiningTuning.IRON),
		GOLD("gold", MiningTuning.GOLD),
		DIAMOND("diamond", MiningTuning.DIAMOND),
		NETHERITE("netherite", MiningTuning.NETHERITE),
		QUARTZ("quartz", MiningTuning.QUARTZ);

		final TagKey<Item> tag;
		final Tier tier;

		TierTag(String name, Tier tier) {
			this.tag = TagKey.create(Registries.ITEM, BubbasReworkedMining.id("tier/" + name));
			this.tier = tier;
		}
	}

	private MiningCurve() {
	}

	/** The tier of a tool, or null for anything outside the curve. */
	public static @Nullable Tier tier(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (TierTag t : TierTag.values()) {
			if (stack.is(t.tag)) {
				return t.tier;
			}
		}
		return null;
	}

	/** Whether the player is mining bare-handed: an empty hand or an item without a Tool component. */
	public static boolean isFist(ItemStack stack) {
		return stack.isEmpty() || !stack.has(DataComponents.TOOL);
	}

	/** Efficiency level on a stack. */
	public static int efficiencyLevel(ItemStack stack) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			if (entry.getKey().is(Enchantments.EFFICIENCY)) {
				return entry.getIntValue();
			}
		}
		return 0;
	}

	/** The tool's speed on a block after the curve and Efficiency, given its vanilla speed. */
	public static float speed(ItemStack stack, BlockState state, float vanillaSpeed) {
		if (isFist(stack)) {
			return vanillaSpeed * factor(MiningTuning.FIST, stack, state);
		}
		if (vanillaSpeed <= 1.0F) {
			return vanillaSpeed;
		}
		Tier tier = tier(stack);
		if (tier == null) {
			return vanillaSpeed;
		}
		return vanillaSpeed * factor(tier, stack, state) * MiningTuning.efficiency(efficiencyLevel(stack));
	}

	private static float factor(Tier tier, ItemStack stack, BlockState state) {
		if (state.is(HARSH_DEEPSLATE)) {
			return tier.deepslate();
		}
		if (state.is(HARSH_STONE)) {
			return tier.stone();
		}
		if (state.is(NETHER_BLOCKS)) {
			return tier.nether();
		}
		return tier.other();
	}

	/** Durability a tool loses on this block, given vanilla's amount. */
	public static int wear(ItemStack stack, BlockState state, int vanillaWear) {
		Tier tier = tier(stack);
		if (tier == null || vanillaWear != 1) {
			return vanillaWear;
		}
		if (state.is(HARSH_DEEPSLATE)) {
			return tier.deepslateWear();
		}
		if (state.is(HARSH_STONE)) {
			return tier.stoneWear();
		}
		return vanillaWear;
	}

	/** Whether vanilla's flat Efficiency bonus should be taken back out. */
	public static boolean replacesEfficiency(ItemStack stack) {
		return tier(stack) != null;
	}
}

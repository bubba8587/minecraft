// dte:C7
package io.github.bubba8587.bubbas_reworked_mining;

import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Every number the owner tunes by playing (specs/reworked-mining.md). Nothing else in the mod holds a balance number.
 *
 * <p>Reading the speed table: a factor multiplies the tool's own vanilla speed, so 0.5 means a block takes twice as
 * long as in vanilla, and 1.0 is vanilla. Vanilla deepslate already takes twice as long as stone (hardness 3 vs 1.5);
 * the factors are on top of that. Wear is the total durability a tool loses per block (vanilla is 1); Unbreaking
 * still gets a chance to cancel each point.
 */
public final class MiningTuning {
	private MiningTuning() {
	}

	/**
	 * One tool tier's curve.
	 *
	 * @param stone         speed factor on plain stone, cobblestone and stone ores (tag {@code harsh_stone})
	 * @param deepslate     speed factor on deepslate, cobbled deepslate and deepslate ores (tag {@code harsh_deepslate})
	 * @param other         speed factor on everything else the tool is meant for (wood for axes, dirt for shovels...)
	 * @param stoneWear     durability lost per {@code harsh_stone} block
	 * @param deepslateWear durability lost per {@code harsh_deepslate} block
	 */
	public record Tier(float stone, float deepslate, float other, int stoneWear, int deepslateWear) {
	}

	// Owner: wood "barely does anything" and can't mine deepslate at all (no drops: data tag incorrect_for_wooden_tool).
	public static final Tier WOOD = new Tier(0.33F, 0.33F, 0.70F, 2, 3);
	// Owner: stone a bit slower on stone, "incredibly slow" on deepslate, heavy wear there.
	public static final Tier STONE = new Tier(0.67F, 0.125F, 0.75F, 1, 3);
	// Owner: copper and iron about vanilla on stone, slow on deepslate with "significant" wear.
	public static final Tier COPPER = new Tier(1.0F, 0.40F, 1.0F, 1, 4);
	public static final Tier IRON = new Tier(1.0F, 0.40F, 1.0F, 1, 3);
	// Builder's choice: gold is wood-level for harvesting in vanilla, so it can't mine deepslate either; it keeps
	// some of its speed on stone.
	public static final Tier GOLD = new Tier(0.67F, 0.33F, 1.0F, 1, 3);
	// Owner: diamond about vanilla everywhere.
	public static final Tier DIAMOND = new Tier(1.0F, 1.0F, 1.0F, 1, 1);
	// Owner: netherite faster than vanilla.
	public static final Tier NETHERITE = new Tier(1.2F, 1.2F, 1.1F, 1, 1);

	/**
	 * Efficiency multiplies the tool's speed instead of vanilla's flat bonus (vanilla's +26 at level V would erase the
	 * whole curve: an Efficiency V stone pickaxe would out-mine a bare diamond one). Index = level. With these, an
	 * Efficiency V iron pickaxe mines deepslate a little faster than a bare diamond one, and Efficiency V diamond
	 * matches vanilla Efficiency V diamond (the "machine"). Levels past the table keep adding the last step.
	 */
	public static final float[] EFFICIENCY_MULTIPLIER = {1.0F, 1.5F, 2.1F, 2.8F, 3.5F, 4.25F};

	/**
	 * Stronger Unbreaking: after vanilla's own chance, each point of durability loss is cancelled again with this
	 * chance. Index = level - 1. Tools then keep a point with odds 1/3, 1/5, 1/7 at I, II, III (vanilla: 1/2, 1/3,
	 * 1/4); armor gets the same extra roll on top of vanilla's weaker armor odds.
	 */
	public static final float[] UNBREAKING_EXTRA_CANCEL = {0.333F, 0.40F, 0.43F};

	// Rare finds from natural stone and deepslate (never placed blocks, never with Silk Touch).

	/** Chance of a find per natural stone-type block, by Fortune level (index 0 = no Fortune). Each level doubles. */
	public static final float[] STONE_FIND_CHANCE = {0.005F, 0.010F, 0.020F, 0.040F};
	/** The same for natural deepslate: slightly better. */
	public static final float[] DEEPSLATE_FIND_CHANCE = {0.008F, 0.016F, 0.032F, 0.064F};

	/** What a find is: relative weights. Nuggets come as 1 to {@link #NUGGET_MAX}. */
	public static final int FIND_IRON_NUGGET = 60;
	public static final int FIND_GOLD_NUGGET = 30;
	public static final int FIND_LAPIS = 7;
	public static final int FIND_EMERALD = 3;
	/** Deepslate's weights (same order) plus a tiny diamond weight. */
	public static final int DEEPSLATE_FIND_IRON_NUGGET = 50;
	public static final int DEEPSLATE_FIND_GOLD_NUGGET = 35;
	public static final int DEEPSLATE_FIND_LAPIS = 10;
	public static final int DEEPSLATE_FIND_EMERALD = 4;
	public static final int DEEPSLATE_FIND_DIAMOND = 1;
	public static final int NUGGET_MAX = 3;

	/** True Unbreaking books: chance of one in each chest table (found only, rare; never enchanting table or villagers). */
	public static final Map<ResourceKey<LootTable>, Float> TRUE_UNBREAKING_LOOT = Map.of(
			chest("chests/ancient_city"), 0.04F,
			chest("chests/end_city_treasure"), 0.03F,
			chest("chests/bastion_treasure"), 0.05F,
			chest("chests/trial_chambers/reward_ominous_rare"), 0.05F);

	private static ResourceKey<LootTable> chest(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
	}

	/** The Efficiency multiplier for a level. */
	public static float efficiency(int level) {
		if (level <= 0) {
			return 1.0F;
		}
		int last = EFFICIENCY_MULTIPLIER.length - 1;
		if (level <= last) {
			return EFFICIENCY_MULTIPLIER[level];
		}
		float step = EFFICIENCY_MULTIPLIER[last] - EFFICIENCY_MULTIPLIER[last - 1];
		return EFFICIENCY_MULTIPLIER[last] + step * (level - last);
	}
}

// dte:C7
package io.github.bubba8587.bubbas_reworked_mining;

import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Every number the owner tunes by playing. */
public final class MiningTuning {
	private MiningTuning() {
	}

	/** One tool tier's curve. */
	public record Tier(float stone, float deepslate, float other, float nether, int stoneWear, int deepslateWear) {
		Tier(float stone, float deepslate, float other, int stoneWear, int deepslateWear) {
			this(stone, deepslate, other, other, stoneWear, deepslateWear);
		}
	}

	public static final Tier WOOD = new Tier(0.33F, 0.33F, 0.70F, 2, 3);
	public static final Tier STONE = new Tier(0.67F, 0.125F, 0.75F, 1, 3);
	public static final Tier COPPER = new Tier(1.0F, 0.40F, 1.0F, 1, 4);
	public static final Tier IRON = new Tier(1.0F, 0.40F, 1.0F, 1, 3);
	public static final Tier GOLD = new Tier(0.67F, 0.33F, 1.0F, 1, 3);
	public static final Tier DIAMOND = new Tier(1.0F, 1.0F, 1.0F, 1, 1);
	public static final Tier NETHERITE = new Tier(1.2F, 1.2F, 1.1F, 1, 1);
	public static final Tier QUARTZ = new Tier(1.0F, 0.40F, 1.0F, 1.6F, 1, 3);

	// The quartz set
	/** Tool durability. */
	public static final int QUARTZ_TOOL_DURABILITY = 220;
	public static final float QUARTZ_TOOL_SPEED = 6.0F;
	public static final float QUARTZ_TOOL_DAMAGE_BONUS = 2.0F;
	/** Enchantability. */
	public static final int QUARTZ_TOOL_ENCHANTABILITY = 14;
	/** Armor durability multiplier and protection per piece: boots, legs, chest, helmet. */
	public static final int QUARTZ_ARMOR_DURABILITY = 13;
	public static final int QUARTZ_ARMOR_BOOTS = 2;
	public static final int QUARTZ_ARMOR_LEGGINGS = 5;
	public static final int QUARTZ_ARMOR_CHESTPLATE = 6;
	public static final int QUARTZ_ARMOR_HELMET = 2;
	public static final int QUARTZ_ARMOR_ENCHANTABILITY = 9;

	/** Efficiency multiplies the tool's speed instead of vanilla's flat bonus. */
	public static final float[] EFFICIENCY_MULTIPLIER = {1.0F, 1.5F, 2.1F, 2.8F, 3.5F, 4.25F};

	/** Stronger Unbreaking: extra cancel chance per level. */
	public static final float[] UNBREAKING_EXTRA_CANCEL = {0.333F, 0.40F, 0.43F};

	// Rare finds

	/** Find chance per stone-type block, by Fortune level. */
	public static final float[] STONE_FIND_CHANCE = {0.005F, 0.010F, 0.020F, 0.040F};
	/** Find chance per deepslate block, by Fortune level. */
	public static final float[] DEEPSLATE_FIND_CHANCE = {0.008F, 0.016F, 0.032F, 0.064F};

	/** What a find is: relative weights. */
	public static final int FIND_IRON_NUGGET = 60;
	public static final int FIND_GOLD_NUGGET = 30;
	public static final int FIND_LAPIS = 7;
	public static final int FIND_EMERALD = 3;
	/** Deepslate's weights plus a tiny diamond weight. */
	public static final int DEEPSLATE_FIND_IRON_NUGGET = 50;
	public static final int DEEPSLATE_FIND_GOLD_NUGGET = 35;
	public static final int DEEPSLATE_FIND_LAPIS = 10;
	public static final int DEEPSLATE_FIND_EMERALD = 4;
	public static final int DEEPSLATE_FIND_DIAMOND = 1;
	public static final int NUGGET_MAX = 3;

	/** True Unbreaking books: chance of one in each chest table. */
	public static final Map<ResourceKey<LootTable>, Float> TRUE_UNBREAKING_LOOT = Map.of(
			chest("chests/ancient_city"), 0.04F,
			chest("chests/end_city_treasure"), 0.03F,
			chest("chests/bastion_treasure"), 0.05F,
			chest("chests/trial_chambers/reward_ominous_rare"), 0.05F,
			ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("bubbas_reworked_mobs", "chests/crypt_noble")), 0.20F);

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

// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Relic maps' numbers (specs/totems-artifacts.md, "Relic maps"). */
public final class RelicTuning {
	/** Named /4 vaults lie this far from the world's middle (blocks). */
	public static final int SMALL_MIN_DISTANCE = 400;
	public static final int SMALL_MAX_DISTANCE = 2400;
	/** Named /9 vaults, farther out. */
	public static final int GRAND_MIN_DISTANCE = 1600;
	public static final int GRAND_MAX_DISTANCE = 4800;
	/** At least this many chunks between two named vaults (when the attempts allow). */
	public static final int SITE_SEPARATION_CHUNKS = 20;
	/** A named vault's floor is this far under the ground: a crypt's floor (5) plus the stair (STAIR_STEPS), as if it hung off a crypt. */
	public static final int NAMED_DEPTH = 5 + 7;
	/** Steps in a vault's stair (it climbs this far toward the surface, then stops). */
	public static final int STAIR_STEPS = 7;
	/** A vault's pedestal holds a totem statue this often (else a fine weapon on display). */
	public static final float PRIME_STATUE_CHANCE = 0.5F;
	/** Chance a noble crypt has a vault behind its far wall. */
	public static final float NOBLE_CRYPT_VAULT_CHANCE = 0.35F;

	/** A shred found in loot is a /9 shred this often (else /4). */
	public static final float LOOT_NINE_CHANCE = 0.35F;
	/** A found shred holds 2 pieces this often, 3 this often (else 1). */
	public static final float LOOT_TWO_CHANCE = 0.15F;
	public static final float LOOT_THREE_CHANCE = 0.05F;

	/** Chance of a shred in each chest (one roll). Vanilla chests and Reworked Mobs' by id. */
	public static final Map<ResourceKey<LootTable>, Float> SHRED_LOOT = shredLoot();

	private RelicTuning() {
	}

	private static Map<ResourceKey<LootTable>, Float> shredLoot() {
		Map<ResourceKey<LootTable>, Float> m = new LinkedHashMap<>();
		put(m, "minecraft:chests/simple_dungeon", 0.30F);
		put(m, "minecraft:chests/abandoned_mineshaft", 0.20F);
		put(m, "minecraft:chests/shipwreck_map", 0.40F);
		put(m, "minecraft:chests/shipwreck_treasure", 0.30F);
		put(m, "minecraft:chests/shipwreck_supply", 0.15F);
		put(m, "minecraft:chests/buried_treasure", 0.40F);
		put(m, "minecraft:chests/desert_pyramid", 0.30F);
		put(m, "minecraft:chests/jungle_temple", 0.30F);
		put(m, "minecraft:chests/igloo_chest", 0.30F);
		put(m, "minecraft:chests/stronghold_corridor", 0.25F);
		put(m, "minecraft:chests/stronghold_crossing", 0.25F);
		put(m, "minecraft:chests/stronghold_library", 0.40F);
		put(m, "minecraft:chests/woodland_mansion", 0.30F);
		put(m, "minecraft:chests/pillager_outpost", 0.25F);
		put(m, "minecraft:chests/ancient_city", 0.25F);
		put(m, "minecraft:chests/ruined_portal", 0.15F);
		put(m, "minecraft:chests/underwater_ruin_big", 0.30F);
		put(m, "minecraft:chests/underwater_ruin_small", 0.15F);
		put(m, "minecraft:chests/village/village_cartographer", 0.30F);
		put(m, "minecraft:chests/village/village_temple", 0.15F);
		put(m, "minecraft:chests/village/village_weaponsmith", 0.10F);
		put(m, "minecraft:chests/village/village_toolsmith", 0.10F);
		put(m, "minecraft:chests/village/village_plains_house", 0.08F);
		put(m, "minecraft:chests/village/village_desert_house", 0.08F);
		put(m, "minecraft:chests/village/village_savanna_house", 0.08F);
		put(m, "minecraft:chests/village/village_snowy_house", 0.08F);
		put(m, "minecraft:chests/village/village_taiga_house", 0.08F);
		put(m, "minecraft:chests/trial_chambers/corridor", 0.10F);
		put(m, "minecraft:chests/bastion_other", 0.10F);
		put(m, "bubbas_reworked_mobs:chests/crypt_noble", 0.50F);
		put(m, "bubbas_reworked_mobs:chests/crypt_family", 0.35F);
		put(m, "bubbas_reworked_mobs:chests/crypt_pauper", 0.15F);
		put(m, "bubbas_reworked_mobs:chests/monster_camp", 0.30F);
		put(m, "bubbas_reworked_mobs:chests/camp_room", 0.30F);
		put(m, "bubbas_reworked_mobs:chests/fabricator_office", 0.25F);
		put(m, "bubbas_reworked_mobs:chests/fabricator_office_with_gems", 0.25F);
		put(m, "bubbas_reworked_mobs:gameplay/enderman_trade", 0.10F);
		return Map.copyOf(m);
	}

	private static void put(Map<ResourceKey<LootTable>, Float> m, String id, float chance) {
		m.put(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)), chance);
	}
}

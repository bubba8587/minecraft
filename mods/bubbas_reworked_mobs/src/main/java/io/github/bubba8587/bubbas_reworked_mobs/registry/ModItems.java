// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import java.util.EnumMap;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {
	/** Sleeper items, one per kind: the form is minecraft:block_state (pose, resting), gear is minecraft:container. */
	public static final Map<UndeadVariant, SleeperItem> SLEEPERS = new EnumMap<>(UndeadVariant.class);

	// The Grave Talisman and the Totem of Warding moved to Bubba's Totems & Artifacts (specs/totems-artifacts.md, C6),
	// which maps their old bubbas_reworked_mobs ids to its own.

	static {
		for (UndeadVariant variant : UndeadVariant.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id(variant.sleeperId()));
			SLEEPERS.put(variant, Registry.register(BuiltInRegistries.ITEM, key,
					new SleeperItem(ModBlocks.sleeper(variant), new Item.Properties().useBlockDescriptionPrefix().stacksTo(16)
							.component(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT
									.withHidden(net.minecraft.core.component.DataComponents.BLOCK_STATE, true))
							.setId(key))));
		}
	}

	/** Gravestone block item (placing opens the text editor, like a sign); stacks to 16 like signs. */
	public static final BlockItem GRAVESTONE;

	static {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id("gravestone"));
		GRAVESTONE = Registry.register(BuiltInRegistries.ITEM, key,
				new BlockItem(ModBlocks.GRAVESTONE, new Item.Properties().useBlockDescriptionPrefix().stacksTo(16)
							.component(net.minecraft.core.component.DataComponents.TOOLTIP_DISPLAY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT
									.withHidden(net.minecraft.core.component.DataComponents.BLOCK_STATE, true))
							.setId(key)));
	}

	// --- Wither Captain (specs/wither-captain.md) ---

	/** Captain's Helm equipment asset: assets/bubbas_reworked_mobs/equipment/captains_helm.json. */
	public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> CAPTAINS_HELM_ASSET = ResourceKey.create(
			net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, BubbasReworkedMobs.id("captains_helm"));
	/** Netherite's numbers with the helm's own look. */
	public static final net.minecraft.world.item.equipment.ArmorMaterial CAPTAINS_HELM_MATERIAL = new net.minecraft.world.item.equipment.ArmorMaterial(
			net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.durability(), net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.defense(),
			net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.enchantmentValue(), net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.equipSound(),
			net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.toughness(), net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.knockbackResistance(),
			net.minecraft.world.item.equipment.ArmorMaterials.NETHERITE.repairIngredient(), CAPTAINS_HELM_ASSET);
	public static final Item CAPTAINS_HELM = register("captains_helm", Item::new,
			new Item.Properties().humanoidArmor(CAPTAINS_HELM_MATERIAL, net.minecraft.world.item.equipment.ArmorType.HELMET).fireResistant());
	public static final Item NETHER_STAR_SHARD = register("nether_star_shard", Item::new,
			new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON).fireResistant());
	public static final Item WITHER_CAPTAIN_SPAWN_EGG = register("wither_captain_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(io.github.bubba8587.bubbas_reworked_mobs.captain.Captains.WITHER_CAPTAIN));
	public static final Item SOULFIRE_BLAZE_SPAWN_EGG = register("soulfire_blaze_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(io.github.bubba8587.bubbas_reworked_mobs.captain.Captains.SOULFIRE_BLAZE));
	public static final Item BOSS_PIGLIN_SPAWN_EGG = register("boss_piglin_spawn_egg", net.minecraft.world.item.SpawnEggItem::new,
			new Item.Properties().spawnEgg(io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglins.BOSS_PIGLIN));

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private ModItems() {
	}

	public static SleeperItem sleeper(UndeadVariant variant) {
		return SLEEPERS.get(variant);
	}

	public static void initialize() {
	}
}

// dte:C7,B15
package io.github.bubba8587.bubbas_reworked_mining.registry;

import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mining.BubbasReworkedMining;
import io.github.bubba8587.bubbas_reworked_mining.MiningTuning;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

/** The quartz tool and armor set (specs/reworked-mining.md "Quartz tools and armor"). */
public final class ModItems {
	/** Quartz blocks: what the set is crafted from and repaired with. */
	public static final TagKey<Item> QUARTZ_MATERIALS = TagKey.create(Registries.ITEM, BubbasReworkedMining.id("quartz_tool_materials"));

	public static final ToolMaterial QUARTZ_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, MiningTuning.QUARTZ_TOOL_DURABILITY,
			MiningTuning.QUARTZ_TOOL_SPEED, MiningTuning.QUARTZ_TOOL_DAMAGE_BONUS, MiningTuning.QUARTZ_TOOL_ENCHANTABILITY, QUARTZ_MATERIALS);

	public static final ResourceKey<EquipmentAsset> QUARTZ_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, BubbasReworkedMining.id("quartz"));
	public static final ArmorMaterial QUARTZ_ARMOR = new ArmorMaterial(MiningTuning.QUARTZ_ARMOR_DURABILITY,
			java.util.Map.of(ArmorType.BOOTS, MiningTuning.QUARTZ_ARMOR_BOOTS, ArmorType.LEGGINGS, MiningTuning.QUARTZ_ARMOR_LEGGINGS,
					ArmorType.CHESTPLATE, MiningTuning.QUARTZ_ARMOR_CHESTPLATE, ArmorType.HELMET, MiningTuning.QUARTZ_ARMOR_HELMET,
					ArmorType.BODY, MiningTuning.QUARTZ_ARMOR_CHESTPLATE),
			MiningTuning.QUARTZ_ARMOR_ENCHANTABILITY, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, QUARTZ_MATERIALS, QUARTZ_ASSET);

	// Attack damage and speed as vanilla's iron tools.
	public static final Item QUARTZ_SWORD = register("quartz_sword", new Item.Properties().sword(QUARTZ_TOOL, 3.0F, -2.4F));
	public static final Item QUARTZ_SHOVEL = register("quartz_shovel", new Item.Properties().shovel(QUARTZ_TOOL, 1.5F, -3.0F));
	public static final Item QUARTZ_PICKAXE = register("quartz_pickaxe", new Item.Properties().pickaxe(QUARTZ_TOOL, 1.0F, -2.8F));
	public static final Item QUARTZ_AXE = register("quartz_axe", new Item.Properties().axe(QUARTZ_TOOL, 6.0F, -3.1F));
	public static final Item QUARTZ_HOE = register("quartz_hoe", new Item.Properties().hoe(QUARTZ_TOOL, -2.0F, -1.0F));

	public static final Item QUARTZ_HELMET = register("quartz_helmet", new Item.Properties().humanoidArmor(QUARTZ_ARMOR, ArmorType.HELMET));
	public static final Item QUARTZ_CHESTPLATE = register("quartz_chestplate", new Item.Properties().humanoidArmor(QUARTZ_ARMOR, ArmorType.CHESTPLATE));
	public static final Item QUARTZ_LEGGINGS = register("quartz_leggings", new Item.Properties().humanoidArmor(QUARTZ_ARMOR, ArmorType.LEGGINGS));
	public static final Item QUARTZ_BOOTS = register("quartz_boots", new Item.Properties().humanoidArmor(QUARTZ_ARMOR, ArmorType.BOOTS));

	private ModItems() {
	}

	private static Item register(String name, Item.Properties properties) {
		return register(name, Item::new, properties);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMining.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		// Next to vanilla's iron gear in the creative tabs.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.insertAfter(Items.IRON_HOE, QUARTZ_SHOVEL, QUARTZ_PICKAXE, QUARTZ_AXE, QUARTZ_HOE));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.insertAfter(Items.IRON_SWORD, QUARTZ_SWORD);
			output.insertAfter(Items.IRON_AXE, QUARTZ_AXE);
			output.insertAfter(Items.IRON_BOOTS, QUARTZ_HELMET, QUARTZ_CHESTPLATE, QUARTZ_LEGGINGS, QUARTZ_BOOTS);
		});
	}
}

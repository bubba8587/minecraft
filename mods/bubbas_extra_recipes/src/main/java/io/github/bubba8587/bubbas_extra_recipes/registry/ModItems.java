// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.registry;

import io.github.bubba8587.bubbas_extra_recipes.BubbasExtraRecipes;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public final class ModItems {
	/** assets/bubbas_extra_recipes/equipment/glider.json: the wings drawn on the player. */
	public static final ResourceKey<EquipmentAsset> GLIDER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, BubbasExtraRecipes.id("glider"));

	/** The Glider. */
	public static final Item GLIDER = register("glider", new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.UNCOMMON)
			.component(DataComponents.GLIDER, Unit.INSTANCE)
			.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
					.setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
					.setAsset(GLIDER_ASSET)
					.setDamageOnHurt(false)
					.build()));

	private ModItems() {
	}

	private static Item register(String path, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasExtraRecipes.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(props.setId(key)));
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.insertAfter(Items.ELYTRA, GLIDER));
	}
}

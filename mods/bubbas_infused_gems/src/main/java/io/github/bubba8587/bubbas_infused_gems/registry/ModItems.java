// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.function.Function;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.item.GemItem;
import io.github.bubba8587.bubbas_infused_gems.item.InfusedGemItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** One Infused Gem item and one finished gem item, each with one model (owner); the data is in their components. */
public final class ModItems {
	public static final Item INFUSED_GEM = register("infused_gem", InfusedGemItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item GEM = register("gem", GemItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasInfusedGems.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
	}
}

// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.registry;

import java.util.function.Function;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.warding.TotemOfWardingItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	/** Where both items lived before this mod existed (Bubba's Reworked Mobs 0.1). */
	public static final String OLD_NAMESPACE = "bubbas_reworked_mobs";

	/** Grave Talisman (talisman/GraveTalisman): glows near undead; the glow is client-side only. */
	public static final Item GRAVE_TALISMAN = register("grave_talisman", Item::new, new Item.Properties().stacksTo(1));

	/** Totem of Warding (warding/TotemOfWardingItem): an artifact, found only; pulses while held, wards a Reworked Beacons beacon. */
	public static final Item TOTEM_OF_WARDING = register("totem_of_warding",
			TotemOfWardingItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasTotemsArtifacts.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		// Saved worlds: stacks saved under the old Reworked Mobs id load as this item (Fabric's registry alias: a
		// lookup of an id that isn't registered falls back to its alias). Whether or not Reworked Mobs is installed.
		BuiltInRegistries.ITEM.addAlias(Identifier.fromNamespaceAndPath(OLD_NAMESPACE, name), key.identifier());
		return item;
	}

	public static void initialize() {
	}
}

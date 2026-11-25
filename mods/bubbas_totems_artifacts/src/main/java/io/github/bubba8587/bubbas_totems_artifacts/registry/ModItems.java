// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.registry;

import java.util.function.Function;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.beginning.TotemOfTheBeginningItem;
import io.github.bubba8587.bubbas_totems_artifacts.end.TotemOfTheEndItem;
import io.github.bubba8587.bubbas_totems_artifacts.gathering.TotemOfGatheringItem;
import io.github.bubba8587.bubbas_totems_artifacts.growth.TotemOfGrowthItem;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.TotemOfSeekingItem;
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

	// The five totems of 2026-11-16 (specs/totems-artifacts.md, "The five new totems"): artifacts, found only, uncommon,
	// one per stack like the Totem of Undying. New ids, so no old-id alias.

	/** Totem of Keeping (keeping/Keeping): carried, used up on death to keep everything. */
	public static final Item TOTEM_OF_KEEPING = registerNew("totem_of_keeping", io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem::new, totem());

	/** Totem of Gathering (gathering/Gathering): held, pulls dropped items; in a beacon, a ring round it. */
	public static final Item TOTEM_OF_GATHERING = registerNew("totem_of_gathering", TotemOfGatheringItem::new, totem());

	/** Totem of Fortification (fortification/Fortification): held or in a beacon, explosions break no blocks. */
	public static final Item TOTEM_OF_FORTIFICATION = registerNew("totem_of_fortification", io.github.bubba8587.bubbas_totems_artifacts.statue.StatueTotemItem::new, totem());

	/** Totem of Growth, working name (growth/Growth): held or in a beacon, plants grow faster. */
	public static final Item TOTEM_OF_GROWTH = registerNew("totem_of_growth", TotemOfGrowthItem::new, totem());

	/** Totem of Seeking (seeking/TotemOfSeekingItem): points to the nearest structure of a chosen type. */
	public static final Item TOTEM_OF_SEEKING = registerNew("totem_of_seeking", TotemOfSeekingItem::new, totem());

	// The Totems of the Beginning and the End (2026-11-22; specs/totems-artifacts.md).

	/** Totem of the Beginning (beginning/TotemOfTheBeginningItem): reusable with a cooldown, the ground round you goes back to how it generated. */
	public static final Item TOTEM_OF_THE_BEGINNING = registerNew("totem_of_the_beginning", TotemOfTheBeginningItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	/** Totem of the End (end/TotemOfTheEndItem): held, endermen treat you as kin; used, teleports you where you aim. */
	public static final Item TOTEM_OF_THE_END = registerNew("totem_of_the_end", TotemOfTheEndItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	// Herding and Clear Skies (owner, 2026-11-22; specs/totems-artifacts.md).

	/** Totem of Herding (herding/Herding): held, livestock follow you; in a beacon or as a statue, a pasture. */
	public static final Item TOTEM_OF_HERDING = registerNew("totem_of_herding", io.github.bubba8587.bubbas_totems_artifacts.herding.TotemOfHerdingItem::new, totem());

	/** Totem of Clear Skies (skies/ClearSkies): used, ends a storm; in a beacon or as a statue, no weather in the area. */
	public static final Item TOTEM_OF_CLEAR_SKIES = registerNew("totem_of_clear_skies", io.github.bubba8587.bubbas_totems_artifacts.skies.TotemOfClearSkiesItem::new, totem());

	/** Totem of the North Wind (northwind/NorthWind): used while gliding, a rocket boost; in a beacon or as a statue, gliders get it. */
	public static final Item TOTEM_OF_NORTH_WIND = registerNew("totem_of_north_wind", io.github.bubba8587.bubbas_totems_artifacts.northwind.TotemOfNorthWindItem::new, totem());

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

	private static Item.Properties totem() {
		return new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON);
	}

	private static Item registerNew(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasTotemsArtifacts.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
	}
}

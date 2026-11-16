// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import java.util.EnumMap;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import io.github.bubba8587.bubbas_reworked_mobs.warding.TotemOfWardingItem;

public final class ModItems {
	/** Sleeper items: place a dormant undead (gear only if the stack carries a minecraft:container component). */
	public static final Map<UndeadVariant, BlockItem> SLEEPERS = new EnumMap<>(UndeadVariant.class);

	/** Grave Talisman (undead/GraveTalisman): glows near undead; the glow is client-side only. */
	public static final Item GRAVE_TALISMAN = register("grave_talisman", new Item.Properties().stacksTo(1));

	/** Totem of Warding (warding/TotemOfWardingItem): an artifact, found only; pulses while held, wards a Reworked Beacons beacon. */
	public static final Item TOTEM_OF_WARDING = register("totem_of_warding",
			TotemOfWardingItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	static {
		for (UndeadVariant variant : UndeadVariant.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id(variant.sleeperId()));
			SLEEPERS.put(variant, Registry.register(BuiltInRegistries.ITEM, key,
					new BlockItem(ModBlocks.sleeper(variant), new Item.Properties().useBlockDescriptionPrefix().stacksTo(16).setId(key))));
		}
	}

	private ModItems() {
	}

	private static Item register(String name, Item.Properties properties) {
		return register(name, Item::new, properties);
	}

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static BlockItem sleeper(UndeadVariant variant) {
		return SLEEPERS.get(variant);
	}

	public static void initialize() {
	}
}

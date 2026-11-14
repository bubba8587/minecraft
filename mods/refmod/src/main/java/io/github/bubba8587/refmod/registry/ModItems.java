// dte:A2,A4
package io.github.bubba8587.refmod.registry;

import java.util.function.Function;

import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.component.TallyCount;
import io.github.bubba8587.refmod.item.LanternCharmItem;
import io.github.bubba8587.refmod.item.PebbleItem;
import io.github.bubba8587.refmod.item.TallyStickItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {
	public static final Item TALLY_STICK = register("tally_stick", TallyStickItem::new,
			new Item.Properties().stacksTo(1).component(ModComponents.TALLY_COUNT, TallyCount.ZERO));
	/** 26.3: no AxeItem; {@code Item.Properties#axe} adds the tool, attributes and the minecraft:axe block transformer. */
	public static final Item FLINT_HATCHET = register("flint_hatchet", Item::new,
			new Item.Properties().axe(ToolMaterial.STONE, 6.0F, -3.2F));
	public static final Item PEBBLE = register("pebble", PebbleItem::new, new Item.Properties().stacksTo(16));
	/** No-new-blocks pattern (B15): places a display-entity pseudo-block instead of a registered block. */
	public static final Item LANTERN_CHARM = register("lantern_charm", LanternCharmItem::new, new Item.Properties().stacksTo(16));

	private ModItems() {
	}

	private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, RefMod.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	public static void initialize() {
	}
}

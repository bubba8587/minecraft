// dte:C1
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
					new SleeperItem(ModBlocks.sleeper(variant), new Item.Properties().useBlockDescriptionPrefix().stacksTo(16).setId(key))));
		}
	}

	/** Gravestone block item (placing opens the text editor, like a sign); stacks to 16 like signs. */
	public static final BlockItem GRAVESTONE;

	static {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BubbasReworkedMobs.id("gravestone"));
		GRAVESTONE = Registry.register(BuiltInRegistries.ITEM, key,
				new BlockItem(ModBlocks.GRAVESTONE, new Item.Properties().useBlockDescriptionPrefix().stacksTo(16).setId(key)));
	}

	private ModItems() {
	}

	public static SleeperItem sleeper(UndeadVariant variant) {
		return SLEEPERS.get(variant);
	}

	public static void initialize() {
	}
}

// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import java.util.function.Supplier;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Which totem a {@link TotemStatueBlock} shows (its {@code totem} block state property). Add a constant for each new
 * totem, plus its statue model (models/block/totem_statue_<name>.json), blockstate variants and loot entry; the
 * Grave Talisman is not a totem and has no statue (owner).
 */
public enum TotemType implements StringRepresentable {
	// Warding first: it is the default, so statues saved before the property existed load as warding statues.
	WARDING("warding", () -> ModItems.TOTEM_OF_WARDING),
	KEEPING("keeping", () -> ModItems.TOTEM_OF_KEEPING),
	GATHERING("gathering", () -> ModItems.TOTEM_OF_GATHERING),
	FORTIFICATION("fortification", () -> ModItems.TOTEM_OF_FORTIFICATION),
	GROWTH("growth", () -> ModItems.TOTEM_OF_GROWTH),
	SEEKING("seeking", () -> ModItems.TOTEM_OF_SEEKING),
	BEGINNING("beginning", () -> ModItems.TOTEM_OF_THE_BEGINNING),
	END("end", () -> ModItems.TOTEM_OF_THE_END),
	HERDING("herding", () -> ModItems.TOTEM_OF_HERDING),
	CLEAR_SKIES("clear_skies", () -> ModItems.TOTEM_OF_CLEAR_SKIES),
	NORTH_WIND("north_wind", () -> ModItems.TOTEM_OF_NORTH_WIND),
	// Vanilla's Totem of Undying: decorative, placed through UndyingStatue.
	UNDYING("undying", () -> Items.TOTEM_OF_UNDYING);

	private final String name;
	private final Supplier<Item> item;

	TotemType(String name, Supplier<Item> item) {
		this.name = name;
		this.item = item;
	}

	/** The totem item this statue is placed from and drops. */
	public Item item() {
		return item.get();
	}

	/** The statue type of a totem item, or null for an item that isn't a totem. */
	public static @org.jspecify.annotations.Nullable TotemType forItem(Item item) {
		for (TotemType type : values()) {
			if (type.item() == item) {
				return type;
			}
		}
		return null;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}

// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import java.util.function.Supplier;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * Which totem a {@link TotemStatueBlock} shows (its {@code totem} block state property). Add a constant for each new
 * totem, plus its statue model (models/block/totem_statue_<name>.json), blockstate variants and loot entry; the
 * Grave Talisman is not a totem and has no statue (owner).
 */
public enum TotemType implements StringRepresentable {
	WARDING("warding", () -> ModItems.TOTEM_OF_WARDING);

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

	@Override
	public String getSerializedName() {
		return name;
	}
}

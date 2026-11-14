// dte:B5,B6
package io.github.bubba8587.refmod.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

/** Item component on an existing item: vanilla sticks burn for 50 ticks (26.3 fuel is the COOKING_FUEL component). */
public final class ModItemComponents {
	public static final int STICK_BURN_TICKS = 50;

	private ModItemComponents() {
	}

	public static void initialize() {
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(Items.STICK, builder ->
				builder.set(DataComponents.COOKING_FUEL, new CookingFuel(
						new ResolvableInt.Constant(STICK_BURN_TICKS),
						ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)))));
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.station.SocketRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Recipe types the data can name: socketing at the smithing table. */
public final class ModRecipes {
	private ModRecipes() {
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BubbasInfusedGems.id("socket"), SocketRecipe.SERIALIZER);
	}
}

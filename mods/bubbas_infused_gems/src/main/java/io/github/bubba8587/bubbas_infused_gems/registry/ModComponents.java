// dte:C4
package io.github.bubba8587.bubbas_infused_gems.registry;

import java.util.function.UnaryOperator;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;

import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;

/** The mod's item data: an Infused Gem's brew, a finished gem's rolls, and the gems socketed into gear. */
public final class ModComponents {
	public static final DataComponentType<InfusedGem> INFUSED_GEM = register("infused_gem",
			b -> b.persistent(InfusedGem.CODEC).networkSynchronized(InfusedGem.STREAM_CODEC).cacheEncoding());
	public static final DataComponentType<Gem> GEM = register("gem",
			b -> b.persistent(Gem.CODEC).networkSynchronized(Gem.STREAM_CODEC).cacheEncoding());
	public static final DataComponentType<SocketedGems> SOCKETED_GEMS = register("socketed_gems",
			b -> b.persistent(SocketedGems.CODEC).networkSynchronized(SocketedGems.STREAM_CODEC).cacheEncoding());

	private ModComponents() {
	}

	private static <T> DataComponentType<T> register(String id, UnaryOperator<DataComponentType.Builder<T>> builder) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BubbasInfusedGems.id(id), builder.apply(DataComponentType.builder()).build());
	}

	public static void initialize() {
		ItemComponentTooltipProviderRegistry.addBefore(DataComponents.ENCHANTMENTS, INFUSED_GEM);
		ItemComponentTooltipProviderRegistry.addBefore(DataComponents.ENCHANTMENTS, GEM);
		ItemComponentTooltipProviderRegistry.addBefore(DataComponents.ENCHANTMENTS, SOCKETED_GEMS);
	}
}

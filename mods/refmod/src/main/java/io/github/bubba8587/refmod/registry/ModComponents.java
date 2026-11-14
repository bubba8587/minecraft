// dte:B5,B6
package io.github.bubba8587.refmod.registry;

import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.component.TallyCount;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;

import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;

public final class ModComponents {
	public static final DataComponentType<TallyCount> TALLY_COUNT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			RefMod.id("tally_count"),
			DataComponentType.<TallyCount>builder()
					.persistent(TallyCount.CODEC)
					.networkSynchronized(TallyCount.STREAM_CODEC)
					.build());

	private ModComponents() {
	}

	public static void initialize() {
		// Vanilla only asks its own components for tooltip lines; register ours.
		ItemComponentTooltipProviderRegistry.addLast(TALLY_COUNT);
	}
}

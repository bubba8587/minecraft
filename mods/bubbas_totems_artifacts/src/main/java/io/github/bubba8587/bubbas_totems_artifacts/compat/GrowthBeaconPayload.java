// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of Growth in a Bubba's Reworked Beacons beacon: crops and saplings across the area grow faster, more per
 * pyramid layer (growth/Growth, every tick while the area lives). Loaded only through that mod's entrypoint.
 */
public final class GrowthBeaconPayload implements BeaconPayload {
	/** Extra random ticks for a pyramid of {@code levels}. */
	public static int extra(int levels) {
		return Math.max(1, levels) * TotemsTuning.GROWTH_BEACON_EXTRA_PER_LAYER;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_GROWTH);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.growth", extra(context.levels()) + 1).withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		TotemAreas.put(level, TotemAreas.Kind.GROWTH, context.pos(), context.area(), extra(context.levels()), TotemsTuning.AREA_LIFETIME_TICKS);
	}
}

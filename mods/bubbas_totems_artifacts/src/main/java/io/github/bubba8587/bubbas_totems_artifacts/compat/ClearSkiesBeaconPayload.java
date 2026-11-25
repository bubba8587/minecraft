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

/** The Totem of Clear Skies in a Bubba's Reworked Beacons beacon. */
public final class ClearSkiesBeaconPayload implements BeaconPayload {
	/** Corner particle colour. */
	@Override
	public int color(ItemStack stack) {
		return 0x6EC6FF;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_CLEAR_SKIES);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.clear_skies").withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		TotemAreas.put(level, TotemAreas.Kind.CLEAR_SKIES, context.pos(), context.area(), 1, TotemsTuning.AREA_LIFETIME_TICKS);
	}
}

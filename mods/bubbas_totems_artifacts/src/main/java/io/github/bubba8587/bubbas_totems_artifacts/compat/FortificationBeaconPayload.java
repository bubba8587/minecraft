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
 * The Totem of Fortification in a Bubba's Reworked Beacons beacon: no explosion breaks blocks in the area
 * (fortification/Fortification). Each pulse refreshes the area a little longer than one pulse, so it lapses about 5 s
 * after the beacon stops. Any pyramid. Loaded only through that mod's entrypoint.
 */
public final class FortificationBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_FORTIFICATION);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.fortify").withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		TotemAreas.put(level, TotemAreas.Kind.FORTIFICATION, context.pos(), context.area(), 1, TotemsTuning.AREA_LIFETIME_TICKS);
	}
}

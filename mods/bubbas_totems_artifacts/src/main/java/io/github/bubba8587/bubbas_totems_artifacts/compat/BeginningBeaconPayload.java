// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of the Beginning in a Bubba's Reworked Beacons beacon: the hand pulse over the beacon's whole area, on the
 * Beginning area rhythm (beginning/BeginningAreas). Without Reworked Mobs there is no pulse, so the beacon doesn't take
 * the totem at all. Loaded only through that mod's entrypoint.
 */
public final class BeginningBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_THE_BEGINNING) && SleeperHook.get() != SleeperHook.NONE;
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.beginning").withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		if (io.github.bubba8587.bubbas_totems_artifacts.beginning.BeginningAreas.due(level, BeaconContext.PULSE_TICKS)) {
			net.minecraft.world.phys.AABB area = context.area();
			int radius = (int) Math.ceil(Math.max(area.getXsize(), area.getZsize()) / 2.0);
			io.github.bubba8587.bubbas_totems_artifacts.beginning.BeginningAreas.pulse(level, context.pos(), radius);
		}
	}
}

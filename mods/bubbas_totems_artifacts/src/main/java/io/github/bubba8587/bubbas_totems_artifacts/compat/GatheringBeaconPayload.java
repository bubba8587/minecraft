// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.gathering.GatheringRing;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of Gathering in a Bubba's Reworked Beacons beacon: the ring (gathering/GatheringRing). Loaded only through that mod's entrypoint.
 */
public final class GatheringBeaconPayload implements BeaconPayload {
	/** Corner particle colour. */
	@Override
	public int color(ItemStack stack) {
		return 0xF0A030;
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_GATHERING);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.gather").withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		GatheringRing.put(level, context.pos(), context.area(), TotemsTuning.AREA_LIFETIME_TICKS);
	}
}

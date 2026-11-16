// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.gathering.Gathering;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of Gathering in a Bubba's Reworked Beacons beacon: each pulse, dropped items in the area go into the
 * nearest chest or barrel under the beacon and XP orbs gather on the beacon (gathering/Gathering#intoChest). Any
 * pyramid. Loaded only through that mod's entrypoint.
 */
public final class GatheringBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_GATHERING);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.gather").withStyle(ChatFormatting.BLUE),
				Component.translatable("beacon.bubbas_totems_artifacts.gather_xp").withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		Gathering.intoChest(level, context.pos(), context.levels(), context.area());
	}
}

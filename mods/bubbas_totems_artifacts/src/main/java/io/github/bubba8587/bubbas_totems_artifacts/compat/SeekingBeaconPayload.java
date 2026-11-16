// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.SeekingBeacon;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.SeekingTypes;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of Seeking in a Bubba's Reworked Beacons beacon: compasses held in the area point to the nearest structure
 * of the totem's type (seeking/SeekingBeacon). A totem with no type chosen does nothing; the screen says how to choose.
 * Loaded only through that mod's entrypoint.
 */
public final class SeekingBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_SEEKING);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return SeekingTypes.byId(stack.get(ModComponents.SEEKING_TYPE))
				.map(type -> List.<Component>of(Component.translatable("beacon.bubbas_totems_artifacts.seek", type.displayName()).withStyle(ChatFormatting.BLUE)))
				.orElseGet(() -> List.of(Component.translatable("beacon.bubbas_totems_artifacts.seek_unset").withStyle(ChatFormatting.GRAY)));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		SeekingBeacon.apply(level, stack, context.pos(), context.livingEntities());
	}
}

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

/** The Totem of Herding in a Bubba's Reworked Beacons beacon. */
public final class HerdingBeaconPayload implements BeaconPayload {
	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_HERDING);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		return List.of(Component.translatable("beacon.bubbas_totems_artifacts.herding").withStyle(ChatFormatting.BLUE));
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		TotemAreas.put(level, TotemAreas.Kind.HERDING, context.pos(), context.area(), 1, TotemsTuning.AREA_LIFETIME_TICKS);
	}
}

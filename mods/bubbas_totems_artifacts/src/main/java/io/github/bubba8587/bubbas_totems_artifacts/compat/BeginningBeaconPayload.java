// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of the Beginning in a Bubba's Reworked Beacons beacon: the area draws Reworked Mobs' caretaker endermen like
 * a cairn (they spawn there at night and tidy the ground back toward how it generated). Through compat/SleeperHook;
 * each pulse refreshes the area a little longer than one pulse, so it lapses about 5 s after the beacon stops. Any
 * pyramid. Without Reworked Mobs there are no caretakers, so the beacon doesn't take the totem at all. Loaded only
 * through that mod's entrypoint.
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
		SleeperHook.get().drawEndermen(level, context.pos(), context.area(), TotemsTuning.AREA_LIFETIME_TICKS);
	}
}

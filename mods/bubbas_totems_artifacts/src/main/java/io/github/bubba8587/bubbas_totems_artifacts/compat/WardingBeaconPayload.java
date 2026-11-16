// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.warding.Wards;
import io.github.bubba8587.bubbas_totems_artifacts.warding.WardingTuning;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Totem of Warding in a Bubba's Reworked Beacons beacon (owner, 2026-11-16; specs/undead.md "Warding"): the
 * beacon's area becomes a ward. Any pyramid: no natural monster spawns and monsters don't enter; a full 4-layer
 * pyramid also keeps sleepers asleep (Bubba's Reworked Mobs). Range is the beacon's (sky column or paired box). Each pulse refreshes the ward
 * for a little longer than one pulse, so a beacon that goes idle, is broken or unloads stops warding within about
 * 5 seconds with no cleanup hook. The only class that touches the beacons API; loaded only through its entrypoint,
 * so only with that mod installed.
 */
public final class WardingBeaconPayload implements BeaconPayload {
	/** Pyramid layers at which the ward also keeps sleepers asleep. */
	public static final int SLEEP_LEVELS = 4;

	@Override
	public boolean accepts(ItemStack stack) {
		return stack.is(ModItems.TOTEM_OF_WARDING);
	}

	@Override
	public List<Component> describe(ItemStack stack, BeaconContext context) {
		List<Component> out = new ArrayList<>();
		out.add(Component.translatable("beacon.bubbas_totems_artifacts.ward").withStyle(ChatFormatting.BLUE));
		// The sleepers line only means something with Bubba's Reworked Mobs installed (its sleeping undead).
		if (SleeperHook.get() != SleeperHook.NONE) {
			out.add(Component.translatable(context.levels() >= SLEEP_LEVELS ? "beacon.bubbas_totems_artifacts.ward_sleepers"
					: "beacon.bubbas_totems_artifacts.ward_sleepers_needs").withStyle(context.levels() >= SLEEP_LEVELS ? ChatFormatting.BLUE : ChatFormatting.GRAY));
		}
		return out;
	}

	@Override
	public void apply(ServerLevel level, ItemStack stack, BeaconContext context) {
		Wards.put(level, context.pos(), context.area(), context.levels() >= SLEEP_LEVELS,
				WardingTuning.WARD_DEFAULT_LIFETIME_TICKS);
	}
}

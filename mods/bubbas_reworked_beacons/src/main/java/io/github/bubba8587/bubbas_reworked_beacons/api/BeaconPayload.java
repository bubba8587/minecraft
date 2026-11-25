// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** "This item in a beacon does X at power Y". */
public interface BeaconPayload {
	/** The fabric.mod.json entrypoint key. */
	String ENTRYPOINT = "bubbas_reworked_beacons:payload";

	/** Whether this payload handles the stack at all. */
	boolean accepts(ItemStack stack);

	/** When several payloads accept the same stack, the highest priority wins. */
	default int priority() {
		return 0;
	}

	/** The item's own power on the Gem Power scale, 1 to 6. */
	default int power(ItemStack stack) {
		return 1;
	}

	/** Pyramid layers needed before the item works at all. */
	default int requiredLevels(ItemStack stack) {
		return 1;
	}

	/** What the beacon will do, in the item's own tooltip words, for the beacon screen. */
	List<Component> describe(ItemStack stack, BeaconContext context);

	/** One pulse of an active beacon. */
	void apply(ServerLevel level, ItemStack stack, BeaconContext context);

	/** The colour of the corner particles shown when the beacon's effect changes (0xRRGGBB). */
	default int color(ItemStack stack) {
		return 0xFFFFFF;
	}

	/** True turns off Reworked Beacons' standalone payments while this payload is installed. */
	default boolean replacesStandalonePayments() {
		return false;
	}
}

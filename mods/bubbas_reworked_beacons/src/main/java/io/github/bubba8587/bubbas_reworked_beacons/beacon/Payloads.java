// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.beacon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_beacons.BubbasReworkedBeacons;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

/** Every BeaconPayload from the ENTRYPOINT entrypoint, loaded on first use. */
public final class Payloads {
	private static @Nullable List<BeaconPayload> all;

	private Payloads() {
	}

	public static synchronized List<BeaconPayload> all() {
		if (all == null) {
			List<BeaconPayload> found = new ArrayList<>();
			for (EntrypointContainer<BeaconPayload> c : FabricLoader.getInstance().getEntrypointContainers(BeaconPayload.ENTRYPOINT, BeaconPayload.class)) {
				try {
					found.add(c.getEntrypoint());
					BubbasReworkedBeacons.LOGGER.info("Beacon payload from {}: {}", c.getProvider().getMetadata().getId(), c.getEntrypoint().getClass().getName());
				} catch (Throwable t) {
					BubbasReworkedBeacons.LOGGER.error("Beacon payload from {} failed to load", c.getProvider().getMetadata().getId(), t);
				}
			}
			boolean standaloneOff = found.stream().anyMatch(BeaconPayload::replacesStandalonePayments);
			if (standaloneOff) {
				found.removeIf(p -> p instanceof StandalonePayload);
			}
			found.sort(Comparator.comparingInt(BeaconPayload::priority).reversed());
			all = List.copyOf(found);
		}
		return all;
	}

	/** The payload that handles this stack, or null if none does. */
	public static @Nullable BeaconPayload find(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (BeaconPayload p : all()) {
			if (p.accepts(stack)) {
				return p;
			}
		}
		return null;
	}
}

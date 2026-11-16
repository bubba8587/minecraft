// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * "This item in a beacon does X at power Y" (specs/reworked-beacons.md, "Payload API").
 *
 * <p>Other mods supply beacon items by implementing this interface and listing the class under the
 * {@value #ENTRYPOINT} entrypoint in their {@code fabric.mod.json}. They compile against this package only
 * ({@code compileOnly}); without Reworked Beacons installed the entrypoint is simply never loaded. Implementations
 * need a public no-argument constructor and are created lazily, after every mod has initialised.
 *
 * <p>Reworked Beacons' own standalone payments (amethyst shard = Speed, ...) are a payload like any other, with
 * {@link #priority()} {@code -1000}.
 */
public interface BeaconPayload {
	/** The {@code fabric.mod.json} entrypoint key. */
	String ENTRYPOINT = "bubbas_reworked_beacons:payload";

	/** Whether this payload handles the stack at all. The beacon's slot only accepts stacks some payload accepts. */
	boolean accepts(ItemStack stack);

	/** When several payloads accept the same stack, the highest priority wins. Default 0. */
	default int priority() {
		return 0;
	}

	/**
	 * The item's own power on the Gem Power scale, 1 to 6. The beacon caps it by pyramid size (1 layer: I,
	 * 2: III, 3: V, 4: VI) and shows "Only outputting Gem Power N" when it does. Items with no power return 1.
	 */
	default int power(ItemStack stack) {
		return 1;
	}

	/** Pyramid layers needed before the item works at all (1 to 4). Below it the beacon says so and stays idle. */
	default int requiredLevels(ItemStack stack) {
		return 1;
	}

	/**
	 * What the beacon will do, in the item's own tooltip words, for the beacon screen. Called on the client and
	 * the server; must not touch the world. {@code context.levels()} is at least 1 here (an idle beacon is
	 * described as if it had a one-layer pyramid; the screen shows the idle state itself).
	 */
	List<Component> describe(ItemStack stack, BeaconContext context);

	/**
	 * One pulse of an active beacon, on the server, every {@link BeaconContext#PULSE_TICKS} ticks (vanilla's
	 * rhythm). {@code context.livingEntities()} lists every living thing in the area, players and mobs alike.
	 * Effects should last {@link BeaconContext#effectDurationTicks()} so they overlap the next pulse.
	 */
	void apply(ServerLevel level, ItemStack stack, BeaconContext context);

	/**
	 * True turns off Reworked Beacons' standalone payments (amethyst shard = Speed, ...) while this payload is
	 * installed, so the slot takes only the other mod's items. Default false.
	 */
	default boolean replacesStandalonePayments() {
		return false;
	}
}

// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

/** How a beacon reaches its area. */
public enum BeaconMode {
	/** Idle: no pyramid, no clear sky and no pair, or no working item. */
	IDLE,
	/** Vanilla's area: a square box out to the range, from below the beacon up to the build limit. */
	SKY,
	/** A floor beacon paired with an upside-down beacon above it: a wide, height-limited box. */
	PAIRED
}

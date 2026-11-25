// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.beacon;

import net.minecraft.world.item.ItemStack;

/** What BeaconBlockEntityMixin adds to every vanilla beacon block entity. */
public interface ReworkedBeacon {
	ItemStack bubba$item();

	void bubba$setItem(ItemStack stack);

	/** Vanilla's sky check: pyramid layers if the beam reaches the sky, else 0. */
	int bubba$skyLevels();

	/** This beacon's own pyramid, counted on every pair check. */
	int bubba$floorLevels();

	/** Blocks up to the upside-down beacon this one pairs with, or 0 when not paired. */
	int bubba$pairGap();

	/** The upside-down beacon's pyramid. */
	int bubba$ceilingLevels();

	void bubba$setPair(int floorLevels, int gap, int ceilingLevels);

	/** True between the pulse that made the pair active and the one that ended it. */
	boolean bubba$wasPaired();

	void bubba$setWasPaired(boolean paired);

	/** What the corner particles last showed (item, power, area), 0 for nothing; not saved. */
	int bubba$edgeSignature();

	/** False until the first pulse after loading, which records the signature without showing it. */
	boolean bubba$edgeSeeded();

	void bubba$setEdge(int signature, boolean seeded);

	/** Ticks of corner particles left to show. */
	int bubba$edgeFlash();

	void bubba$setEdgeFlash(int ticks);

	default boolean bubba$isPaired() {
		return this.bubba$pairGap() > 0 && this.bubba$floorLevels() > 0 && this.bubba$ceilingLevels() > 0;
	}

	/** A pair's layers: the smaller pyramid. */
	default int bubba$pairLevels() {
		return Math.min(this.bubba$floorLevels(), this.bubba$ceilingLevels());
	}
}

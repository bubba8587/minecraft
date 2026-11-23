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

	default boolean bubba$isPaired() {
		return this.bubba$pairGap() > 0 && this.bubba$floorLevels() > 0 && this.bubba$ceilingLevels() > 0;
	}

	/** A pair's layers: the smaller pyramid. */
	default int bubba$pairLevels() {
		return Math.min(this.bubba$floorLevels(), this.bubba$ceilingLevels());
	}
}

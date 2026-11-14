// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.display;

import net.minecraft.util.StringRepresentable;

/**
 * How a displayed item sits in the world (blockstate {@code placement}). The client pose table is
 * {@code LootDisplayRenderer}. {@code facing} is the side the item shows its front to (for {@link #WALL}: away
 * from the wall it hangs on; for the floor placements: toward whoever placed it).
 */
public enum DisplayPlacement implements StringRepresentable {
	/** Lying flat on the floor, grip toward {@code facing}. */
	GROUND("ground"),
	/** Mostly sunk into the block below, the grip sticking up at an angle. Needs a shovel to take. */
	BURIED("buried"),
	/** Hung flat on the wall behind it. */
	WALL("wall"),
	/** Planted point-down, upright. */
	STUCK("stuck");

	private final String name;

	DisplayPlacement(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}

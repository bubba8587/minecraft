// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reading and filling a beacon's one item slot from another mod (specs/reworked-beacons.md, "Payload API"): for
 * structures that come with a loaded beacon (Bubba's Reworked Mobs' Ancient Fabricators put their Fabricator Heart
 * in theirs). Server side. Callers in other mods must only reach this class when Reworked Beacons is loaded.
 */
public final class BeaconSlots {
	private BeaconSlots() {
	}

	/** The item in the beacon at {@code pos}, or empty when there's no beacon. */
	public static ItemStack item(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof ReworkedBeacon beacon ? beacon.bubba$item() : ItemStack.EMPTY;
	}

	/** Puts {@code stack} in the beacon at {@code pos} (replacing what was there); false when there's no beacon. */
	public static boolean setItem(Level level, BlockPos pos, ItemStack stack) {
		if (level.getBlockEntity(pos) instanceof ReworkedBeacon beacon) {
			beacon.bubba$setItem(stack.copy());
			((net.minecraft.world.level.block.entity.BlockEntity) beacon).setChanged();
			return true;
		}
		return false;
	}
}

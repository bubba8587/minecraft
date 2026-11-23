// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reading and filling a beacon's one item slot from another mod. */
public final class BeaconSlots {
	private BeaconSlots() {
	}

	/** The item in the beacon at pos, or empty when there's no beacon. */
	public static ItemStack item(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof ReworkedBeacon beacon ? beacon.bubba$item() : ItemStack.EMPTY;
	}

	/** Puts stack in the beacon at pos; false when there's no beacon. */
	public static boolean setItem(Level level, BlockPos pos, ItemStack stack) {
		if (level.getBlockEntity(pos) instanceof ReworkedBeacon beacon) {
			beacon.bubba$setItem(stack.copy());
			((net.minecraft.world.level.block.entity.BlockEntity) beacon).setChanged();
			return true;
		}
		return false;
	}
}

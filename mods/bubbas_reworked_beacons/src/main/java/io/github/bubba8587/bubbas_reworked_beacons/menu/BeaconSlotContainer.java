// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.menu;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconAreas;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.Payloads;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/** The beacon's one slot. */
public class BeaconSlotContainer implements Container {
	private final BeaconBlockEntity be;

	public BeaconSlotContainer(BeaconBlockEntity be) {
		this.be = be;
	}

	private ReworkedBeacon rb() {
		return (ReworkedBeacon) this.be;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.rb().bubba$item().isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? this.rb().bubba$item() : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		if (slot != 0 || this.isEmpty() || count <= 0) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = this.rb().bubba$item().split(count);
		this.setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack item = this.getItem(slot);
		this.rb().bubba$setItem(ItemStack.EMPTY);
		return item;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			this.rb().bubba$setItem(stack);
			this.setChanged();
		}
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return Payloads.find(stack) != null;
	}

	@Override
	public void setChanged() {
		this.be.setChanged();
		if (this.be.getLevel() != null && !this.be.getLevel().isClientSide()) {
			BeaconAreas.remove(this.be.getLevel(), this.be.getBlockPos());
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this.be, player);
	}

	@Override
	public void clearContent() {
		this.rb().bubba$setItem(ItemStack.EMPTY);
	}
}

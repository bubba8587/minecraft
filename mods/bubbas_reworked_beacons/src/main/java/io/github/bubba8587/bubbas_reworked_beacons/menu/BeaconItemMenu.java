// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.menu;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.Payloads;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** The reworked beacon screen's menu. */
public class BeaconItemMenu extends AbstractContainerMenu {
	public static final int DATA_MODE = 0;
	public static final int DATA_LEVELS = 1;
	public static final int DATA_FLOOR_LEVELS = 2;
	public static final int DATA_COUNT = 3;

	private final Container beacon;
	private final ContainerData data;
	private final ContainerLevelAccess access;
	private final Slot itemSlot;

	/** Client side: filled in by the server's slot and data sync. */
	public BeaconItemMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public BeaconItemMenu(int containerId, Inventory inventory, Container beacon, ContainerData data, ContainerLevelAccess access) {
		super(ModMenus.BEACON, containerId);
		checkContainerSize(beacon, 1);
		checkContainerDataCount(data, DATA_COUNT);
		this.beacon = beacon;
		this.data = data;
		this.access = access;
		this.itemSlot = this.addSlot(new Slot(beacon, 0, 136, 110) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Payloads.find(stack) != null;
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		this.addDataSlots(data);
		this.addStandardInventorySlots(inventory, 36, 137);
	}

	public ItemStack item() {
		return this.itemSlot.getItem();
	}

	public BeaconMode mode() {
		int m = this.data.get(DATA_MODE);
		return m >= 0 && m < BeaconMode.values().length ? BeaconMode.values()[m] : BeaconMode.IDLE;
	}

	/** Layers that count for the current mode, 0 when idle. */
	public int levels() {
		return this.data.get(DATA_LEVELS);
	}

	/** The beacon's own pyramid below it, counted even when idle. */
	public int floorLevels() {
		return this.data.get(DATA_FLOOR_LEVELS);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, Blocks.BEACON);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			clicked = stack.copy();
			if (slotIndex == 0) {
				if (!this.moveItemStackTo(stack, 1, 37, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, clicked);
			} else if (!this.itemSlot.hasItem() && this.itemSlot.mayPlace(stack)) {
				if (!this.moveItemStackTo(stack, 0, 1, false)) {
					return ItemStack.EMPTY;
				}
			} else if (slotIndex < 28) {
				if (!this.moveItemStackTo(stack, 28, 37, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 1, 28, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			if (stack.getCount() == clicked.getCount()) {
				return ItemStack.EMPTY;
			}
			slot.onTake(player, stack);
		}
		return clicked;
	}
}

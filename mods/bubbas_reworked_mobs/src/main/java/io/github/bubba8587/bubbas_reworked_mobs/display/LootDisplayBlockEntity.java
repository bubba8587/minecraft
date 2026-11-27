// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.display;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The one item on display. Saved like a one-slot container ({@code Items}, and the vanilla minecraft:container
 * component on items), so it survives save/load, structure templates and {@code /setblock ...{Items:[...]}}.
 * Nothing ticks.
 */
public class LootDisplayBlockEntity extends BlockEntity {
	private final NonNullList<ItemStack> slot = NonNullList.withSize(1, ItemStack.EMPTY);

	public LootDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LOOT_DISPLAY, pos, state);
	}

	/** The displayed item (do not modify; use {@link #setItem} / {@link #takeItem}). */
	public ItemStack getItem() {
		return slot.getFirst();
	}

	public void setItem(ItemStack stack) {
		slot.set(0, stack.copyWithCount(Math.min(1, stack.getCount())));
		changed();
	}

	/** Removes and returns the item, so removing the block afterwards drops nothing. */
	public ItemStack takeItem() {
		ItemStack taken = slot.getFirst();
		slot.set(0, ItemStack.EMPTY);
		changed();
		return taken;
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Mined, exploded, popped or replaced: the item falls out. Taking it first ({@link #takeItem}) leaves nothing to drop. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), slot.getFirst());
			slot.set(0, ItemStack.EMPTY);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slot);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slot.set(0, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, slot);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slot));
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		slot.set(0, ItemStack.EMPTY);
		components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slot);
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("Items");
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The client needs the item to draw it. */
	@Override
	public CompoundTag getUpdateTag(HolderGetter.Provider registries) {
		return saveCustomOnly(registries);
	}
}

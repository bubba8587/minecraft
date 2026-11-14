// dte:B5,B6
package io.github.bubba8587.refmod.block;

import io.github.bubba8587.refmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Block entity: counts server ticks, saves through ValueOutput/ValueInput, syncs to clients once a second. */
public class CounterBlockEntity extends BlockEntity {
	public static final String TICKS_KEY = "ticks";
	public static final int SYNC_INTERVAL = 20;
	private long ticks;

	public CounterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.COUNTER, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CounterBlockEntity be) {
		be.ticks++;
		be.setChanged();
		if (be.ticks % SYNC_INTERVAL == 0) {
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
	}

	public long getTicks() {
		return ticks;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong(TICKS_KEY, ticks);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		ticks = input.getLongOr(TICKS_KEY, 0L);
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}

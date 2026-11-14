// dte:B5,B6
package io.github.bubba8587.refmod.block;

import io.github.bubba8587.refmod.network.LampTogglePayload;
import io.github.bubba8587.refmod.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Block + block state: right-click toggles {@link #LIT}; counts toggles per player and tells the client. */
public class SignalLampBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public SignalLampBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			BlockState toggled = state.cycle(LIT);
			level.setBlockAndUpdate(pos, toggled);
			level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.5F, toggled.getValue(LIT) ? 0.6F : 0.5F);
			if (player instanceof ServerPlayer serverPlayer) {
				int total = ModAttachments.incrementLampToggles(serverPlayer);
				if (serverPlayer.connection != null) {
					ServerPlayNetworking.send(serverPlayer, new LampTogglePayload(pos, toggled.getValue(LIT), total));
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}

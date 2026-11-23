// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.display;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Laying a held item on the ground as a display: players (sneak + middle click) and mobs alike. */
public final class HandPlacing {
	/** A player's sneak + middle click on a block face. */
	public record LayRequest(BlockPos pos, Direction face) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<LayRequest> TYPE = new CustomPacketPayload.Type<>(BubbasReworkedMobs.id("lay_item"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LayRequest> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, LayRequest::pos,
				Direction.STREAM_CODEC, LayRequest::face,
				LayRequest::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private HandPlacing() {
	}

	public static void initialize() {
		PayloadTypeRegistry.serverboundPlay().register(LayRequest.TYPE, LayRequest.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LayRequest.TYPE, (payload, context) -> request(context.player(), payload));
	}

	/** Whether an item can be laid at target: free (air or water) over a floor that holds a display. */
	public static boolean canLayAt(Level level, BlockPos target) {
		var state = level.getBlockState(target);
		return (state.isAir() || state.is(Blocks.WATER) && state.getFluidState().isSource()) && Block.canSupportCenter(level, target.below(), Direction.UP);
	}

	/** Lays one of stack at target, facing the placer; the stack shrinks by one. */
	public static boolean lay(ServerLevel level, @Nullable LivingEntity placer, BlockPos target, ItemStack stack) {
		if (stack.isEmpty() || !canLayAt(level, target)) {
			return false;
		}
		Direction facing = placer == null ? Direction.NORTH : placer.getDirection().getOpposite();
		LootDisplays.place(level, target, DisplayPlacement.GROUND, facing, stack.split(1));
		level.playSound(null, target, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		level.gameEvent(placer, GameEvent.BLOCK_PLACE, target);
		return true;
	}

	private static void request(ServerPlayer player, LayRequest request) {
		ServerLevel level = player.level();
		ItemStack held = player.getMainHandItem();
		BlockPos target = request.pos().relative(request.face());
		if (held.isEmpty() || !player.isAlive() || player.isSpectator() || !player.isWithinBlockInteractionRange(request.pos(), 1.0)
				|| !level.mayInteract(player, target) || !player.mayUseItemAt(target, request.face(), held)) {
			return;
		}
		ItemStack from = player.hasInfiniteMaterials() ? held.copyWithCount(1) : held;
		lay(level, player, target, from);
	}
}

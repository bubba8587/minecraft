// dte:B5,B6
package io.github.bubba8587.refmod.network;

import io.github.bubba8587.refmod.RefMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C: a signal lamp the player toggled, its new state, and the player's running toggle total. */
public record LampTogglePayload(BlockPos pos, boolean lit, int total) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LampTogglePayload> TYPE = new CustomPacketPayload.Type<>(RefMod.id("lamp_toggle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LampTogglePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, LampTogglePayload::pos,
			ByteBufCodecs.BOOL, LampTogglePayload::lit,
			ByteBufCodecs.VAR_INT, LampTogglePayload::total,
			LampTogglePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

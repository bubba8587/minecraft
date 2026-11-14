// dte:A2,A4,B15
package io.github.bubba8587.refmod.network;

import io.github.bubba8587.refmod.RefMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;

/**
 * Turns away clients without the mod (requirement 18). Adding a block state property shifts every block state id
 * after that block. Fabric's registry sync rebuilds state ids from each side's own blocks and sends nothing when no
 * registry has modded entries, so a client without the property would join and decode chunks with shifted ids.
 * The client registers a receiver for this configuration channel; a client that didn't is disconnected.
 */
public record ExtraStatesHandshake() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ExtraStatesHandshake> TYPE = new CustomPacketPayload.Type<>(RefMod.id("extra_block_states"));
	public static final StreamCodec<FriendlyByteBuf, ExtraStatesHandshake> CODEC = StreamCodec.unit(new ExtraStatesHandshake());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundConfiguration().register(TYPE, CODEC);
		ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
			if (!ServerConfigurationNetworking.canSend(listener, TYPE)) {
				listener.disconnect(Component.literal("This server adds block states (refmod): install refmod on your client."));
			}
		});
	}
}

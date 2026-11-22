// dte:C5,B15
package io.github.bubba8587.bubbas_reworked_beacons.network;

import io.github.bubba8587.bubbas_reworked_beacons.BubbasReworkedBeacons;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;

/**
 * Turns away clients without the mod (as Extra Recipes does, docs/knowledge/item-keyed-block-drops.md). The
 * upside-down state shifts every block state id after the beacon, and Fabric's registry sync sends nothing for
 * state-only changes, so a client without the mod would decode chunks wrongly. The menu type is a modded
 * registry entry, which registry sync already checks; this keeps the rule explicit.
 */
public record ExtraStatesHandshake() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ExtraStatesHandshake> TYPE = new CustomPacketPayload.Type<>(BubbasReworkedBeacons.id("extra_block_states"));
	public static final StreamCodec<FriendlyByteBuf, ExtraStatesHandshake> CODEC = StreamCodec.unit(new ExtraStatesHandshake());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundConfiguration().register(TYPE, CODEC);
		ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
			if (!ServerConfigurationNetworking.canSend(listener, TYPE)) {
				listener.disconnect(Component.literal("This server runs Bubba's Reworked Beacons, which adds a beacon block state: install the same mod on your client."));
			}
		});
	}
}

// dte:A2,A4
package io.github.bubba8587.refmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class RefModNetworking {
	private RefModNetworking() {
	}

	/** Payload types are registered in common code so the dedicated server knows them too. */
	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(LampTogglePayload.TYPE, LampTogglePayload.CODEC);
	}
}

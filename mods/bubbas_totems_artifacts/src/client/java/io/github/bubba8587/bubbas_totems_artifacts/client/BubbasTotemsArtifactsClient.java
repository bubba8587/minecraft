// dte:C6,B16
package io.github.bubba8587.bubbas_totems_artifacts.client;

import io.github.bubba8587.bubbas_totems_artifacts.client.keeping.KeepingEffect;
import io.github.bubba8587.bubbas_totems_artifacts.client.talisman.GraveTalismanGlow;

import net.fabricmc.api.ClientModInitializer;

public class BubbasTotemsArtifactsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Grave Talisman: glow level worked out on the client, read by the item model.
		GraveTalismanGlow.initialize();

		// Totem of Keeping: the totem animation on respawn after a kept death.
		KeepingEffect.initialize();

		// Totem of Clear Skies: the server's dry areas, so rain and snow aren't drawn there.
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies.AreasPayload.TYPE,
				(payload, context) -> io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies.received(payload));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> io.github.bubba8587.bubbas_totems_artifacts.skies.ClearSkies.clearClient());
	}
}

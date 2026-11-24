// dte:C6,B16
package io.github.bubba8587.bubbas_totems_artifacts.client;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.client.keeping.KeepingEffect;
import io.github.bubba8587.bubbas_totems_artifacts.client.talisman.GraveTalismanGlow;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;

public class BubbasTotemsArtifactsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 32x textures (Classic Faithful style) as an optional built-in resource pack: specs/textures.md.
		FabricLoader.getInstance().getModContainer(BubbasTotemsArtifacts.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasTotemsArtifacts.id(BubbasTotemsArtifacts.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Totems & Artifacts 32x (Classic Faithful style)"), PackActivationType.NORMAL));

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

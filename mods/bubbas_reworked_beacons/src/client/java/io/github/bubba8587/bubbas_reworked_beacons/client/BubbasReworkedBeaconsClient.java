package io.github.bubba8587.bubbas_reworked_beacons.client;

import io.github.bubba8587.bubbas_reworked_beacons.BubbasReworkedBeacons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class BubbasReworkedBeaconsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 32x textures (Faithful style) as an optional built-in resource pack: specs/textures.md.
		FabricLoader.getInstance().getModContainer(BubbasReworkedBeacons.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasReworkedBeacons.id(BubbasReworkedBeacons.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Reworked Beacons 32x (Faithful style)"), PackActivationType.NORMAL));
	}
}

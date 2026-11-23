package io.github.bubba8587.bubbas_reworked_mining.client;

import io.github.bubba8587.bubbas_reworked_mining.BubbasReworkedMining;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class BubbasReworkedMiningClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance().getModContainer(BubbasReworkedMining.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasReworkedMining.id(BubbasReworkedMining.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Reworked Mining 32x (Faithful style)"), PackActivationType.NORMAL));
	}
}

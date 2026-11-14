package io.github.bubba8587.bubbas_reworked_mobs.client;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class BubbasReworkedMobsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 32x textures (Faithful style) as an optional built-in resource pack: specs/textures.md.
		FabricLoader.getInstance().getModContainer(BubbasReworkedMobs.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasReworkedMobs.id(BubbasReworkedMobs.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Reworked Mobs 32x (Faithful style)"), PackActivationType.NORMAL));
	}
}

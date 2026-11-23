// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import io.github.bubba8587.bubbas_world_tweaks.lava.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.network.chat.Component;

public class BubbasWorldTweaksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader.getInstance().getModContainer(BubbasWorldTweaks.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasWorldTweaks.id(BubbasWorldTweaks.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's World Tweaks 32x (Faithful style)"), PackActivationType.NORMAL));
		ParticleProviderRegistry.getInstance().register(ModParticles.LAVA_BUBBLE, LavaBubbleParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(ModParticles.LAVA_GEYSER_PLUME, LavaGeyserPlumeParticle.Provider::new);
		EntityRenderers.register(LavaGeysers.LAVA_GEYSER, NoopRenderer::new);
	}
}

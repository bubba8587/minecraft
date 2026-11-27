// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.client;

import io.github.bubba8587.bubbas_world_tweaks.client.golem.AmethystGolemRenderer;
import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import io.github.bubba8587.bubbas_world_tweaks.lava.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

public class BubbasWorldTweaksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleProviderRegistry.getInstance().register(ModParticles.LAVA_BUBBLE, LavaBubbleParticle.Provider::new);
		ParticleProviderRegistry.getInstance().register(ModParticles.LAVA_GEYSER_PLUME, LavaGeyserPlumeParticle.Provider::new);
		EntityRenderers.register(LavaGeysers.LAVA_GEYSER, NoopRenderer::new);
		AmethystGolemRenderer.initialize();
	}
}

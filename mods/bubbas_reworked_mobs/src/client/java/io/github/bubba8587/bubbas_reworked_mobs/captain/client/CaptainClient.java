// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import io.github.bubba8587.bubbas_reworked_mobs.captain.Captains;
import io.github.bubba8587.bubbas_reworked_mobs.captain.WitherCaptain;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Client side of the Wither Captain (specs/wither-captain.md): renderers, the helm's horns, summoning particles. */
public final class CaptainClient {
	private CaptainClient() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void initialize() {
		EntityRenderers.register(Captains.WITHER_CAPTAIN, CaptainRenderer::new);
		EntityRenderers.register(Captains.SOULFIRE_BLAZE, SoulfireBlazeRenderer::new);
		EntityRenderers.register(Captains.SOULFIRE_CHARGE, context -> new ThrownItemRenderer<>(context, 0.75F, true));
		ModelLayerRegistry.registerModelLayer(CaptainsHelmHornsLayer.LAYER, CaptainsHelmHorns::createLayer);
		// horns on every humanoid renderer (players, armour stands, zombies, skeletons, the captain)
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof HumanoidModel<?>) {
				helper.register((RenderLayer) new CaptainsHelmHornsLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
						(LivingEntityRenderer) renderer, context.bakeLayer(CaptainsHelmHornsLayer.LAYER)));
			}
		});
		WitherCaptain.clientSummoningTick = captain -> {
			if (captain.level() instanceof ClientLevel level) {
				CaptainParticles.summoningTick(level, captain.getX(), captain.getY(), captain.getZ(), captain.getBbHeight(), captain.getRandom());
			}
		};
	}
}

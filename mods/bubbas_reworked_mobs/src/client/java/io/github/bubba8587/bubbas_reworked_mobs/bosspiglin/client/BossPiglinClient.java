// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglins;
import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/** Client side of the Boss Piglin (specs/boss-piglin.md): renderer and model layers. */
public final class BossPiglinClient {
	private BossPiglinClient() {
	}

	public static void initialize() {
		ModelLayerRegistry.registerModelLayer(BossPiglinRenderer.BODY, BossPiglinModel::createBodyLayer);
		var armor = BossPiglinModel.createArmorLayers();
		ModelLayerRegistry.registerModelLayer(BossPiglinRenderer.ARMOR.head(), armor::head);
		ModelLayerRegistry.registerModelLayer(BossPiglinRenderer.ARMOR.chest(), armor::chest);
		ModelLayerRegistry.registerModelLayer(BossPiglinRenderer.ARMOR.legs(), armor::legs);
		ModelLayerRegistry.registerModelLayer(BossPiglinRenderer.ARMOR.feet(), armor::feet);
		EntityRenderers.register(BossPiglins.BOSS_PIGLIN, BossPiglinRenderer::new);
	}
}

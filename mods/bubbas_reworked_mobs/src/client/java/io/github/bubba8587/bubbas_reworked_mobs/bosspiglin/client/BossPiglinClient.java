// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.client;

import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.BossPiglins;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Client side of the Boss Piglin (specs/boss-piglin.md): its renderer (the model bakes its own layers). */
public final class BossPiglinClient {
	private BossPiglinClient() {
	}

	public static void initialize() {
		EntityRenderers.register(BossPiglins.BOSS_PIGLIN, BossPiglinRenderer::new);
	}
}

// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Blaze;

/** Soulfire Blaze: the blaze renderer with its blue texture, and no orange burning overlay (it shows soul flames instead). */
public class SoulfireBlazeRenderer extends BlazeRenderer {
	private static final Identifier TEXTURE = BubbasReworkedMobs.id("textures/entity/soulfire_blaze.png");

	public SoulfireBlazeRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public void extractRenderState(Blaze entity, LivingEntityRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.displayFireAnimation = false;
	}
}

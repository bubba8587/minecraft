// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.client.creeper.CreeperSkinState;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries a creeper's biome skin from extract to render. */
@Mixin(CreeperRenderState.class)
abstract class CreeperRenderStateSkinMixin implements CreeperSkinState {
	@Unique
	private @Nullable Identifier brm$skinTexture;

	@Override
	public @Nullable Identifier brm$skinTexture() {
		return brm$skinTexture;
	}

	@Override
	public void brm$setSkinTexture(@Nullable Identifier texture) {
		brm$skinTexture = texture;
	}
}

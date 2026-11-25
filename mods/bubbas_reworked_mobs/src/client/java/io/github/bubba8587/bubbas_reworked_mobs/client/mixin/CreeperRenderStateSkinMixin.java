// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.client.creeper.CreeperCarryState;
import io.github.bubba8587.bubbas_reworked_mobs.client.creeper.CreeperSkinState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries a creeper's biome skin and carried pillar block from extract to render. */
@Mixin(CreeperRenderState.class)
abstract class CreeperRenderStateSkinMixin implements CreeperSkinState, CreeperCarryState {
	@Unique
	private final BlockModelRenderState brm$carried = new BlockModelRenderState();

	@Unique
	private @Nullable Identifier brm$skinTexture;

	@Override
	public BlockModelRenderState brm$carried() {
		return brm$carried;
	}

	@Override
	public @Nullable Identifier brm$skinTexture() {
		return brm$skinTexture;
	}

	@Override
	public void brm$setSkinTexture(@Nullable Identifier texture) {
		brm$skinTexture = texture;
	}
}

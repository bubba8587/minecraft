// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.creeper;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** A creeper render state's biome skin texture (null: vanilla). */
public interface CreeperSkinState {
	@Nullable
	Identifier brm$skinTexture();

	void brm$setSkinTexture(@Nullable Identifier texture);
}

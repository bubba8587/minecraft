// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.client;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;

/** On-body flame sprites for Soulfire burning: vanilla's soul fire sprites in the block atlas (stand-ins for ModelBakery.FIRE_0 / FIRE_1). */
public final class SoulFlames {
	public static final SpriteId FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_0");
	public static final SpriteId FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_1");

	private SoulFlames() {
	}
}

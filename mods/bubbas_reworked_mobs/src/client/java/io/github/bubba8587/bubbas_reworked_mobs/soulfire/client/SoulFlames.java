// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.client;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;

/** The flame sprites drawn on a soul-burning entity, in place of vanilla's fire_0 / fire_1 (placeholder: vanilla soul fire). */
public final class SoulFlames {
	public static final SpriteId FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_0");
	public static final SpriteId FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_1");

	private SoulFlames() {
	}
}

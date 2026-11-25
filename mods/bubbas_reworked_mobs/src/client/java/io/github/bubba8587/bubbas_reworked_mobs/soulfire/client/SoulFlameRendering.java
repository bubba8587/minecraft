// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.client;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/** Soul-burning on the render state: set at extraction, read where the flames are drawn. */
public final class SoulFlameRendering {
	public static final RenderStateDataKey<Boolean> SOUL_BURNING = RenderStateDataKey.create(() -> "bubbas_reworked_mobs:soul_burning");

	private SoulFlameRendering() {
	}
}

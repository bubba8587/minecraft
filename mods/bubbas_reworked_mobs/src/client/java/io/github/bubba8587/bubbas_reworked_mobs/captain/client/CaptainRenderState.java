// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

/** Wither Captain render state: the summoning blend, 0 (normal) to 1 (summoning). */
public class CaptainRenderState extends SkeletonRenderState {
	public float summoning;
	public boolean isSummoning;
}

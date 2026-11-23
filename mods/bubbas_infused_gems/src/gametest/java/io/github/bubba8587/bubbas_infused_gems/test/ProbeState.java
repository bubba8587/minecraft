// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.entity.Entity;

/** State for the ProjectileProbe mixin. */
public final class ProbeState {
	public static Entity watched;
	public static final Map<Integer, Integer> CALLS = new HashMap<>();

	private ProbeState() {
	}
}

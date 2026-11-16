// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import net.minecraft.world.entity.monster.Creeper;

/** Implemented on every vanilla {@link Creeper} by {@code CreeperMixin}: its {@link CreeperMind}. */
public interface CreeperAccess {
	CreeperMind brm$mind();

	static CreeperMind mind(Creeper creeper) {
		return ((CreeperAccess) creeper).brm$mind();
	}
}

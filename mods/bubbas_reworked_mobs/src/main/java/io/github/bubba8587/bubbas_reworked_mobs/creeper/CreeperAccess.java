// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import net.minecraft.world.entity.monster.Creeper;

/** Implemented on every vanilla {@link Creeper} by {@code CreeperMixin}: its {@link CreeperMind}. */
public interface CreeperAccess {
	CreeperMind brm$mind();

	/** Gives this creeper back vanilla's hunting goals and drops the watcher's (spawner creepers; saved). */
	void brm$makeVanilla();

	boolean brm$isVanilla();

	static CreeperMind mind(Creeper creeper) {
		return ((CreeperAccess) creeper).brm$mind();
	}
}

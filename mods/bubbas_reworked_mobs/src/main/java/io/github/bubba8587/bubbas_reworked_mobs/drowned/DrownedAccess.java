// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import net.minecraft.world.entity.monster.zombie.Drowned;

/** Implemented on every vanilla {@link Drowned} by {@code DrownedMixin}: its {@link DrownedMind}. */
public interface DrownedAccess {
	DrownedMind brm$mind();

	static DrownedMind mind(Drowned drowned) {
		return ((DrownedAccess) drowned).brm$mind();
	}
}

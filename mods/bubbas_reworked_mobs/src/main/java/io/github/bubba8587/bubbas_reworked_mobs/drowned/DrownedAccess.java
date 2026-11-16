// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import net.minecraft.world.entity.monster.zombie.Drowned;

/** Implemented on every vanilla {@link Drowned} by {@code DrownedMixin}: its {@link DrownedMind}. */
public interface DrownedAccess {
	DrownedMind brm$mind();

	/**
	 * Turns the rework off for this drowned for good (saved as {@code bubbas_reworked_mobs:vanilla}): vanilla goals,
	 * targeting, attributes and equipment. Spawner and trial spawner drowned (owner, 2026-11-16: "for now").
	 */
	void brm$makeVanilla();

	boolean brm$isVanilla();

	static DrownedMind mind(Drowned drowned) {
		return ((DrownedAccess) drowned).brm$mind();
	}
}

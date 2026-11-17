// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.api;

import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The small public hook other mods use for the Ancient Fabricators (specs/mob-factory.md), like {@link Endermen}.
 * Bubba's Totems & Artifacts asks it so a ward leaves the Fabricator's deactivated mobs alone (owner, 2026-11-16: a
 * ward over a Fabricator stops its spawning, not its teleporter, so the limp mobs already there still get sent up).
 * Wards stop the dripper through {@link Endermen#keepOut}, which every ward already calls.
 *
 * <p>Stable: keep these signatures; add, don't change.
 */
public final class Fabricators {
	private Fabricators() {
	}

	/** Whether this is a Fabricator mob still deactivated (limp, no AI, waiting to be teleported). */
	public static boolean isDeactivated(Entity entity) {
		return entity instanceof LivingEntity living && FabricatorMobs.isDeactivated(living);
	}
}

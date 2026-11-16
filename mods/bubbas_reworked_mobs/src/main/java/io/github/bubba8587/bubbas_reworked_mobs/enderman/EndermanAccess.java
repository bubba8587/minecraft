// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import org.jspecify.annotations.Nullable;

/**
 * Added to every enderman by mixin/EndermanMixin. The mind is null for endermen that stay vanilla: outside the
 * overworld, and vanilla's random natural spawns (owner, 2026-11-16: they keep vanilla behavior; saved with them).
 */
public interface EndermanAccess {
	@Nullable EndermanMind brm$enderMind();

	/** Gives this enderman back vanilla's block goals and drops its caretaker mind (for good: saved). */
	void brm$makeVanilla();

	/** Marks an enderman the cairn spawner is about to finalize, so its NATURAL spawn reason doesn't make it vanilla. */
	void brm$markCairnSpawn();

	boolean brm$isCairnSpawn();
}

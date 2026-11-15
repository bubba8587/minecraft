// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

/** Added to LivingEntity by a mixin: the wearer's combined armor gems and a little per-entity state. */
public interface GemEntity {
	GemTotals bubbas$wornGems();

	void bubbas$setWornGems(GemTotals totals);

	/** Game time when this entity's weapon Healing/Harming strike is charged again. */
	long bubbas$strikeReadyAt();

	void bubbas$setStrikeReadyAt(long time);

	/** Entity tick count of the last damage taken (Absorption gems refill 30 s after it). */
	int bubbas$lastHurtTick();

	void bubbas$setLastHurtTick(int tick);
}

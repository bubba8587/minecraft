// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

/** Added to LivingEntity by a mixin: the wearer's combined armor gems and a little per-entity state. */
public interface GemEntity {
	GemTotals bubbas$wornGems();

	void bubbas$setWornGems(GemTotals totals);

	/** Entity tick count of the last damage taken (Absorption gems refill 30 s after it). */
	int bubbas$lastHurtTick();

	void bubbas$setLastHurtTick(int tick);

	/** Entity tick count of the last time equipping Absorption gems filled the shield at once. */
	int bubbas$lastEquipFillTick();

	/** Saturation gems: food restored so far that has not made a whole point yet. */
	float bubbas$foodCarry();

	void bubbas$setFoodCarry(float carry);

	void bubbas$setLastEquipFillTick(int tick);
}

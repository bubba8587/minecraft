// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

/** Added to LivingEntity by a mixin. */
public interface GemEntity {
	GemTotals bubbas$wornGems();

	void bubbas$setWornGems(GemTotals totals);

	/** Entity tick count of the last damage taken. */
	int bubbas$lastHurtTick();

	void bubbas$setLastHurtTick(int tick);

	/** Saturation gems: food restored so far that has not made a whole point yet. */
	float bubbas$foodCarry();

	void bubbas$setFoodCarry(float carry);
}

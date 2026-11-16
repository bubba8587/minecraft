// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.talisman;

import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Grave Talisman (specs/undead.md "Grave Talisman (v0)"; moved here, specs/totems-artifacts.md): glows faintly when undead are near, brighter the closer
 * they are. No gameplay effect. The client works it out for its own player (client/talisman/GraveTalismanGlow); the
 * search is here, on plain {@link Level}, so server game tests can check it on a real level.
 */
public final class GraveTalisman {
	// Starting values, owner to tune.
	/** How far the talisman senses undead (blocks, straight-line distance from the holder's eyes). */
	public static final double RANGE = 40.0;
	/** Closest undead within this distance: brightest (3). */
	public static final double BRIGHTEST_WITHIN = 12.0;
	/** ... within this: bright (2); anything else in RANGE: faint (1). */
	public static final double BRIGHT_WITHIN = 24.0;
	/** The client searches this often (ticks). */
	public static final int SEARCH_INTERVAL_TICKS = 5;

	public static final int OFF = 0;
	public static final int FAINT = 1;
	public static final int BRIGHT = 2;
	public static final int BRIGHTEST = 3;

	private GraveTalisman() {
	}

	/** Glow level (OFF..BRIGHTEST) for the squared distance to the closest undead ({@code Double.MAX_VALUE}: none). */
	public static int glowLevel(double distanceSqr) {
		if (distanceSqr <= BRIGHTEST_WITHIN * BRIGHTEST_WITHIN) {
			return BRIGHTEST;
		}
		if (distanceSqr <= BRIGHT_WITHIN * BRIGHT_WITHIN) {
			return BRIGHT;
		}
		return distanceSqr <= RANGE * RANGE ? FAINT : OFF;
	}

	/**
	 * Squared distance from {@code centre} to the closest undead within {@code range}: living entities in
	 * {@code #minecraft:undead}, and with Bubba's Reworked Mobs installed its sleepers too (block entities in chunks
	 * already loaded; never loads one), through compat/SleeperHook.
	 *
	 * @return the squared distance, or {@code Double.MAX_VALUE} when there is none
	 */
	public static double nearestUndeadSqr(Level level, Vec3 centre, double range) {
		double rangeSqr = range * range;
		double best = SleeperHook.get().nearestSleeperSqr(level, centre, range);
		for (Entity entity : level.getEntities((Entity) null, new AABB(centre, centre).inflate(range),
				e -> e.isAlive() && e.is(EntityTypeTags.UNDEAD))) {
			double d = centre.distanceToSqr(entity.getBoundingBox().getCenter());
			if (d <= rangeSqr && d < best) {
				best = d;
			}
		}
		return best;
	}

	/** The glow level a talisman held at {@code centre} shows right now. */
	public static int glowAt(Level level, Vec3 centre) {
		return glowLevel(nearestUndeadSqr(level, centre, RANGE));
	}
}

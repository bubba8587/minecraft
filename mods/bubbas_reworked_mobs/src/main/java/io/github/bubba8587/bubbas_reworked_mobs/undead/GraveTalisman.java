// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Grave Talisman (specs/undead.md "Grave Talisman (v0)"): glows faintly when undead are near, brighter the closer
 * they are. No gameplay effect. The client works it out for its own player (client/undead/GraveTalismanGlow); the
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
	 * Squared distance from {@code centre} to the closest undead within {@code range}: sleeper blocks (block entities in
	 * chunks already loaded; never loads one) and living entities in {@code #minecraft:undead}.
	 *
	 * @return the squared distance, or {@code Double.MAX_VALUE} when there is none
	 */
	public static double nearestUndeadSqr(Level level, Vec3 centre, double range) {
		double best = Double.MAX_VALUE;
		double rangeSqr = range * range;
		int minX = SectionPos.blockToSectionCoord(centre.x - range);
		int maxX = SectionPos.blockToSectionCoord(centre.x + range);
		int minZ = SectionPos.blockToSectionCoord(centre.z - range);
		int maxZ = SectionPos.blockToSectionCoord(centre.z + range);
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof SleeperBlockEntity && !entity.isRemoved()) {
						BlockPos pos = entity.getBlockPos();
						double d = centre.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
						if (d <= rangeSqr && d < best) {
							best = d;
						}
					}
				}
			}
		}
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

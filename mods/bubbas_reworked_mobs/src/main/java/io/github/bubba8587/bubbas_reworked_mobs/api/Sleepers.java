// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.api;

import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleepAreas;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The small public hook other mods use to reach Reworked Mobs' sleepers (dormant undead blocks), the same way Reworked
 * Beacons offers its payload API. Bubba's Totems & Artifacts uses it, only when this mod is installed: the Totem of
 * Warding calms and keeps sleepers asleep, the Grave Talisman senses them. Nothing here depends on that mod.
 *
 * <p>Stable: keep these signatures; add, don't change. Server-side calls are server thread only.
 */
public final class Sleepers {
	private Sleepers() {
	}

	/**
	 * Takes {@code points} off the disturbance meter of every sleeper within {@code radius} of {@code centre} (a sleeper
	 * wakes at 100; the meter never goes below 0). Loaded chunks only.
	 *
	 * @return how many sleepers were in range
	 */
	public static int calm(ServerLevel level, Vec3 centre, double radius, float points) {
		long now = level.getGameTime();
		int count = 0;
		for (SleeperBlockEntity sleeper : SleeperFinder.within(level, centre, radius)) {
			sleeper.calm(now, points);
			count++;
		}
		return count;
	}

	/**
	 * Keeps every sleeper inside {@code area} asleep (noise counts for nothing) for {@code lifetimeTicks}. {@code source}
	 * is any key of the caller's (a beacon's position); calling again replaces and refreshes that source's area, so a
	 * caller that stops refreshing needs no cleanup.
	 */
	public static void keepAsleep(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		SleepAreas.put(level, source, area, lifetimeTicks);
	}

	/** Ends {@code source}'s keep-asleep area now. */
	public static void release(ServerLevel level, Object source) {
		SleepAreas.remove(level, source);
	}

	/** Whether a sleeper at {@code pos} is being kept asleep by some caller's area. */
	public static boolean isKeptAsleep(Level level, BlockPos pos) {
		return SleepAreas.isKeptAsleep(level, pos);
	}

	/** Whether there is a sleeper (a dormant undead) at {@code pos}. Works on client and server levels. */
	public static boolean isSleeper(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper && !sleeper.isRemoved();
	}

	/**
	 * Squared distance from {@code centre} to the nearest sleeper's block centre within {@code range}, from chunks
	 * already loaded (never loads one). Works on client and server levels.
	 *
	 * @return the squared distance, or {@code Double.MAX_VALUE} when there is none
	 */
	public static double nearestSqr(Level level, Vec3 centre, double range) {
		double best = Double.MAX_VALUE;
		for (SleeperBlockEntity sleeper : SleeperFinder.within(level, centre, range)) {
			best = Math.min(best, centre.distanceToSqr(Vec3.atCenterOf(sleeper.getBlockPos())));
		}
		return best;
	}
}

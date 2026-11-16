// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import io.github.bubba8587.bubbas_reworked_mobs.api.Endermen;
import io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@link SleeperHook} through Bubba's Reworked Mobs' api/Sleepers. The only class that touches that mod; created only
 * when it is installed (SleeperHook.Holder checks), so it is never loaded otherwise.
 */
final class ReworkedMobsSleepers implements SleeperHook {
	@Override
	public void calm(ServerLevel level, Vec3 centre, double radius, float points) {
		Sleepers.calm(level, centre, radius, points);
	}

	@Override
	public void keepAsleep(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		Sleepers.keepAsleep(level, source, area, lifetimeTicks);
	}

	@Override
	public void release(ServerLevel level, Object source) {
		Sleepers.release(level, source);
	}

	@Override
	public void keepEndermenOut(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		try {
			Endermen.keepOut(level, source, area, lifetimeTicks);
		} catch (LinkageError e) {
			// An older Reworked Mobs without api/Endermen: nothing to keep out.
		}
	}

	@Override
	public void releaseEndermen(ServerLevel level, Object source) {
		try {
			Endermen.release(level, source);
		} catch (LinkageError e) {
			// As above.
		}
	}

	@Override
	public double nearestSleeperSqr(Level level, Vec3 centre, double range) {
		return Sleepers.nearestSqr(level, centre, range);
	}
}

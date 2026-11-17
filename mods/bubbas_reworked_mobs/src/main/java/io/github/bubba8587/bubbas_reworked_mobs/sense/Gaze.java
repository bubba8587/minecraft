// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.sense;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "Is it looking at me?" for the reworked mobs (owner, 2026-11-16: look-at detection is a cone, not a direct line).
 * A watcher looks at a target when its view direction is within a cone of any of several points on the target (so the
 * edge of a body counts, not just its middle), and, if sight is required, one of those points inside the cone has a
 * clear collision line from the watcher's eyes (fluids don't block it: you can see a head at the waterline).
 * Cost: one dot product per point; a raycast only for points inside the cone, stopping at the first clear one.
 */
public final class Gaze {
	private Gaze() {
	}

	/** Whether {@code watcher} looks within {@code halfAngleDegrees} of any of {@code points} (with clear sight if {@code needSight}). */
	public static boolean looksAt(Level level, LivingEntity watcher, double halfAngleDegrees, boolean needSight, Vec3... points) {
		Vec3 eye = watcher.getEyePosition();
		Vec3 view = watcher.getViewVector(1.0F);
		double cone = Math.cos(Math.toRadians(halfAngleDegrees));
		for (Vec3 point : points) {
			Vec3 to = point.subtract(eye);
			double length = to.length();
			if (length < 1.0E-3) {
				return true;
			}
			if (view.dot(to) / length < cone) {
				continue;
			}
			if (!needSight || level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, watcher))
					.getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	/** Points over a body standing at {@code feet}: its eyes, middle and the left and right edges at eye height, as seen from {@code from}. */
	public static Vec3[] bodyPoints(Vec3 feet, double width, double height, double eyeHeight, Vec3 from) {
		double dx = feet.x - from.x;
		double dz = feet.z - from.z;
		double length = Math.sqrt(dx * dx + dz * dz);
		double half = width / 2.0;
		double px = length < 1.0E-4 ? half : -dz / length * half;
		double pz = length < 1.0E-4 ? 0.0 : dx / length * half;
		return new Vec3[] {
				feet.add(0.0, eyeHeight, 0.0),
				feet.add(0.0, height * 0.5, 0.0),
				feet.add(px, eyeHeight, pz),
				feet.add(-px, eyeHeight, -pz),
				feet.add(0.0, height, 0.0),
		};
	}
}

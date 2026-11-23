// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.warding;

import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_totems_artifacts.compat.SleeperHook;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The edge of a ward for everything path-blocking can't stop. */
final class WardBarrier {
	/** Outward speed added each tick to a kept-out mob inside a ward (blocks per tick). */
	static final double NUDGE = 0.06;
	/** How far outside the edge a pushed-back steered vehicle ends up. */
	static final double EDGE_MARGIN = 0.6;

	/** Where a kept-out mob near the edge last stood outside the ward, and when. */
	private record Seen(Vec3 pos, long time) {
	}

	private static final Map<Mob, Seen> LAST_OUTSIDE = new WeakHashMap<>();

	private WardBarrier() {
	}

	/** Monsters (all treated the same) and happy ghasts are kept out of wards. */
	static boolean keptOut(Entity entity) {
		return (entity instanceof Enemy || entity.getType() == EntityTypes.HAPPY_GHAST) && !SleeperHook.get().wardIgnores(entity);
	}

	/** After a kept-out mob's own tick (it has moved). */
	static void afterMove(Mob mob, AABB area, long now) {
		if (!mob.isAlive() || mob.isPassenger()) {
			return;
		}
		if (mob.getControllingPassenger() instanceof ServerPlayer rider) {
			pushSteered(mob, rider, area);
		} else {
			pushBack(mob, area, now);
		}
	}

	private static void pushBack(Mob mob, AABB area, long now) {
		boolean inside = area.contains(mob.position());
		Vec3 normal = exitNormal(area, mob.position(), mob.isNoGravity());
		Vec3 motion = mob.getDeltaMovement();
		double inward = motion.dot(normal);
		if (!inside) {
			LAST_OUTSIDE.put(mob, new Seen(mob.position(), now));
			if (inward >= 0) {
				return;
			}
		}
		if (inward < 0) {
			motion = motion.subtract(normal.scale(inward));
		}
		if (inside) {
			Seen last = LAST_OUTSIDE.get(mob);
			if (last != null && last.time >= now - 1) {
				Vec3 at = mob.position().add(normal.scale(depth(area, mob.position(), normal) + 0.01));
				mob.setPos(at.x, at.y, at.z);
				LAST_OUTSIDE.put(mob, new Seen(at, now));
			} else if (!(mob instanceof PathfinderMob walker && WardFleeGoal.isTrapped(walker))) {
				motion = motion.add(normal.scale(NUDGE));
			}
		}
		mob.setDeltaMovement(motion);
		mob.needsSync = true;
	}

	private static void pushSteered(Mob vehicle, ServerPlayer rider, AABB area) {
		Vec3 pos = vehicle.position();
		if (!area.contains(pos)) {
			return;
		}
		Vec3 normal = exitNormal(area, pos, true);
		double depth = depth(area, pos, normal);
		Vec3 to = pos.add(normal.scale(Math.min(depth + EDGE_MARGIN, 1.0)));
		vehicle.setPos(to.x, to.y, to.z);
		Vec3 motion = vehicle.getDeltaMovement();
		double inward = motion.dot(normal);
		if (inward < 0) {
			vehicle.setDeltaMovement(motion.subtract(normal.scale(inward)));
		}
		rider.connection.send(ClientboundMoveVehiclePacket.fromEntity(vehicle));
	}

	/** The unit vector out of the box through its nearest face. */
	static Vec3 exitNormal(AABB box, Vec3 from, boolean vertical) {
		double[] d = {from.x - box.minX, box.maxX - from.x, from.z - box.minZ, box.maxZ - from.z,
				vertical ? from.y - box.minY : Double.MAX_VALUE, vertical ? box.maxY - from.y : Double.MAX_VALUE};
		int best = 0;
		for (int i = 1; i < d.length; i++) {
			if (d[i] < d[best]) {
				best = i;
			}
		}
		return switch (best) {
			case 0 -> new Vec3(-1, 0, 0);
			case 1 -> new Vec3(1, 0, 0);
			case 2 -> new Vec3(0, 0, -1);
			case 3 -> new Vec3(0, 0, 1);
			case 4 -> new Vec3(0, -1, 0);
			default -> new Vec3(0, 1, 0);
		};
	}

	/** How far inside the box from is, measured along the exit normal. */
	private static double depth(AABB box, Vec3 from, Vec3 normal) {
		if (normal.x < 0) {
			return from.x - box.minX;
		}
		if (normal.x > 0) {
			return box.maxX - from.x;
		}
		if (normal.z < 0) {
			return from.z - box.minZ;
		}
		if (normal.z > 0) {
			return box.maxZ - from.z;
		}
		return normal.y < 0 ? from.y - box.minY : box.maxY - from.y;
	}
}

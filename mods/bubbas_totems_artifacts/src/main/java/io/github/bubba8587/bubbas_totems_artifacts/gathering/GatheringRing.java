// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.gathering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** The Totem of Gathering in a beacon. */
public final class GatheringRing {
	private static final class Ring {
		final BlockPos beacon;
		AABB area;
		long expiresAt;
		final List<Entity> members = new ArrayList<>();

		Ring(BlockPos beacon) {
			this.beacon = beacon;
		}

		Vec3 centre() {
			return new Vec3(beacon.getX() + 0.5, beacon.getY() + TotemsTuning.RING_HEIGHT, beacon.getZ() + 0.5);
		}
	}

	private static final Map<ServerLevel, Map<BlockPos, Ring>> RINGS = new WeakHashMap<>();

	private GatheringRing() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(GatheringRing::tick);
	}

	/** Start or refresh the ring round a beacon. */
	public static void put(ServerLevel level, BlockPos beacon, AABB area, int lifetimeTicks) {
		Ring ring = RINGS.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(beacon.immutable(), Ring::new);
		ring.area = area;
		ring.expiresAt = level.getGameTime() + lifetimeTicks;
	}

	/** Entities in a beacon's ring. */
	public static List<Entity> members(ServerLevel level, BlockPos beacon) {
		Map<BlockPos, Ring> rings = RINGS.get(level);
		Ring ring = rings == null ? null : rings.get(beacon);
		return ring == null ? List.of() : List.copyOf(ring.members);
	}

	/** Where the ring is at time for member index of count. */
	public static Vec3 slot(BlockPos beacon, long time, int index, int count) {
		double angle = time * (2.0 * Math.PI / TotemsTuning.RING_PERIOD_TICKS) + index * (2.0 * Math.PI / Math.max(1, count));
		double r = TotemsTuning.RING_RADIUS;
		return new Vec3(beacon.getX() + 0.5 + Math.cos(angle) * r, beacon.getY() + TotemsTuning.RING_HEIGHT, beacon.getZ() + 0.5 + Math.sin(angle) * r);
	}

	private static void tick(ServerLevel level) {
		Map<BlockPos, Ring> rings = RINGS.get(level);
		if (rings == null || rings.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		Set<Entity> taken = new HashSet<>();
		for (Ring ring : rings.values()) {
			taken.addAll(ring.members);
		}
		for (Iterator<Ring> it = rings.values().iterator(); it.hasNext(); ) {
			Ring ring = it.next();
			if (ring.expiresAt < now) {
				release(ring);
				it.remove();
				continue;
			}
			ring.members.removeIf(e -> !e.isAlive());
			if ((now + ring.beacon.hashCode()) % TotemsTuning.RING_COLLECT_TICKS == 0) {
				collect(level, ring, taken);
			}
			steer(level, ring, now);
		}
	}

	private static void collect(ServerLevel level, Ring ring, Set<Entity> taken) {
		for (Entity e : level.getEntities((Entity) null, ring.area, e -> !taken.contains(e)
				&& (e instanceof ItemEntity item && Gathering.pullable(item, null) || e instanceof ExperienceOrb && EntitySelector.ENTITY_STILL_ALIVE.test(e)))) {
			if (ring.members.size() >= TotemsTuning.RING_MAX_MEMBERS) {
				return;
			}
			ring.members.add(e);
			taken.add(e);
			e.setNoGravity(true);
			if (e instanceof ItemEntity item) {
				item.setUnlimitedLifetime();
			}
		}
	}

	private static void steer(ServerLevel level, Ring ring, long now) {
		int count = ring.members.size();
		Vec3 centre = ring.centre();
		for (int i = 0; i < count; i++) {
			Entity e = ring.members.get(i);
			if (e instanceof ExperienceOrb && level.getNearestPlayer(e.getX(), e.getY(), e.getZ(), TotemsTuning.RING_XP_RELEASE_DISTANCE, EntitySelector.NO_SPECTATORS) instanceof Player) {
				e.setNoGravity(false);
				ring.members.remove(i);
				i--;
				count--;
				continue;
			}
			Vec3 target = slot(ring.beacon, now + 1, i, count);
			Vec3 to = target.subtract(e.position());
			double distance = to.length();
			Vec3 velocity = distance > TotemsTuning.RING_FLY_SPEED ? to.scale(TotemsTuning.RING_FLY_SPEED / distance) : to;
			e.setDeltaMovement(velocity);
			e.needsSync = true;
			if (distance > 2.0 && now % 4 == 0) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 0.15, e.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
			}
		}
		if (now % 20 == 0 && count > 0) {
			level.sendParticles(ParticleTypes.ENCHANT, centre.x, centre.y, centre.z, 3, TotemsTuning.RING_RADIUS, 0.1, TotemsTuning.RING_RADIUS, 0.0);
		}
	}

	private static void release(Ring ring) {
		for (Entity e : ring.members) {
			if (e.isAlive()) {
				e.setNoGravity(false);
			}
		}
		ring.members.clear();
	}
}

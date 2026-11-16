// dte:C6,B17,B18
package io.github.bubba8587.bubbas_totems_artifacts.gathering;

import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.mixin.ItemEntityAccessor;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Totem of Gathering (specs/totems-artifacts.md).
 *
 * <p><b>Held</b> ({@link #pull}, every tick): dropped items within {@link TotemsTuning#GATHER_RADIUS} fly to the
 * holder exactly the way vanilla's XP orbs fly to a player (same pull, ExperienceOrb.followNearbyPlayer, B17), and are
 * picked up by the usual rules. XP orbs already do that by themselves in vanilla within the same 8 blocks, so the
 * totem leaves them to it. Items the holder just threw (still on their pickup delay) and items that can never be
 * picked up (displays) are left alone. Players and mobs alike (B18).
 *
 * <p><b>In a beacon</b> ({@link #intoChest}, every pulse): dropped items in the beacon's area go into the nearest chest
 * or barrel under the beacon (the 3x3 column below it, down through the pyramid to
 * {@link TotemsTuning#GATHER_CHEST_DEPTH_BELOW_PYRAMID} blocks below its bottom layer; nearest first), as a hopper would
 * put them in, and XP orbs are gathered onto the beacon, where they wait for someone to collect them. No chest or
 * barrel: nothing happens.
 */
public final class Gathering {
	private Gathering() {
	}

	/** Whether {@code item} may be pulled toward {@code holder} (null: by a beacon). */
	static boolean pullable(ItemEntity item, @Nullable LivingEntity holder) {
		if (!item.isAlive() || item.getItem().isEmpty()) {
			return false;
		}
		int delay = ((ItemEntityAccessor) item).bta$pickupDelay();
		if (delay >= 32767) {
			return false; // never picked up (displays, props)
		}
		// The holder's own throw comes back only once it could be picked up again.
		return holder == null || delay <= 0 || item.getOwner() != holder;
	}

	/** One tick of a held totem: nearby items fly toward the holder. Returns how many were pulled. */
	public static int pull(ServerLevel level, LivingEntity holder) {
		double r = TotemsTuning.GATHER_RADIUS;
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, holder.getBoundingBox().inflate(r),
				item -> item.distanceToSqr(holder) <= r * r && pullable(item, holder));
		int pulled = 0;
		for (ItemEntity item : items) {
			if (pulled++ >= TotemsTuning.GATHER_MAX_ITEMS_PER_TICK) {
				break;
			}
			// Vanilla's XP orb pull, verbatim.
			Vec3 delta = new Vec3(holder.getX() - item.getX(), holder.getY() + holder.getEyeHeight() / 2.0 - item.getY(), holder.getZ() - item.getZ());
			double power = 1.0 - Math.sqrt(delta.lengthSqr()) / r;
			if (power > 0.0 && delta.lengthSqr() > 1.0E-4) {
				item.setDeltaMovement(item.getDeltaMovement().add(delta.normalize().scale(power * power * 0.1)));
				item.needsSync = true; // items only sync motion now and then; this one is steering
			}
		}
		return Math.min(pulled, TotemsTuning.GATHER_MAX_ITEMS_PER_TICK);
	}

	/** The nearest chest or barrel under the beacon at {@code beacon} with a pyramid of {@code levels}, or null. */
	public static @Nullable BlockPos chestUnder(ServerLevel level, BlockPos beacon, int levels) {
		int depth = Math.max(1, levels) + TotemsTuning.GATHER_CHEST_DEPTH_BELOW_PYRAMID;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dy = 1; dy <= depth; dy++) {
			// Straight below first, then the sides, then the corners.
			for (int ring = 0; ring <= 2; ring++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (Math.abs(dx) + Math.abs(dz) != ring) {
							continue;
						}
						pos.set(beacon.getX() + dx, beacon.getY() - dy, beacon.getZ() + dz);
						BlockState state = level.getBlockState(pos);
						if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
							return pos.immutable();
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * One beacon pulse: items in {@code area} into the chest or barrel under the beacon, XP orbs onto the beacon.
	 * Returns how many item entities were (at least partly) stored.
	 */
	public static int intoChest(ServerLevel level, BlockPos beacon, int levels, AABB area) {
		BlockPos chestPos = chestUnder(level, beacon, levels);
		if (chestPos == null) {
			return 0;
		}
		Container chest = HopperBlockEntity.getContainerAt(level, chestPos);
		if (chest == null) {
			return 0;
		}
		int stored = 0;
		int seen = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, item -> pullable(item, null))) {
			if (seen++ >= TotemsTuning.GATHER_BEACON_MAX_PER_PULSE) {
				break;
			}
			int before = item.getItem().getCount();
			Vec3 from = item.position();
			HopperBlockEntity.addItem(chest, item);
			if (!item.isAlive() || item.getItem().getCount() < before) {
				stored++;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.2, from.z, 6, 0.15, 0.15, 0.15, 0.02);
			}
		}
		Vec3 onBeacon = Vec3.atBottomCenterOf(beacon.above());
		int orbs = 0;
		for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
			if (orbs++ >= TotemsTuning.GATHER_BEACON_MAX_PER_PULSE) {
				break;
			}
			if (orb.distanceToSqr(onBeacon) > 1.0) {
				orb.teleportTo(onBeacon.x, onBeacon.y + 0.1, onBeacon.z);
				orb.setDeltaMovement(Vec3.ZERO);
			}
		}
		if (stored > 0) {
			level.playSound(null, chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 0.8F);
		}
		return stored;
	}
}

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
 * holder at {@link TotemsTuning#GATHER_SPEED} blocks a tick, aimed like vanilla's XP orbs (at the holder's middle), and
 * are picked up by the usual rules. (Vanilla's orb pull itself was tried first, B17: on the ground an item's friction
 * eats it, and items 6 blocks out moved under a block in 5 s.) XP orbs already fly to a player by themselves in vanilla
 * within the same 8 blocks, so the totem leaves them to it. Items the holder just threw (still on their pickup delay) and items that can never be
 * picked up (displays) are left alone. Players and mobs alike (B18).
 *
 * <p><b>In a beacon:</b> {@link GatheringRing}.
 *
 * <p><b>As a statue:</b> {@link #intoChest}.
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
			// Aimed like an XP orb (the holder's middle); a steady speed, slowing in the last block so it doesn't overshoot.
			Vec3 delta = new Vec3(holder.getX() - item.getX(), holder.getY() + holder.getEyeHeight() / 2.0 - item.getY(), holder.getZ() - item.getZ());
			double distance = delta.length();
			if (distance > 1.0E-3) {
				double speed = TotemsTuning.GATHER_SPEED * Math.min(1.0, distance);
				// Plus gravity's pull back, so the item glides instead of scraping along the ground.
				item.setDeltaMovement(delta.scale(speed / distance).add(0.0, 0.04, 0.0));
				item.needsSync = true; // items only sync motion now and then; this one is steering
			}
		}
		return Math.min(pulled, TotemsTuning.GATHER_MAX_ITEMS_PER_TICK);
	}

	/** A chest or barrel touching pos, or null. */
	public static @Nullable BlockPos chestBeside(ServerLevel level, BlockPos pos) {
		for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[] {net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.UP}) {
			BlockPos at = pos.relative(d);
			BlockState state = level.getBlockState(at);
			if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
				return at;
			}
		}
		return null;
	}

	/** Gathering statue: nearby drops into a touching chest or barrel; returns how many were stored. */
	public static int intoChest(ServerLevel level, BlockPos statue) {
		BlockPos chestPos = chestBeside(level, statue);
		if (chestPos == null) {
			return 0;
		}
		Container chest = HopperBlockEntity.getContainerAt(level, chestPos);
		if (chest == null) {
			return 0;
		}
		AABB area = new AABB(statue).inflate(TotemsTuning.GATHER_STATUE_RADIUS);
		int stored = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, item -> pullable(item, null))) {
			int before = item.getItem().getCount();
			Vec3 from = item.position();
			HopperBlockEntity.addItem(chest, item);
			if (!item.isAlive() || item.getItem().getCount() < before) {
				stored++;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.2, from.z, 6, 0.15, 0.15, 0.15, 0.02);
			}
		}
		if (stored > 0) {
			level.playSound(null, chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 0.8F);
		}
		return stored;
	}
}

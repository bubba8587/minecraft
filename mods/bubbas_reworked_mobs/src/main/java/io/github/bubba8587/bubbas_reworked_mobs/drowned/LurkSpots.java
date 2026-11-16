// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Block-level searches for the drowned (specs/drowned.md): lurk spots (2-deep water by a shore) and the deepest water
 * nearby (where it drags a victim). Bounded: a fixed number of columns, a fixed scan height each.
 */
public final class LurkSpots {
	private LurkSpots() {
	}

	/**
	 * A lurk spot: the water column whose floor block is {@code floor} (the drowned stands on top of it), the water's
	 * surface height, and the yaw that faces the nearest shore.
	 */
	public record Spot(BlockPos floor, double surfaceY, float shoreYaw) {
		/** Where its feet go when standing on the floor. */
		public double floorTop() {
			return floor.getY() + 1.0;
		}

		public Vec3 centre() {
			return new Vec3(floor.getX() + 0.5, floorTop(), floor.getZ() + 0.5);
		}

		public double horizontalDistanceSqr(Entity entity) {
			double dx = entity.getX() - (floor.getX() + 0.5);
			double dz = entity.getZ() - (floor.getZ() + 0.5);
			return dx * dx + dz * dz;
		}
	}

	/**
	 * Up to {@link DrownedTuning#SPOT_SEARCH_COLUMNS} random columns around {@code around} (a player), between the
	 * min and max player distance and within travel range of the drowned; the best is the one nearest to 6 blocks from
	 * the player, then nearest the drowned. {@code avoid} (a spot it just left) and anything within 3 blocks of it is
	 * skipped.
	 */
	public static @Nullable Spot find(Level level, Entity drowned, Entity around, @Nullable BlockPos avoid, RandomSource random) {
		Spot best = null;
		double bestScore = Double.MAX_VALUE;
		double minSqr = DrownedTuning.SPOT_MIN_PLAYER_DISTANCE * DrownedTuning.SPOT_MIN_PLAYER_DISTANCE;
		double maxTravelSqr = DrownedTuning.SPOT_MAX_TRAVEL * DrownedTuning.SPOT_MAX_TRAVEL;
		BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
		for (int i = 0; i < DrownedTuning.SPOT_SEARCH_COLUMNS; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = DrownedTuning.SPOT_MIN_PLAYER_DISTANCE
					+ random.nextDouble() * (DrownedTuning.SPOT_MAX_PLAYER_DISTANCE - DrownedTuning.SPOT_MIN_PLAYER_DISTANCE);
			int x = Mth.floor(around.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(around.getZ() + Math.sin(angle) * distance);
			column.set(x, Mth.floor(around.getY()), z);
			Spot spot = at(level, column);
			if (spot == null) {
				continue;
			}
			if (avoid != null && spot.floor().distSqr(avoid) <= 9.0) {
				continue;
			}
			double toPlayer = spot.horizontalDistanceSqr(around);
			double toDrowned = spot.centre().distanceToSqr(drowned.position());
			if (toPlayer < minSqr || toDrowned > maxTravelSqr) {
				continue;
			}
			double score = Math.abs(Math.sqrt(toPlayer) - 6.0) * 4.0 + Math.sqrt(toDrowned);
			if (score < bestScore) {
				bestScore = score;
				best = spot;
			}
		}
		return best;
	}

	/** The lurk spot in this column near {@code near}'s height (scanning SPOT_SCAN_UP above to SPOT_SCAN_DOWN below), or null. */
	public static @Nullable Spot at(Level level, BlockPos near) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int top = near.getY() + DrownedTuning.SPOT_SCAN_UP;
		int bottom = near.getY() - DrownedTuning.SPOT_SCAN_DOWN;
		// The surface: the highest water block with no water above it.
		boolean aboveIsWater = isOpenWater(level, pos.set(near.getX(), top + 1, near.getZ()));
		for (int y = top; y >= bottom; y--) {
			boolean water = isOpenWater(level, pos.set(near.getX(), y, near.getZ()));
			if (water && !aboveIsWater) {
				return spotWithSurface(level, pos.immutable());
			}
			aboveIsWater = water;
		}
		return null;
	}

	/** {@code surface} is the top water block. Exactly LURK_DEPTH deep over a solid floor, open air above, a shore near. */
	private static @Nullable Spot spotWithSurface(Level level, BlockPos surface) {
		for (int d = 1; d < DrownedTuning.LURK_DEPTH; d++) {
			if (!isOpenWater(level, surface.below(d))) {
				return null;
			}
		}
		BlockPos floor = surface.below(DrownedTuning.LURK_DEPTH);
		BlockState floorState = level.getBlockState(floor);
		if (!floorState.isFaceSturdy(level, floor, Direction.UP)) {
			return null;
		}
		BlockPos above = surface.above();
		if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty() || !level.getFluidState(above).isEmpty()) {
			return null;
		}
		FluidState fluid = level.getFluidState(surface);
		double surfaceY = surface.getY() + fluid.getHeight(level, surface);
		Float yaw = shoreYaw(level, surface);
		return yaw == null ? null : new Spot(floor, surfaceY, yaw);
	}

	/** The yaw toward the nearest land (level with the surface or one block above it) in 8 directions, or null. */
	private static @Nullable Float shoreYaw(Level level, BlockPos surface) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int bestDistance = Integer.MAX_VALUE;
		Float best = null;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				for (int step = 1; step <= DrownedTuning.SHORE_MAX_DISTANCE && step < bestDistance; step++) {
					int x = surface.getX() + dx * step;
					int z = surface.getZ() + dz * step;
					pos.set(x, surface.getY(), z);
					if (isOpenWater(level, pos)) {
						continue;
					}
					if (standable(level, pos) || standable(level, pos.set(x, surface.getY() + 1, z))) {
						bestDistance = step;
						best = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
					}
					break;
				}
			}
		}
		return best;
	}

	/** A block you can stand on top of: solid at {@code pos}, nothing solid above. */
	private static boolean standable(Level level, BlockPos.MutableBlockPos pos) {
		if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
			return false;
		}
		pos.move(Direction.UP);
		boolean free = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && !level.getFluidState(pos).is(FluidTags.WATER);
		pos.move(Direction.DOWN);
		return free;
	}

	/** Water a body can be in: water fluid, no collision (seagrass and kelp are fine). */
	public static boolean isOpenWater(Level level, BlockPos pos) {
		return level.getFluidState(pos).is(FluidTags.WATER) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/**
	 * The deepest water near {@code from} (columns every DEEP_SEARCH_STEP in a DEEP_SEARCH_RADIUS square whose top
	 * water block is at {@code surfaceBlockY}): the bottom-centre of the deepest, nearest first on ties. Null when
	 * nothing is deeper than {@code currentFloorY} (the lowest water block of the column it is in).
	 */
	public static @Nullable Vec3 deepest(Level level, BlockPos from, int surfaceBlockY, int currentFloorY) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int bestFloor = currentFloorY;
		double bestDistance = Double.MAX_VALUE;
		Vec3 best = null;
		int r = DrownedTuning.DEEP_SEARCH_RADIUS;
		for (int dx = -r; dx <= r; dx += DrownedTuning.DEEP_SEARCH_STEP) {
			for (int dz = -r; dz <= r; dz += DrownedTuning.DEEP_SEARCH_STEP) {
				int x = from.getX() + dx;
				int z = from.getZ() + dz;
				pos.set(x, surfaceBlockY, z);
				if (!isOpenWater(level, pos)) {
					continue;
				}
				int y = surfaceBlockY;
				int limit = surfaceBlockY - DrownedTuning.DEEP_MAX_DEPTH;
				while (y > limit && isOpenWater(level, pos.set(x, y - 1, z))) {
					y--;
				}
				// y is now the lowest water block; the floor is below it.
				double distance = dx * dx + dz * dz;
				if (y < bestFloor || y == bestFloor && best != null && distance < bestDistance) {
					bestFloor = y;
					bestDistance = distance;
					best = new Vec3(x + 0.5, y, z + 0.5);
				}
			}
		}
		return best;
	}
}

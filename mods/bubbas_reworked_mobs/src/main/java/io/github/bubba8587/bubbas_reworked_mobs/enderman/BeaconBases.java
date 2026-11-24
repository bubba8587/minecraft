// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Whether a block is part of a working beacon's pyramid (specs/endermen.md "Beacon bases"). */
public final class BeaconBases {
	/** Vanilla's pyramid depth. */
	private static final int MAX_LAYERS = 4;

	private BeaconBases() {
	}

	/** {@code state} at {@code pos} is in a full pyramid layer under a beacon, or over one (upside-down pairs). */
	public static boolean inWorkingBase(Level level, BlockPos pos, BlockState state) {
		if (!state.is(BlockTags.BEACON_BASE_BLOCKS)) {
			return false;
		}
		return servesBeacon(level, pos, 1) || servesBeacon(level, pos, -1);
	}

	/** A beacon {@code layer} blocks above (dir 1) or below (dir -1) pos whose pyramid is full down to pos's layer. */
	private static boolean servesBeacon(Level level, BlockPos pos, int dir) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int layer = 1; layer <= MAX_LAYERS; layer++) {
			int y = pos.getY() + dir * layer;
			if (y < level.getMinY() || y > level.getMaxY()) {
				return false;
			}
			for (int dx = -layer; dx <= layer; dx++) {
				for (int dz = -layer; dz <= layer; dz++) {
					at.set(pos.getX() + dx, y, pos.getZ() + dz);
					if (level.isLoaded(at) && level.getBlockState(at).is(Blocks.BEACON) && fullTo(level, at, -dir, layer)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Layers 1 to {@code layers} from the beacon at {@code beacon}, going {@code dir}, are all base blocks. */
	private static boolean fullTo(Level level, BlockPos beacon, int dir, int layers) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int step = 1; step <= layers; step++) {
			int y = beacon.getY() + dir * step;
			for (int x = beacon.getX() - step; x <= beacon.getX() + step; x++) {
				for (int z = beacon.getZ() - step; z <= beacon.getZ() + step; z++) {
					at.set(x, y, z);
					if (!level.isLoaded(at) || !level.getBlockState(at).is(BlockTags.BEACON_BASE_BLOCKS)) {
						return false;
					}
				}
			}
		}
		return true;
	}
}

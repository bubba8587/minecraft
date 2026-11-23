// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A camp's shade (owner, 2026-11-22: guaranteed shade spots and an underground room its mobs can go in; they burn
 * otherwise). Taking cover is vanilla's own behaviour (skeletons' sun goals, given to camp zombies too:
 * mixin/CampMemberMixin); this only answers where the shade is, so a roster that appears by day appears in it.
 */
public final class CampShade {
	private CampShade() {
	}

	/** Whether the sun burns monsters here now (vanilla's rule, without the light-level dice). */
	public static boolean sunBurns(ServerLevel level, BlockPos pos) {
		return level.isBrightOutside() && level.environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, pos);
	}

	/**
	 * Where a mob can stand at this shade spot: the nearest height (the given one first, then up and down to 4 away)
	 * with something to stand on, three free blocks (a leader is taller) and a roof somewhere over it; null if none.
	 */
	public static @Nullable BlockPos resolve(ServerLevel level, BlockPos spot) {
		if (!level.isLoaded(spot)) {
			return null;
		}
		for (int i = 0; i <= 8; i++) {
			int dy = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
			BlockPos at = spot.above(dy);
			BlockState below = level.getBlockState(at.below());
			if (below.isFaceSturdy(level, at.below(), Direction.UP) || below.getBlock() instanceof SlabBlock || below.getBlock() instanceof StairBlock) {
				if (open(level, at) && open(level, at.above()) && open(level, at.above(2)) && level.getFluidState(at).isEmpty() && covered(level, at.above(2))) {
					return at;
				}
			}
		}
		return null;
	}

	private static boolean open(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(net.minecraft.tags.BlockTags.WOOL_CARPETS) || state.getCollisionShape(level, pos).isEmpty();
	}

	/**
	 * Whether something stands over this cell to keep the sun off (any block up to 24 above). Read from the blocks, not
	 * the sky light, so it holds the moment a camp is built, before the light engine catches up.
	 */
	public static boolean covered(ServerLevel level, BlockPos pos) {
		for (int dy = 1; dy <= 24; dy++) {
			BlockState state = level.getBlockState(pos.above(dy));
			if (!state.isAir() && state.getFluidState().isEmpty() && !state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
				return true;
			}
		}
		return false;
	}
}

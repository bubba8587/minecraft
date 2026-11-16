// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What endermen think belongs at a block (specs/endermen.md, "The target world"): the cairn's design where a cairn site
 * covers it, natural terrain as first generated everywhere else. Computed, never stored. Answers null where endermen
 * have no opinion: the terrain isn't worked out yet (asked for, ready in a moment), a structure covers the spot, a ward
 * covers it, or the level can't be reproduced.
 */
public final class TargetWorld {
	private TargetWorld() {
	}

	/** The target block here, or null when endermen leave this spot alone (for now). */
	public static @Nullable BlockState target(ServerLevel level, BlockPos pos) {
		if (EndermanWards.isWarded(level, pos)) {
			return null;
		}
		CairnSite site = CairnSites.covering(level, pos);
		if (site != null) {
			BlockState cairn = site.at(pos);
			if (cairn != null) {
				return cairn;
			}
		}
		TerrainOracle.TerrainChunk chunk = TerrainOracle.get(level, pos.getX() >> 4, pos.getZ() >> 4);
		if (chunk == null || chunk.inStructure(pos)) {
			return null;
		}
		return chunk.state(pos);
	}

	/** Y of the top solid block of the generated terrain in this column, or MIN_VALUE when not worked out yet. */
	public static int surface(ServerLevel level, int x, int z) {
		TerrainOracle.TerrainChunk chunk = TerrainOracle.get(level, x >> 4, z >> 4);
		return chunk == null ? Integer.MIN_VALUE : chunk.surface(x, z);
	}
}

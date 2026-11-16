// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * What endermen think belongs at a block (specs/endermen.md, "The target world"): the cairn's design where a cairn site
 * covers it, natural terrain as first generated everywhere else. Computed, never stored. Answers null where endermen
 * have no opinion: the terrain isn't worked out yet (asked for, ready in a moment), a structure covers the spot, a ward
 * covers it, or the level can't be reproduced.
 */
public final class TargetWorld {
	/** A stand-in for natural terrain inside a box (game tests only: the test world's real terrain is superflat). */
	private record Override(ServerLevel level, BoundingBox box, Function<BlockPos, BlockState> terrain, java.util.function.IntBinaryOperator surface) {
	}

	private static final List<Override> OVERRIDES = new ArrayList<>();

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
		Override override = override(level, pos.getX(), pos.getY(), pos.getZ());
		if (override != null) {
			return override.terrain.apply(pos);
		}
		if (level.getServer() instanceof GameTestServer) {
			// The test world is a grid of test fields on superflat ground: endermen judge only where a test says.
			return null;
		}
		TerrainOracle.TerrainChunk chunk = TerrainOracle.get(level, pos.getX() >> 4, pos.getZ() >> 4);
		if (chunk == null || chunk.inStructure(pos)) {
			return null;
		}
		return chunk.state(pos);
	}

	/** Y of the top solid block of the generated terrain in this column, or MIN_VALUE when not worked out yet. */
	public static int surface(ServerLevel level, int x, int z) {
		Override override = override(level, x, Integer.MIN_VALUE, z);
		if (override != null) {
			return override.surface.applyAsInt(x, z);
		}
		TerrainOracle.TerrainChunk chunk = TerrainOracle.get(level, x >> 4, z >> 4);
		return chunk == null ? Integer.MIN_VALUE : chunk.surface(x, z);
	}

	/** Game tests: inside {@code box}, natural terrain is {@code terrain} with its top at {@code surface(x, z)}. */
	public static Runnable overrideTerrain(ServerLevel level, BoundingBox box, Function<BlockPos, BlockState> terrain, java.util.function.IntBinaryOperator surface) {
		Override override = new Override(level, box, terrain, surface);
		OVERRIDES.add(override);
		return () -> OVERRIDES.remove(override);
	}

	private static @Nullable Override override(ServerLevel level, int x, int y, int z) {
		if (OVERRIDES.isEmpty()) {
			return null;
		}
		for (Override override : OVERRIDES) {
			BoundingBox box = override.box;
			if (override.level == level && x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ()
					&& (y == Integer.MIN_VALUE || (y >= box.minY() && y <= box.maxY()))) {
				return override;
			}
		}
		return null;
	}
}

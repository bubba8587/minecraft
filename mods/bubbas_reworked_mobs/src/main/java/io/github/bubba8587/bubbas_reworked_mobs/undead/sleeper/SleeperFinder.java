// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** Sleepers near a point, from the block entities of chunks already loaded (never loads one). */
public final class SleeperFinder {
	private SleeperFinder() {
	}

	/** Every sleeper whose block centre is within {@code range} of {@code centre}. */
	public static List<SleeperBlockEntity> within(Level level, Vec3 centre, double range) {
		List<SleeperBlockEntity> found = new ArrayList<>();
		double rangeSqr = range * range;
		int minX = SectionPos.blockToSectionCoord(centre.x - range);
		int maxX = SectionPos.blockToSectionCoord(centre.x + range);
		int minZ = SectionPos.blockToSectionCoord(centre.z - range);
		int maxZ = SectionPos.blockToSectionCoord(centre.z + range);
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof SleeperBlockEntity sleeper && !sleeper.isRemoved()
							&& centre.distanceToSqr(Vec3.atCenterOf(sleeper.getBlockPos())) <= rangeSqr) {
						found.add(sleeper);
					}
				}
			}
		}
		return found;
	}
}

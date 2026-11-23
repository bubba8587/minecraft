// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Block entities of a kind in an area, read from the chunks' block entity maps. */
public final class LoadedBlockEntities {
	private LoadedBlockEntities() {
	}

	/** Those within {@code range} of {@code centre} (block centres), from loaded chunks only. */
	public static <T extends BlockEntity> List<T> near(Level level, Class<T> type, Vec3 centre, double range) {
		double rangeSqr = range * range;
		List<T> out = new ArrayList<>();
		visit(level, SectionPos.blockToSectionCoord(centre.x - range), SectionPos.blockToSectionCoord(centre.z - range),
				SectionPos.blockToSectionCoord(centre.x + range), SectionPos.blockToSectionCoord(centre.z + range), false, entity -> {
					if (type.isInstance(entity) && !entity.isRemoved() && centre.distanceToSqr(Vec3.atCenterOf(entity.getBlockPos())) <= rangeSqr) {
						out.add(type.cast(entity));
					}
				});
		return out;
	}

	/** Those inside {@code box}, or null when part of the box isn't loaded. */
	public static <T extends BlockEntity> @Nullable List<T> in(Level level, Class<T> type, BoundingBox box) {
		List<T> out = new ArrayList<>();
		boolean all = visit(level, box.minX() >> 4, box.minZ() >> 4, box.maxX() >> 4, box.maxZ() >> 4, false, collect(type, box, out));
		return all ? out : null;
	}

	/** Those inside {@code box}, loading its chunks where needed. */
	public static <T extends BlockEntity> List<T> inLoading(Level level, Class<T> type, BoundingBox box) {
		List<T> out = new ArrayList<>();
		visit(level, box.minX() >> 4, box.minZ() >> 4, box.maxX() >> 4, box.maxZ() >> 4, true, collect(type, box, out));
		return out;
	}

	private static <T extends BlockEntity> Consumer<BlockEntity> collect(Class<T> type, BoundingBox box, List<T> out) {
		return entity -> {
			if (type.isInstance(entity) && !entity.isRemoved() && box.isInside(entity.getBlockPos())) {
				out.add(type.cast(entity));
			}
		};
	}

	/** Visits every block entity in the chunk range; false if a chunk was skipped as unloaded. */
	private static boolean visit(Level level, int minCx, int minCz, int maxCx, int maxCz, boolean load, Consumer<BlockEntity> visitor) {
		boolean all = true;
		for (int cx = minCx; cx <= maxCx; cx++) {
			for (int cz = minCz; cz <= maxCz; cz++) {
				ChunkAccess chunk = load ? level.getChunk(cx, cz) : level.getChunkSource().getChunkNow(cx, cz);
				if (!(chunk instanceof LevelChunk loaded)) {
					all = false;
					continue;
				}
				for (BlockEntity entity : loaded.getBlockEntities().values()) {
					visitor.accept(entity);
				}
			}
		}
		return all;
	}
}

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.AquaticFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/** Vanilla warm-ocean coral clumps grown on the top surfaces of deep-sea fortresses in deep lukewarm ocean. */
public final class CoralClumps {
	/** Columns tried per chunk, and the chance in percent that a found surface gets a clump. */
	private static final int TRIES = 6;
	private static final int CHANCE = 60;

	private CoralClumps() {
	}

	public static void place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, List<StructurePiece> pieces) {
		List<BoundingBox> boxes = pieces.stream().filter(p -> !(p instanceof TearPiece)).map(StructurePiece::getBoundingBox).filter(b -> b.intersects(chunkBB)).toList();
		if (boxes.isEmpty()) {
			return;
		}
		Feature coral = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValueOrThrow(AquaticFeatures.WARM_OCEAN_VEGETATION);
		int top = boxes.stream().mapToInt(BoundingBox::maxY).max().orElseThrow() + 1;
		int bottom = boxes.stream().mapToInt(BoundingBox::minY).min().orElseThrow();
		for (int i = 0; i < TRIES; i++) {
			int x = chunkBB.minX() + random.nextInt(16);
			int z = chunkBB.minZ() + random.nextInt(16);
			boolean chosen = random.nextInt(100) < CHANCE;
			if (!chosen || boxes.stream().noneMatch(b -> b.minX() <= x && x <= b.maxX() && b.minZ() <= z && z <= b.maxZ())) {
				continue;
			}
			BlockPos surface = surface(level, x, z, top, bottom);
			if (surface != null && level.getBiome(surface).is(Biomes.DEEP_LUKEWARM_OCEAN)) {
				coral.place(level, generator, random, surface);
			}
		}
	}

	/** The highest water block standing on the fortress in this column, or null. */
	private static BlockPos surface(WorldGenLevel level, int x, int z, int top, int bottom) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		BlockState above = level.getBlockState(pos);
		for (int y = top - 1; y >= bottom; y--) {
			pos.setY(y);
			BlockState here = level.getBlockState(pos);
			if (above.is(Blocks.WATER) && !here.is(Blocks.WATER) && !here.isAir()) {
				return here.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP) ? pos.above().immutable() : null;
			}
			above = here;
		}
		return null;
	}
}

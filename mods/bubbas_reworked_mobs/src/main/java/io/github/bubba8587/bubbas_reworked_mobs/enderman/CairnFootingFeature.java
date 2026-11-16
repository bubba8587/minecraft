// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * What a cairn site looks like before endermen arrive (owner, 2026-11-16: nothing unnatural a player could already
 * notice): only its natural offerings, placed when the chunk generates: flowers on moss, mushrooms on podzol, sand and
 * clay piles, cactus, melons, plain pumpkins. Never TNT or carved pumpkins (they don't generate naturally) and no End
 * blocks: endermen bring every one of those, footing first. Runs once per overworld chunk (added to every overworld
 * biome), and costs one cheap seed hash unless the region's site is in this chunk.
 */
public record CairnFootingFeature() implements Feature {
	public static final MapCodec<CairnFootingFeature> CODEC = MapCodec.unit(CairnFootingFeature::new);

	@Override
	public MapCodec<CairnFootingFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		ServerLevel server = level.getLevel();
		if (!CairnSites.naturalSites(server)) {
			return false;
		}
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		int rx = Math.floorDiv(chunkX, EndermanTuning.SITE_REGION_CHUNKS);
		int rz = Math.floorDiv(chunkZ, EndermanTuning.SITE_REGION_CHUNKS);
		long[] candidate = CairnSites.candidate(server.getSeed(), rx, rz);
		if (((int) candidate[0] >> 4) != chunkX || ((int) candidate[1] >> 4) != chunkZ) {
			return false;
		}
		Optional<CairnSite> site = CairnSites.compute(server, rx, rz);
		if (site.isEmpty()) {
			return false;
		}
		for (BlockPos pos : site.get().order()) {
			BlockState state = site.get().at(pos);
			if (CairnSite.isStubBlock(state)) {
				level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			}
		}
		return true;
	}
}

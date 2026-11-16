// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * What a cairn site looks like before endermen arrive (builder's choice, specs/endermen.md): its footing and the
 * first block or two of its trunks, placed when the chunk generates, so players can find sites and endermen have
 * something to grow. Only the End blocks of the design up to one block above the ground; the offerings and the rest
 * are for endermen to bring. Runs once per overworld chunk (added to every overworld biome), and costs one cheap
 * seed hash unless the region's site is in this chunk.
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
		BlockPos centre = site.get().origin();
		for (BlockPos pos : site.get().order()) {
			BlockState state = site.get().at(pos);
			if (pos.getY() - centre.getY() <= 1 && !state.is(BlockTags.ENDERMAN_HOLDABLE)) {
				level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			}
		}
		return true;
	}
}

// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import com.mojang.serialization.MapCodec;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Ice Caves (26.4): a Frostbite sleeper lying on the cave floor, frozen in clear ice. */
public record FrozenFrostbiteFeature() implements Feature {
	public static final MapCodec<FrozenFrostbiteFeature> CODEC = MapCodec.unit(FrozenFrostbiteFeature::new);

	@Override
	public MapCodec<FrozenFrostbiteFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (random.nextFloat() >= UndeadTuning.FROZEN_FROSTBITE_CHUNK_CHANCE) {
			return false;
		}
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		for (int i = 0; i < UndeadTuning.FROZEN_FROSTBITE_COLUMN_TRIES; i++) {
			int x = baseX + 2 + random.nextInt(12);
			int z = baseZ + 2 + random.nextInt(12);
			BlockPos floor = findFloor(level, x, z, random);
			if (floor != null) {
				place(level, floor, random);
				return true;
			}
		}
		return false;
	}

	/** An open spot on a cave floor in the Ice Caves biome, searched top-down from a random height. */
	private static BlockPos findFloor(WorldGenLevel level, int x, int z, RandomSource random) {
		int top = Math.min(UndeadTuning.FROZEN_FROSTBITE_MAX_Y, level.getMaxY() - 2);
		int bottom = Math.max(UndeadTuning.FROZEN_FROSTBITE_MIN_Y, level.getMinY() + 1);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		for (int y = top; y > bottom; y--) {
			pos.setY(y);
			if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
					&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
					&& level.getBiome(pos).is(Biomes.ICE_CAVES)) {
				return pos.immutable();
			}
		}
		return null;
	}

	private static void place(WorldGenLevel level, BlockPos at, RandomSource random) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		SleeperPose pose = random.nextFloat() < 0.3F ? SleeperPose.PRONE : SleeperPose.LYING;
		level.setBlock(at, ModBlocks.sleeper(UndeadVariant.FROSTBITE).defaultBlockState()
				.setValue(SleeperBlock.POSE, pose).setValue(SleeperBlock.FACING, facing), Block.UPDATE_CLIENTS);
		// Clear ice over and round it, so it is seen frozen inside (what a stray breaks out of)
		level.setBlock(at.above(), Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos p = at.relative(side);
			if (level.getBlockState(p).isAir()) {
				level.setBlock(p, Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}
}

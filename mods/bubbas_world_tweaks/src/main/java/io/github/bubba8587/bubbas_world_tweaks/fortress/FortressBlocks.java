// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/** The Nether-to-sea block swap for deep-sea fortress pieces. */
public final class FortressBlocks {
	/** Share of plain bricks that are mossy, then cracked, in percent. */
	private static final int MOSSY = 20;
	private static final int CRACKED = 10;
	private static final int MOSSY_STAIRS = 25;

	private FortressBlocks() {
	}

	/** The state a deep-sea fortress places instead of what the vanilla piece asked for at local (x, y, z). */
	public static BlockState swap(StructurePiece piece, BlockState state, BlockPos pos, int x, int y, int z) {
		if (state.isAir()) {
			BlockState trim = FortressTrim.inWater(piece, x, y, z);
			return trim != null ? trim : Blocks.WATER.defaultBlockState();
		}
		Block block = state.getBlock();
		if (block == Blocks.NETHER_BRICKS) {
			BlockState trim = FortressTrim.trim(piece, x, y, z);
			return trim != null ? trim : bricks(pos);
		}
		if (block == Blocks.NETHER_BRICK_STAIRS) {
			Block stairs = roll(pos) < MOSSY_STAIRS ? Blocks.MOSSY_STONE_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS;
			return waterlogged(stairs.withPropertiesOf(state));
		}
		if (block == Blocks.NETHER_BRICK_FENCE) {
			return waterlogged(Blocks.PRISMARINE_WALL.defaultBlockState());
		}
		if (block == Blocks.LAVA) {
			return Blocks.MAGMA_BLOCK.defaultBlockState();
		}
		if (block == Blocks.SOUL_SAND) {
			return Blocks.SAND.defaultBlockState();
		}
		if (block == Blocks.NETHER_WART) {
			return waterlogged(Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + roll(pos) % SeaPickleBlock.MAX_PICKLES));
		}
		return waterlogged(state);
	}

	/** Stone bricks, mossy or cracked by position. */
	public static BlockState bricks(BlockPos pos) {
		int r = roll(pos);
		if (r < MOSSY) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (r < MOSSY + CRACKED) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static int roll(BlockPos pos) {
		return (int) Math.floorMod(Mth.getSeed(pos), 100L);
	}

	private static BlockState waterlogged(BlockState state) {
		return state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, true) : state;
	}

	/** A loot barrel facing out of its wall, in place of a fortress chest. */
	public static boolean placeBarrel(ServerLevelAccessor level, BoundingBox chunkBB, RandomSource random, BlockPos pos) {
		if (!chunkBB.isInside(pos) || level.getBlockState(pos).is(Blocks.BARREL)) {
			return false;
		}
		Direction facing = StructurePiece.reorient(level, pos, Blocks.CHEST.defaultBlockState()).getValue(HorizontalDirectionalBlock.FACING);
		level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing), 2);
		if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
			barrel.setLootTable(DeepseaFortresses.BARREL_LOOT, random.nextLong());
		}
		return true;
	}
}

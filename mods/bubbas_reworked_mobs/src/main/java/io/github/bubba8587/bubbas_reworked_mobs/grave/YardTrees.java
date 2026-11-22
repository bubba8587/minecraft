// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which trees and flowers a yard has, from its biome (owner, 2026-11-22: "trees and plants can be in the crypt yards ...
 * but do it intentionally"). Wild trees are cleared out of a yard; these are planted on purpose: one or two per yard in
 * place of a grave, and flowers left on some graves.
 */
public enum YardTrees {
	OAK(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, new Block[] { Blocks.POPPY, Blocks.DANDELION, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER }),
	BIRCH(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5, 2, new Block[] { Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET }),
	SPRUCE(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, 5, 3, new Block[] { Blocks.LILY_OF_THE_VALLEY, Blocks.CORNFLOWER, Blocks.FERN }),
	DARK_OAK(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, 4, 2, new Block[] { Blocks.LILY_OF_THE_VALLEY, Blocks.RED_TULIP, Blocks.POPPY }),
	PALE_OAK(Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_LEAVES, 4, 2, new Block[] { Blocks.OPEN_EYEBLOSSOM, Blocks.CLOSED_EYEBLOSSOM, Blocks.LILY_OF_THE_VALLEY }),
	/** Deserts: no trees, dead bushes instead of grass, and the odd cactus flower... kept simple: dead bushes. */
	NONE(Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4, 2, new Block[] { Blocks.DEAD_BUSH });

	private final Block log;
	private final Block leaves;
	private final int minHeight;
	private final int canopyDepth;
	private final Block[] flowers;

	YardTrees(Block log, Block leaves, int minHeight, int canopyDepth, Block[] flowers) {
		this.log = log;
		this.leaves = leaves;
		this.minHeight = minHeight;
		this.canopyDepth = canopyDepth;
		this.flowers = flowers;
	}

	public BlockState log() {
		return log.defaultBlockState();
	}

	/** Leaves that never decay (the tree is built without vanilla's leaf distances). */
	public BlockState leaves() {
		return leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	}

	public int minHeight() {
		return minHeight;
	}

	/** How many layers of leaves hang below the trunk's top. */
	public int canopyDepth() {
		return canopyDepth;
	}

	/** Canopy radius at {@code dy} relative to the trunk's top: spruces taper to a point, broadleaves are rounded. */
	public int radiusAt(int dy) {
		if (this == SPRUCE) {
			return dy >= 1 ? 0 : dy == 0 ? 1 : Math.min(2, -dy);
		}
		return dy >= 0 ? 1 : 2;
	}

	public BlockState flower(RandomSource random) {
		return flowers[random.nextInt(flowers.length)].defaultBlockState();
	}

	public static YardTrees forBiome(Holder<Biome> biome) {
		if (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS)) {
			return NONE;
		}
		if (biome.is(Biomes.PALE_GARDEN)) {
			return PALE_OAK;
		}
		if (biome.is(Biomes.DARK_FOREST)) {
			return DARK_OAK;
		}
		if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
			return BIRCH;
		}
		if (biome.is(BiomeTags.IS_TAIGA) || biome.value().getBaseTemperature() < 0.15F) {
			return SPRUCE;
		}
		return OAK;
	}
}

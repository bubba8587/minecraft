// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

/**
 * How endermen sort blocks (specs/endermen.md, E1). No record of placed blocks: each spot is bucketed by what kind of
 * block it is.
 * <ul>
 * <li>{@link Family terrain families}: the blocks natural terrain is made of (dirt, sand, stone...). Freely reverted
 * toward the target, and any member fills a hole whose target is in the same family (an enderman with cobblestone
 * fills a stone hole with stone).</li>
 * <li>{@link #isNatural natural}: blocks that generate in the overworld (families plus trees, ores, plants, ice...).
 * Never touched unless a cairn's offering needs exactly that block.</li>
 * <li>everything else is unnatural (planks, glass, bricks, End blocks...): touched only with a small chance.</li>
 * </ul>
 */
public final class TerrainKinds {
	/** A terrain material. The order is only for display. */
	public enum Family {
		SOIL, STONE, DEEPSLATE, SAND, RED_SAND, GRAVEL, CLAY, MUD, SNOW, TERRACOTTA
	}

	private static final Set<Block> SOIL = Set.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.COARSE_DIRT,
			Blocks.ROOTED_DIRT, Blocks.DIRT_PATH);
	private static final Set<Block> STONE = Set.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.ANDESITE,
			Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF, Blocks.CALCITE, Blocks.SMOOTH_STONE);
	private static final Set<Block> DEEPSLATE = Set.of(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
	private static final Set<Block> SAND = Set.of(Blocks.SAND, Blocks.SANDSTONE);
	private static final Set<Block> RED_SAND = Set.of(Blocks.RED_SAND, Blocks.RED_SANDSTONE);
	/** Natural blocks without a family that endermen still never move: logs, leaves, ores, ice and the like. */
	private static final Set<Block> NATURAL_EXTRA = Set.of(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.MAGMA_BLOCK,
			Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.SMOOTH_BASALT, Blocks.DRIPSTONE_BLOCK, Blocks.MOSS_BLOCK,
			Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM, Blocks.PUMPKIN, Blocks.MELON,
			Blocks.BONE_BLOCK, Blocks.SNOW, Blocks.POWDER_SNOW, Blocks.SCULK, Blocks.PACKED_MUD, Blocks.PALE_MOSS_BLOCK,
			Blocks.CACTUS, Blocks.CACTUS_FLOWER, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM);

	private TerrainKinds() {
	}

	/** The terrain family of this block, or null when it isn't a terrain material. */
	public static @Nullable Family family(BlockState state) {
		Block block = state.getBlock();
		if (SOIL.contains(block)) {
			return Family.SOIL;
		}
		if (STONE.contains(block)) {
			return Family.STONE;
		}
		if (DEEPSLATE.contains(block)) {
			return Family.DEEPSLATE;
		}
		if (SAND.contains(block)) {
			return Family.SAND;
		}
		if (RED_SAND.contains(block)) {
			return Family.RED_SAND;
		}
		if (block == Blocks.GRAVEL) {
			return Family.GRAVEL;
		}
		if (block == Blocks.CLAY) {
			return Family.CLAY;
		}
		if (block == Blocks.MUD) {
			return Family.MUD;
		}
		if (block == Blocks.SNOW_BLOCK) {
			return Family.SNOW;
		}
		if (state.is(BlockTags.TERRACOTTA)) {
			return Family.TERRACOTTA;
		}
		return null;
	}

	/** Whether this block generates naturally in the overworld (E1): touched freely if a family, else never. */
	public static boolean isNatural(BlockState state) {
		return family(state) != null || NATURAL_EXTRA.contains(state.getBlock()) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
				|| state.is(BlockTags.FLOWERS) || state.is(BlockTags.ICE) || state.is(BlockTags.BASE_STONE_OVERWORLD)
				|| state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(ConventionalBlockTags.ORES)
				|| !state.getFluidState().isEmpty() || state.is(BlockTags.REPLACEABLE);
	}

	/**
	 * Whether an enderman may lift this block at all: no block entity (chests, sleepers, displays), not unbreakable or
	 * boss-proof, and either vanilla's holdable set or a plain full cube (so no doors, beds, rails, torches...).
	 */
	public static boolean liftable(BlockGetter level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, pos) < 0.0F
				|| state.is(BlockTags.WITHER_IMMUNE) || state.is(BlockTags.DRAGON_IMMUNE)) {
			return false;
		}
		return state.is(BlockTags.ENDERMAN_HOLDABLE) || state.isCollisionShapeFullBlock(level, pos);
	}

	/**
	 * Whether {@code real} counts as the {@code target} block: same block, and the same facing, axis, half and slab
	 * type where it has them (the shape of stairs and the like follows its neighbours, so it doesn't count).
	 */
	public static boolean matches(BlockState real, BlockState target) {
		if (real.getBlock() != target.getBlock()) {
			return false;
		}
		return same(real, target, BlockStateProperties.FACING) && same(real, target, BlockStateProperties.HORIZONTAL_FACING)
				&& same(real, target, BlockStateProperties.AXIS) && same(real, target, BlockStateProperties.HALF)
				&& same(real, target, BlockStateProperties.SLAB_TYPE);
	}

	private static <T extends Comparable<T>> boolean same(BlockState a, BlockState b, Property<T> property) {
		return !a.hasProperty(property) || !b.hasProperty(property) || a.getValue(property).equals(b.getValue(property));
	}

	/** Whether an enderman carrying {@code carried} can put it in a spot whose target is {@code target}. */
	public static boolean fills(BlockState carried, BlockState target) {
		if (matches(carried, target)) {
			return true;
		}
		Family family = family(target);
		return family != null && family == family(carried);
	}
}

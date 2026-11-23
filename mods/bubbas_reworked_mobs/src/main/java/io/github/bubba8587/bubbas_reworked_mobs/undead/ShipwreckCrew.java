// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Drowned sleepers placed inside a shipwreck piece as it generates. */
public final class ShipwreckCrew {
	private ShipwreckCrew() {
	}

	/** Places the crew in the part of {@code piece} inside {@code chunk}. */
	public static void place(WorldGenLevel level, BoundingBox piece, BoundingBox chunk, long seed) {
		BoundingBox box = intersect(piece, chunk);
		if (box == null) {
			return;
		}
		net.minecraft.world.level.levelgen.PositionalRandomFactory cells = new net.minecraft.world.level.levelgen.XoroshiroRandomSource(seed).forkPositional();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			RandomSource cell = cells.at(pos);
			if (cell.nextFloat() >= UndeadTuning.SHIPWRECK_CREW_CHANCE || !inHull(level, pos)) {
				continue;
			}
			BlockState here = level.getBlockState(pos);
			boolean water = !here.getFluidState().isEmpty();
			Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(cell);
			boolean sitting = cell.nextFloat() < 0.3F && open(level, pos.above());
			level.setBlock(pos, ModBlocks.sleeper(UndeadVariant.DROWNED).defaultBlockState()
					.setValue(SleeperBlock.POSE, sitting ? SleeperPose.SITTING : SleeperPose.LYING).setValue(SleeperBlock.FACING, facing)
					.setValue(SleeperBlock.WATERLOGGED, water), Block.UPDATE_CLIENTS);
			if (level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper) {
				float gear = cell.nextFloat();
				if (gear < 0.15F) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
				} else if (gear < 0.3F) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
				}
				if (cell.nextFloat() < 0.1F) {
					sleeper.setGear(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
				}
				if (cell.nextFloat() < 0.25F) {
					sleeper.setGear(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
				}
			}
		}
	}

	/** Between decks: empty or flooded, on a wooden floor with wood overhead within 4 (one column, so every chunk agrees). */
	public static boolean inHull(WorldGenLevel level, BlockPos pos) {
		BlockState here = level.getBlockState(pos);
		if (!(here.isAir() || here.is(Blocks.WATER))) {
			return false;
		}
		BlockState floor = level.getBlockState(pos.below());
		if (!wood(floor) || !floor.isFaceSturdy(level, pos.below(), Direction.UP)) {
			return false;
		}
		for (int dy = 1; dy <= 4; dy++) {
			if (wood(level.getBlockState(pos.above(dy)))) {
				return true;
			}
		}
		return false;
	}

	/** Whether a placed sleeper is between decks: wooden floor, wood overhead within 4. */
	public static boolean inHullAround(WorldGenLevel level, BlockPos pos) {
		if (!wood(level.getBlockState(pos.below()))) {
			return false;
		}
		for (int dy = 1; dy <= 4; dy++) {
			if (wood(level.getBlockState(pos.above(dy)))) {
				return true;
			}
		}
		return false;
	}

	private static boolean wood(BlockState state) {
		return state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_STAIRS)
				|| state.is(BlockTags.WOODEN_TRAPDOORS);
	}

	private static boolean open(WorldGenLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private static BoundingBox intersect(BoundingBox a, BoundingBox b) {
		if (!a.intersects(b)) {
			return null;
		}
		return new BoundingBox(Math.max(a.minX(), b.minX()), Math.max(a.minY(), b.minY()), Math.max(a.minZ(), b.minZ()), Math.min(a.maxX(), b.maxX()),
				Math.min(a.maxY(), b.maxY()), Math.min(a.maxZ(), b.maxZ()));
	}
}

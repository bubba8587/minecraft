// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * The design of one cairn (specs/endermen.md, "Cairn look"), fixed by its seed: a twisting, chorus-like spire of
 * purpur on an end stone brick footing, branches ending in end-rod 'flowers', end-city touches, and overworld
 * offerings at its foot that depend on the biome. The tallest end rod, at the top of the main trunk, lies on its side
 * pointing toward the nearest stronghold. Structural blocks are only ones that never generate in the overworld (no
 * obsidian, E3); offerings are vanilla enderman-holdable blocks by design. No banners (the preview's placement was
 * bad, owner): left out for now.
 *
 * <p>Pure function of its inputs; positions are relative to the cairn origin (the air block just above the ground at
 * its centre, y 0; the footing is sunk into the ground at y -1).
 */
public final class CairnDesign {
	/** What grows around a cairn's foot, picked from the biome at its centre. */
	public enum Offerings {
		TEMPERATE, FLOWERY, JUNGLE, SWAMP, SANDY, BADLANDS, SNOWY, DARK, MUSHROOM
	}

	private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

	private final RandomSource random;
	private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
	private final IntBinaryOperator groundAt;

	private CairnDesign(long seed, IntBinaryOperator groundAt) {
		this.random = new LegacyRandomSource(seed);
		this.groundAt = groundAt;
	}

	/**
	 * Designs a cairn.
	 *
	 * @param seed the site's seed
	 * @param offerings the offering palette (from the biome)
	 * @param pointer the horizontal direction of the nearest stronghold
	 * @param groundAt for a relative (x, z), the relative y of the top ground block there (-1 on the flat centre)
	 * @return every block, relative to the origin, in build order (bottom up)
	 */
	public static List<Map.Entry<BlockPos, BlockState>> design(long seed, Offerings offerings, Direction pointer, IntBinaryOperator groundAt) {
		CairnDesign design = new CairnDesign(seed, groundAt);
		design.build(offerings, pointer);
		List<Map.Entry<BlockPos, BlockState>> list = new ArrayList<>(design.blocks.entrySet());
		// Stable sort: bottom up, keeping each trunk's growth order within a layer.
		list.sort(Comparator.comparingInt(e -> e.getKey().getY()));
		return list;
	}

	private void build(Offerings offerings, Direction pointer) {
		int footing = random.nextFloat() < 0.45F ? 2 : 1;
		buildFooting(footing);
		int trunks = 1 + (random.nextFloat() < 0.55F ? 1 : 0) + (random.nextFloat() < 0.2F ? 1 : 0);
		int height = EndermanTuning.CAIRN_MIN_HEIGHT + random.nextInt(EndermanTuning.CAIRN_MAX_HEIGHT - EndermanTuning.CAIRN_MIN_HEIGHT + 1);
		boolean glassUsed = false;
		List<BlockPos> starts = new ArrayList<>();
		starts.add(BlockPos.ZERO);
		for (int i = 1; i < trunks; i++) {
			for (int tries = 0; tries < 8; tries++) {
				BlockPos start = new BlockPos(random.nextInt(2 * footing + 1) - footing, 0, random.nextInt(2 * footing + 1) - footing);
				if (starts.stream().noneMatch(s -> s.distManhattan(start) < 2)) {
					starts.add(start);
					break;
				}
			}
		}
		for (int i = 0; i < starts.size(); i++) {
			boolean main = i == 0;
			int h = main ? height : Math.max(4, (int) (height * (0.4F + random.nextFloat() * 0.35F)));
			glassUsed |= growTrunk(starts.get(i), h, main, pointer, !glassUsed);
		}
		buildRim(footing);
		buildOfferings(offerings, footing);
	}

	private void buildFooting(int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius + 1) {
					continue;
				}
				put(new BlockPos(dx, -1, dz), random.nextFloat() < 0.25F ? Blocks.END_STONE.defaultBlockState() : Blocks.END_STONE_BRICKS.defaultBlockState());
			}
		}
	}

	/** Low slabs around the footing's rim, like a worn step. */
	private void buildRim(int radius) {
		for (Direction side : HORIZONTAL) {
			if (random.nextFloat() < 0.55F) {
				int along = random.nextInt(2 * radius + 1) - radius;
				BlockPos at = new BlockPos(side.getStepX() * (radius + 1) + (side.getAxis() == Direction.Axis.Z ? along : 0), 0,
						side.getStepZ() * (radius + 1) + (side.getAxis() == Direction.Axis.X ? along : 0));
				if (groundAt.applyAsInt(at.getX(), at.getZ()) == -1) {
					putIfFree(at, random.nextBoolean() ? Blocks.PURPUR_SLAB.defaultBlockState() : Blocks.END_STONE_BRICK_SLAB.defaultBlockState());
				}
			}
		}
	}

	/** A chorus-like stalk: pillar runs that step sideways at purpur-block knuckles; returns whether it hung the glass. */
	private boolean growTrunk(BlockPos start, int height, boolean main, Direction pointer, boolean mayHangGlass) {
		boolean hungGlass = false;
		BlockPos column = start;
		Direction last = null;
		int y = 0;
		while (true) {
			int run = 2 + random.nextInt(2);
			for (int i = 0; i < run && y < height - 1; i++, y++) {
				if (!putIfFree(column.atY(y), pillar(Direction.Axis.Y))) {
					return hungGlass;
				}
			}
			BlockPos knuckle = column.atY(y);
			if (!putIfFree(knuckle, Blocks.PURPUR_BLOCK.defaultBlockState())) {
				return hungGlass;
			}
			if (y >= height - 1) {
				if (main) {
					// The tallest end rod, on its side at the very top: points toward the nearest stronghold.
					putIfFree(knuckle.relative(pointer), Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, pointer));
				} else {
					putIfFree(knuckle.above(), Blocks.END_ROD.defaultBlockState());
				}
				return hungGlass;
			}
			Direction step = pickStep(knuckle, last, start);
			if (y > 2 && random.nextFloat() < 0.6F) {
				hungGlass |= branch(knuckle, step, mayHangGlass && !hungGlass);
			}
			if (step == null) {
				putIfFree(knuckle.above(), Blocks.END_ROD.defaultBlockState());
				return hungGlass;
			}
			// The knuckle: stairs upside down beside the block, then the next run climbs from the stairs.
			BlockPos next = knuckle.relative(step);
			if (!putIfFree(next, Blocks.PURPUR_STAIRS.defaultBlockState().setValue(StairBlock.FACING, step.getOpposite()).setValue(StairBlock.HALF, Half.TOP))) {
				return hungGlass;
			}
			column = next;
			last = step;
			y++;
		}
	}

	/** A sideways step that doesn't double back, stays within 2 of the trunk's start and lands on free blocks. */
	private Direction pickStep(BlockPos knuckle, Direction last, BlockPos start) {
		int offset = random.nextInt(4);
		for (int i = 0; i < 4; i++) {
			Direction d = HORIZONTAL[(offset + i) % 4];
			if (last != null && d == last.getOpposite()) {
				continue;
			}
			BlockPos next = knuckle.relative(d);
			if (Math.abs(next.getX() - start.getX()) > 2 || Math.abs(next.getZ() - start.getZ()) > 2) {
				continue;
			}
			if (!blocks.containsKey(next) && !blocks.containsKey(next.above()) && !blocks.containsKey(next.above(2))) {
				return d;
			}
		}
		return null;
	}

	/** A short arm out of a knuckle, rising to an end-rod flower; maybe a magenta window pane hanging under it. */
	private boolean branch(BlockPos knuckle, Direction step, boolean mayHangGlass) {
		int offset = random.nextInt(4);
		for (int i = 0; i < 4; i++) {
			Direction d = HORIZONTAL[(offset + i) % 4];
			if (d == step) {
				continue;
			}
			int length = 1 + random.nextInt(2);
			BlockPos arm = knuckle.relative(d);
			BlockPos end = knuckle.relative(d, length);
			if (blocks.containsKey(arm) || blocks.containsKey(end) || blocks.containsKey(end.above()) || blocks.containsKey(end.above(2))) {
				continue;
			}
			if (length == 2) {
				put(arm, pillar(d.getAxis()));
			}
			put(end, Blocks.PURPUR_BLOCK.defaultBlockState());
			BlockPos top = end;
			int rise = random.nextInt(3);
			for (int r = 0; r < rise && !blocks.containsKey(top.above()); r++) {
				top = top.above();
				put(top, pillar(Direction.Axis.Y));
			}
			putIfFree(top.above(), Blocks.END_ROD.defaultBlockState());
			if (mayHangGlass && random.nextFloat() < 0.5F && !blocks.containsKey(end.below())) {
				put(end.below(), Blocks.STAINED_GLASS.magenta().defaultBlockState());
				return true;
			}
			return false;
		}
		return false;
	}

	private void buildOfferings(Offerings palette, int footing) {
		int count = 3 + random.nextInt(4);
		double angle = random.nextDouble() * Math.PI * 2.0;
		int tntAt = random.nextFloat() < (palette == Offerings.SANDY || palette == Offerings.BADLANDS ? 0.34F : EndermanTuning.CAIRN_TNT_CHANCE)
				? random.nextInt(count) : -1;
		List<BlockPos> used = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			angle += Math.PI * 2.0 / count * (0.7 + random.nextDouble() * 0.6);
			double distance = footing + 1.6 + random.nextDouble() * 1.4;
			int x = (int) Math.round(Math.cos(angle) * distance);
			int z = (int) Math.round(Math.sin(angle) * distance);
			// Cactus needs free sides; keep offerings apart.
			if (used.stream().anyMatch(u -> Math.abs(u.getX() - x) <= 1 && Math.abs(u.getZ() - z) <= 1)) {
				continue;
			}
			int ground = groundAt.applyAsInt(x, z);
			if (Math.abs(ground + 1) > 2) {
				continue;
			}
			BlockPos base = new BlockPos(x, ground + 1, z);
			if (blocks.containsKey(base) || blocks.containsKey(base.above()) || blocks.containsKey(base.above(2))) {
				continue;
			}
			used.add(base);
			if (i == tntAt) {
				put(base, Blocks.TNT.defaultBlockState());
				continue;
			}
			offering(palette, base, Direction.fromYRot(Math.toDegrees(angle) - 90.0));
		}
	}

	private void offering(Offerings palette, BlockPos base, Direction outward) {
		float roll = random.nextFloat();
		switch (palette) {
			case SANDY -> {
				if (roll < 0.55F) {
					cactus(base, Blocks.SAND.defaultBlockState());
				} else {
					put(base, roll < 0.8F ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
				}
			}
			case BADLANDS -> {
				if (roll < 0.5F) {
					cactus(base, Blocks.RED_SAND.defaultBlockState());
				} else {
					put(base, Blocks.RED_SAND.defaultBlockState());
				}
			}
			case SWAMP -> {
				if (roll < 0.35F) {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.BLUE_ORCHID.defaultBlockState());
				} else if (roll < 0.6F) {
					flower(base, Blocks.PODZOL.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState());
				} else {
					put(base, roll < 0.8F ? Blocks.CLAY.defaultBlockState() : Blocks.MUD.defaultBlockState());
				}
			}
			case JUNGLE -> {
				if (roll < 0.4F) {
					put(base, Blocks.MELON.defaultBlockState());
				} else {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.BLUE_ORCHID.defaultBlockState(), Blocks.POPPY.defaultBlockState(),
							Blocks.DANDELION.defaultBlockState()));
				}
			}
			case SNOWY -> {
				if (roll < 0.4F) {
					put(base, roll < 0.2F ? Blocks.GRAVEL.defaultBlockState() : Blocks.CLAY.defaultBlockState());
				} else if (roll < 0.6F) {
					put(base, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, outward));
				} else {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.WHITE_TULIP.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()));
				}
			}
			case DARK -> {
				if (roll < 0.5F) {
					flower(base, Blocks.PODZOL.defaultBlockState(), pick(Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()));
				} else if (roll < 0.75F) {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.CLOSED_EYEBLOSSOM.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()));
				} else {
					put(base, Blocks.PUMPKIN.defaultBlockState());
				}
			}
			case MUSHROOM -> flower(base, Blocks.MYCELIUM.defaultBlockState(), pick(Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()));
			case FLOWERY -> flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.ALLIUM.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState(),
					Blocks.PINK_TULIP.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState()));
			case TEMPERATE -> {
				if (roll < 0.55F) {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
							Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
							Blocks.RED_TULIP.defaultBlockState()));
				} else if (roll < 0.7F) {
					put(base, Blocks.PUMPKIN.defaultBlockState());
				} else if (roll < 0.82F) {
					put(base, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, outward));
				} else if (roll < 0.92F) {
					flower(base, Blocks.PODZOL.defaultBlockState(), pick(Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()));
				} else {
					put(base, Blocks.CLAY.defaultBlockState());
				}
			}
		}
	}

	private void flower(BlockPos base, BlockState soil, BlockState plant) {
		put(base, soil);
		put(base.above(), plant);
	}

	private void cactus(BlockPos base, BlockState sand) {
		put(base, sand);
		put(base.above(), Blocks.CACTUS.defaultBlockState());
		if (random.nextFloat() < 0.5F) {
			put(base.above(2), Blocks.CACTUS_FLOWER.defaultBlockState());
		}
	}

	private BlockState pick(BlockState... states) {
		return states[random.nextInt(states.length)];
	}

	private static BlockState pillar(Direction.Axis axis) {
		return Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private void put(BlockPos pos, BlockState state) {
		blocks.put(pos, state);
	}

	private boolean putIfFree(BlockPos pos, BlockState state) {
		if (blocks.containsKey(pos)) {
			return false;
		}
		blocks.put(pos, state);
		return true;
	}
}

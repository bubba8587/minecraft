// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import java.util.HashSet;
import java.util.Set;

import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlock;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One crypt yard and the crypt under it, built in code (specs/graveyards.md). Everything is decided by the piece's
 * own seed, never by the chunk being generated, so every chunk the yard spans builds its own part of the same yard.
 * Each column follows the ground as generated there (the top solid block), so yards drape over gentle slopes.
 *
 * <p>Layout (x across, z from the gate at the south fence to the north): an iron bar fence with stone brick posts at
 * the corners and either side of a 3-wide gate, soul lanterns on the gate posts; a coarse-dirt path from the gate
 * north; rows of graves either side (a gravestone at the north end, a coarse dirt or podzol plot, a sleeper one block
 * under it, sometimes loot buried in the plot); stairs down at the north end into a family vault (stone brick, sleepers
 * in wall niches, candles, a soul lantern, a chest with its own loot table) under the ground north of the yard.
 *
 * <p>This first build has one state of breakdown, overgrown: some fence bars missing, some stones turned askew or
 * fallen, grass and moss through the path, weeds on the plots.
 */
public class CryptYardPiece extends StructurePiece {
	private final long seed;
	/** South-west corner of the yard, at ground height (the top solid block). */
	private final BlockPos origin;
	private final int width;
	private final int depth;
	private final boolean crypt;
	/** The yard's own trees, picked from its biome (owner, 2026-11-22: trees and plants in yards, placed on purpose). */
	private final YardTrees trees;

	public CryptYardPiece(long seed, BlockPos origin, int width, int depth, boolean crypt, YardTrees trees) {
		super(Graveyards.CRYPT_YARD_PIECE, 0, box(origin, width, depth, crypt));
		this.seed = seed;
		this.origin = origin;
		this.width = width;
		this.depth = depth;
		this.crypt = crypt;
		this.trees = trees;
	}

	public CryptYardPiece(CompoundTag tag) {
		super(Graveyards.CRYPT_YARD_PIECE, tag);
		this.seed = tag.getLongOr("Seed", 0L);
		this.origin = BlockPos.of(tag.getLongOr("Origin", 0L));
		this.width = tag.getIntOr("Width", GraveTuning.YARD_MIN_WIDTH);
		this.depth = tag.getIntOr("Depth", GraveTuning.YARD_MIN_DEPTH);
		this.crypt = tag.getBooleanOr("Crypt", false);
		this.trees = YardTrees.values()[Math.floorMod(tag.getIntOr("Trees", 0), YardTrees.values().length)];
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Seed", seed);
		tag.putLong("Origin", origin.asLong());
		tag.putInt("Width", width);
		tag.putInt("Depth", depth);
		tag.putBoolean("Crypt", crypt);
		tag.putInt("Trees", trees.ordinal());
	}

	// --- the vault's size and place (beyond the stairs, under the ground south of the yard) -------------

	private static final int VAULT_HALF_WIDTH = 3;
	private static final int VAULT_LENGTH = 9;
	private static final int VAULT_HEIGHT = 4;

	private static int gateX(BlockPos origin, int width) {
		return origin.getX() + width / 2;
	}

	/** First stair (top step) z: four rows in from the far (south) fence. */
	private static int stairsZ(BlockPos origin, int depth) {
		return origin.getZ() + depth - 5;
	}

	private static BoundingBox box(BlockPos origin, int width, int depth, boolean crypt) {
		int minY = crypt ? origin.getY() - GraveTuning.CRYPT_DEPTH - 1 : origin.getY() - 4;
		int maxZ = crypt ? stairsZ(origin, depth) + GraveTuning.CRYPT_DEPTH + VAULT_LENGTH + 2 : origin.getZ() + depth - 1;
		int minX = crypt ? Math.min(origin.getX(), gateX(origin, width) - VAULT_HALF_WIDTH - 2) : origin.getX();
		int maxX = crypt ? Math.max(origin.getX() + width - 1, gateX(origin, width) + VAULT_HALF_WIDTH + 2) : origin.getX() + width - 1;
		return new BoundingBox(minX, minY, origin.getZ(), maxX, origin.getY() + 26, maxZ);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource chunkRandom,
			BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		Build b = new Build(level, chunkBB);
		// Yards are built after trees (structure step top_layer_modification): clear them out of the yard first.
		for (int x = minX(); x <= maxX(); x++) {
			for (int z = minZ(); z <= maxZ(); z++) {
				b.clearCover(x, z, 24);
			}
		}
		fence(b, RandomSource.create(seed ^ 0xFE11CEL));
		path(b, RandomSource.create(seed ^ 0x9A7AL));
		graves(b, RandomSource.create(seed ^ 0x6A4E5L));
		if (crypt) {
			vault(b, RandomSource.create(seed ^ 0xC4F97L));
		}
	}

	/** Whether this yard has stairs down to a crypt. */
	public boolean hasCrypt() {
		return crypt;
	}

	/** The middle of the vault's floor (feet height), for previews and tests; meaningful only with a crypt. */
	public BlockPos vaultCentre() {
		int floor = origin.getY() - GraveTuning.CRYPT_DEPTH + 1;
		int start = stairsZ(origin, depth) + (origin.getY() - floor) + 2;
		return new BlockPos(gateX(origin, width), floor, start + VAULT_LENGTH / 2);
	}

	/** The gate's ground block (the middle of the north fence). */
	public BlockPos gate() {
		return new BlockPos(gateX(origin, width), origin.getY(), origin.getZ());
	}

	public int width() {
		return width;
	}

	public int depth() {
		return depth;
	}

	// --- the yard -----------------------------------------------------------------------------------

	private int minX() {
		return origin.getX();
	}

	private int maxX() {
		return origin.getX() + width - 1;
	}

	private int minZ() {
		return origin.getZ();
	}

	private int maxZ() {
		return origin.getZ() + depth - 1;
	}

	private void fence(Build b, RandomSource random) {
		int gate = gateX(origin, width);
		Set<Long> bars = new java.util.LinkedHashSet<>();
		Set<Long> posts = new java.util.LinkedHashSet<>();
		int lanternSide = RandomSource.create(seed ^ 0x1A7E2L).nextBoolean() ? 2 : -2;
		for (int x = minX(); x <= maxX(); x++) {
			for (int z = minZ(); z <= maxZ(); z++) {
				boolean edge = x == minX() || x == maxX() || z == minZ() || z == maxZ();
				if (!edge) {
					continue;
				}
				boolean corner = (x == minX() || x == maxX()) && (z == minZ() || z == maxZ());
				boolean gatePost = z == minZ() && Math.abs(x - gate) == 2;
				if (z == minZ() && Math.abs(x - gate) <= 1) {
					continue; // the gate
				}
				if (corner || gatePost) {
					posts.add(BlockPos.asLong(x, 0, z));
				} else {
					bars.add(BlockPos.asLong(x, 0, z));
				}
			}
		}
		for (long key : posts) {
			int x = BlockPos.getX(key);
			int z = BlockPos.getZ(key);
			// Every roll is made before looking at the world, so every chunk draws the same sequence.
			BlockState brick = random.nextFloat() < 0.35F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
			int g = b.ground(x, z);
			if (g == Integer.MIN_VALUE) {
				continue;
			}
			b.put(x, g + 1, z, brick);
			b.put(x, g + 2, z, brick);
			// One soul lantern in the whole yard (owner, 2026-11-22), on one gate post (which one is the seed's).
			if (z == minZ() && x == gate + lanternSide) {
				b.put(x, g + 3, z, Blocks.SOUL_LANTERN.defaultBlockState());
			}
		}
		for (long key : bars) {
			int x = BlockPos.getX(key);
			int z = BlockPos.getZ(key);
			float roll = random.nextFloat();
			int g = b.ground(x, z);
			if (g == Integer.MIN_VALUE) {
				continue;
			}
			if (roll < 0.12F) {
				continue; // overgrown: a gap
			}
			BlockState state = Blocks.IRON_BARS.defaultBlockState();
			for (Direction d : Direction.Plane.HORIZONTAL) {
				long next = BlockPos.asLong(x + d.getStepX(), 0, z + d.getStepZ());
				if (bars.contains(next) || posts.contains(next)) {
					state = state.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(d), true);
				}
			}
			b.put(x, g + 1, z, state);
			if (roll > 0.3F) {
				b.put(x, g + 2, z, state); // the rest stand two high; some have lost their top
			}
		}
	}

	private void path(Build b, RandomSource random) {
		int gate = gateX(origin, width);
		int end = crypt ? stairsZ(origin, depth) - 1 : maxZ() - 1;
		for (int z = minZ(); z <= end; z++) {
			for (int x = gate - 1; x <= gate + 1; x++) {
				boolean skip = x != gate && random.nextFloat() < 0.5F;
				float r = random.nextFloat();
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE || skip) {
					continue;
				}
				BlockState block = r < 0.55F ? Blocks.COARSE_DIRT.defaultBlockState() : r < 0.75F ? Blocks.GRAVEL.defaultBlockState()
						: r < 0.9F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
				b.put(x, g, z, block);
				b.clearAbove(x, g, z, 2);
			}
		}
	}

	private void graves(Build b, RandomSource random) {
		int gate = gateX(origin, width);
		int lastHead = crypt ? stairsZ(origin, depth) - 1 : maxZ() - 2;
		java.util.List<int[]> slots = new java.util.ArrayList<>();
		for (int head = minZ() + 4; head <= lastHead; head += 4) {
			for (int x = minX() + 2; x <= maxX() - 2; x += 2) {
				if (Math.abs(x - gate) > 2) {
					slots.add(new int[] { x, head });
				}
			}
		}
		// One or two grave spots hold a tree instead (none in deserts), picked from the seed.
		Set<Long> treeSlots = new HashSet<>();
		RandomSource treeRandom = RandomSource.create(seed ^ 0x7EE5L);
		int treeCount = trees == YardTrees.NONE || slots.isEmpty() ? 0 : treeRandom.nextFloat() < 0.35F ? 2 : treeRandom.nextFloat() < 0.75F ? 1 : 0;
		for (int i = 0; i < treeCount; i++) {
			int[] slot = slots.get(treeRandom.nextInt(slots.size()));
			treeSlots.add(BlockPos.asLong(slot[0], 0, slot[1]));
		}
		for (int[] slot : slots) {
			if (treeSlots.contains(BlockPos.asLong(slot[0], 0, slot[1]))) {
				tree(b, slot[0], slot[1] - 1, RandomSource.create(seed ^ BlockPos.asLong(slot[0], 0, slot[1])));
			} else {
				grave(b, random, slot[0], slot[1]);
			}
		}
	}

	/**
	 * A yard tree at ({@code x}, {@code z}): an old tree of the yard's wood, built in code so every chunk builds the same
	 * one (a vanilla tree feature can't be clipped to the chunk being generated). Its height is reckoned from the yard's
	 * ground height; in the trunk's own chunk the trunk reaches down to the real ground.
	 */
	private void tree(Build b, int x, int z, RandomSource random) {
		BlockState log = trees.log();
		BlockState leaves = trees.leaves();
		int base = origin.getY() + 1;
		int height = trees.minHeight() + random.nextInt(3);
		int g = b.ground(x, z);
		if (g != Integer.MIN_VALUE) {
			b.put(x, g, z, Blocks.DIRT.defaultBlockState());
			for (int y = g + 1; y < base; y++) {
				b.put(x, y, z, log);
			}
		}
		for (int y = base; y < base + height; y++) {
			b.put(x, y, z, log);
		}
		int top = base + height;
		for (int dy = -trees.canopyDepth(); dy <= 1; dy++) {
			int r = trees.radiusAt(dy);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					boolean corner = Math.abs(dx) == r && Math.abs(dz) == r && r > 0;
					if ((dx == 0 && dz == 0 && dy < 0) || (corner && random.nextFloat() < 0.7F) || random.nextFloat() < 0.08F) {
						continue;
					}
					b.putIfAir(x + dx, top + dy, z + dz, leaves);
				}
			}
		}
	}

	/**
	 * One grave, all on one level (owner, 2026-11-22: the sleeper lies under both plot blocks, so they share a y): the
	 * stone at {@code head} facing north over its plot (the two blocks north of it), the sleeper one block under the
	 * plot. The level is the ground of the plot's first block; soil is filled under all three and the space above
	 * cleared. Plots are coarse or rooted dirt, which grass doesn't spread onto (owner). A grave that would cross a
	 * chunk border is left out (its level must be read from one column, which only one chunk can see).
	 */
	private void grave(Build b, RandomSource random, int x, int head) {
		boolean straddles = (head - 2) >> 4 != head >> 4;
		float condition = random.nextFloat();
		boolean sleeper = random.nextFloat() < GraveTuning.GRAVE_SLEEPER_CHANCE;
		boolean skeleton = random.nextFloat() < GraveTuning.SKELETON_SHARE;
		boolean buried = random.nextFloat() < GraveTuning.BURIED_LOOT_CHANCE;
		int itemRoll = random.nextInt(4);
		long plotSeed = random.nextLong();
		SignText text = GraveText.random(random, GraveTuning.BLANK_STONE_CHANCE);
		if (straddles) {
			return;
		}
		int py = b.ground(x, head - 1);
		if (py == Integer.MIN_VALUE) {
			return;
		}
		RandomSource plot = RandomSource.create(plotSeed);
		for (int z = head - 2; z <= head; z++) {
			b.put(x, py - 2, z, Blocks.DIRT.defaultBlockState());
			b.put(x, py - 1, z, Blocks.DIRT.defaultBlockState());
			b.clearAbove(x, py, z, 3);
		}
		b.put(x, py, head, Blocks.GRASS_BLOCK.defaultBlockState());
		// The stone: mostly upright, some turned askew, a few fallen (a stone brick slab in the grass).
		if (condition < 0.1F) {
			b.put(x, py + 1, head, Blocks.STONE_BRICK_SLAB.defaultBlockState());
		} else {
			int rotation = condition < 0.35F ? (plot.nextBoolean() ? 1 : 15) : 0;
			b.put(x, py + 1, head, ModBlocks.GRAVESTONE.defaultBlockState().setValue(StandingSignBlock.ROTATION, (8 + rotation) % 16));
			b.text(x, py + 1, head, text);
		}
		// The plot: two blocks of coarse or rooted dirt, some with weeds or a flower left on them.
		for (int z = head - 2; z <= head - 1; z++) {
			b.put(x, py, z, plot.nextFloat() < 0.65F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState());
			float plant = plot.nextFloat();
			if (plant < 0.15F) {
				b.put(x, py + 1, z, trees.flower(plot));
			} else if (plant < 0.4F) {
				b.put(x, py + 1, z, trees == YardTrees.NONE ? Blocks.DEAD_BUSH.defaultBlockState()
						: plot.nextBoolean() ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState());
			}
		}
		// The sleeper, under the plot, lying with its head at the stone.
		if (sleeper) {
			b.sleeper(x, py - 1, head - 1, skeleton ? UndeadVariant.SKELETON : UndeadVariant.ZOMBIE, SleeperPose.LYING, Direction.NORTH, plot, false);
		}
		// Something buried with them, showing in the plot's far end.
		if (buried) {
			ItemStack item = switch (itemRoll) {
				case 0 -> new ItemStack(Items.IRON_SWORD);
				case 1 -> new ItemStack(Items.STONE_AXE);
				case 2 -> new ItemStack(Items.IRON_SHOVEL);
				default -> new ItemStack(Items.BOW);
			};
			b.put(x, py + 1, head - 2, Blocks.AIR.defaultBlockState());
			b.display(x, py + 1, head - 2, DisplayPlacement.BURIED, Direction.NORTH, item);
		}
	}

	// --- the crypt ----------------------------------------------------------------------------------

	private void vault(Build b, RandomSource random) {
		int gate = gateX(origin, width);
		int top = stairsZ(origin, depth);
		int g = origin.getY();
		int floor = g - GraveTuning.CRYPT_DEPTH + 1;
		BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
		// Stairs going south, away from the gate, one block down per step: step i is a stair block at y = g - i (the first replaces the
		// ground), with three blocks of headroom, a brick floor under it, brick side walls, and a brick roof once the
		// headroom is below ground. The last step is at the vault's floor height; the landing after it is the doorway.
		int steps = g - floor;
		for (int i = 0; i <= steps; i++) {
			int z = top + i;
			int y = g - i;
			for (int x = gate - 2; x <= gate + 2; x++) {
				if (Math.abs(x - gate) == 2) {
					// Side walls down to the step; a low parapet only round the top two steps, so the rest reads as a
					// trench cut into the ground.
					int wallTop = i <= 1 ? g + 1 : g;
					for (int yy = y - 1; yy <= wallTop; yy++) {
						b.put(x, yy, z, bricks(random));
					}
					continue;
				}
				b.put(x, y - 1, z, bricks(random));
				b.put(x, y, z, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
				for (int yy = y + 1; yy <= y + 3; yy++) {
					b.put(x, yy, z, Blocks.AIR.defaultBlockState());
				}
				if (y + 4 <= g) {
					b.put(x, y + 4, z, bricks(random));
				}
			}
		}
		// Soul torches on top of the entrance's side walls.
		b.put(gate - 2, g + 2, top, Blocks.SOUL_TORCH.defaultBlockState());
		b.put(gate + 2, g + 2, top, Blocks.SOUL_TORCH.defaultBlockState());

		// The vault: VAULT_LENGTH long beyond the stairs' foot, 2*VAULT_HALF_WIDTH+1 wide, VAULT_HEIGHT high inside.
		int start = top + steps + 2; // the landing (start - 1) is the vault's doorway
		int end = start + VAULT_LENGTH - 1;
		for (int z = start - 1; z <= end + 1; z++) {
			for (int x = gate - VAULT_HALF_WIDTH - 1; x <= gate + VAULT_HALF_WIDTH + 1; x++) {
				for (int y = floor - 1; y <= floor + VAULT_HEIGHT; y++) {
					boolean shell = z == start - 1 || z == end + 1 || Math.abs(x - gate) == VAULT_HALF_WIDTH + 1 || y == floor - 1 || y == floor + VAULT_HEIGHT;
					if (shell) {
						boolean doorway = z == start - 1 && Math.abs(x - gate) <= 1 && y >= floor && y <= floor + 2;
						b.put(x, y, z, doorway ? Blocks.AIR.defaultBlockState() : y == floor - 1 ? floorBlock(random) : bricks(random));
					} else {
						b.put(x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		// Niches in both long walls, every other block: a sleeper lying in each (most of them).
		for (int z = start + 1; z <= end - 1; z += 2) {
			for (int side = -1; side <= 1; side += 2) {
				int wx = gate + side * (VAULT_HALF_WIDTH + 1);
				b.put(wx, floor, z, Blocks.AIR.defaultBlockState());
				b.put(wx + side, floor, z, bricks(random));
				b.put(wx, floor + 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
				if (random.nextFloat() < 0.8F) {
					UndeadVariant variant = random.nextFloat() < 0.6F ? UndeadVariant.SKELETON : UndeadVariant.ZOMBIE;
					b.sleeper(wx, floor, z, variant, SleeperPose.LYING, side < 0 ? Direction.EAST : Direction.WEST, random, true);
				}
			}
		}
		// The far end: a low altar with candles and the family's chest. Candlelight only (owner, 2026-11-22: crypts
		// shouldn't be well lit): the altar's candles are lit, a few more stand along the walls, most of them out.
		b.put(gate, floor, end, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		b.put(gate, floor + 1, end, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		b.put(gate - 1, floor, end, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
		b.loot(gate - 1, floor, end, GraveTuning.CRYPT_FAMILY_LOOT, random);
		b.put(gate + 1, floor, end, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + random.nextInt(2)).setValue(CandleBlock.LIT, true));
		for (int z = start + 2; z <= end - 2; z += 3) {
			for (int side = -1; side <= 1; side += 2) {
				if (random.nextFloat() < 0.6F) {
					b.put(gate + side * (VAULT_HALF_WIDTH - 1), floor, z, Blocks.CANDLE.defaultBlockState()
							.setValue(CandleBlock.CANDLES, 1 + random.nextInt(3)).setValue(CandleBlock.LIT, random.nextFloat() < 0.25F));
				}
			}
		}
		// Cobwebs in the corners.
		for (int i = 0; i < 4; i++) {
			int x = gate + (random.nextBoolean() ? -VAULT_HALF_WIDTH : VAULT_HALF_WIDTH);
			int z = start + random.nextInt(VAULT_LENGTH);
			b.put(x, floor + VAULT_HEIGHT - 1, z, Blocks.COBWEB.defaultBlockState());
		}
	}

	private static BlockState bricks(RandomSource random) {
		float r = random.nextFloat();
		return r < 0.18F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : r < 0.32F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
				: Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static BlockState floorBlock(RandomSource random) {
		return random.nextFloat() < 0.3F ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
	}

	// --- placing, clipped to the chunk being generated -------------------------------------------------

	/** Puts blocks only inside the chunk being generated; reads the ground the same way in every chunk. */
	private static final class Build {
		private final WorldGenLevel level;
		private final BoundingBox chunk;

		Build(WorldGenLevel level, BoundingBox chunk) {
			this.level = level;
			this.chunk = chunk;
		}

		boolean inside(int x, int y, int z) {
			return chunk.isInside(x, y, z);
		}

		/**
		 * The ground of this column: the top block that isn't a tree, a plant or air, or MIN_VALUE when the column isn't
		 * in this chunk or the ground is under water. Read the same way in every chunk (the yard is built after trees,
		 * which it clears).
		 */
		int ground(int x, int z) {
			if (x < chunk.minX() || x > chunk.maxX() || z < chunk.minZ() || z > chunk.maxZ()) {
				return Integer.MIN_VALUE;
			}
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
			while (pos.getY() > level.getMinY() && cover(level.getBlockState(pos))) {
				pos.move(0, -1, 0);
			}
			return level.getFluidState(pos).isEmpty() ? pos.getY() : Integer.MIN_VALUE;
		}

		/** Trees and plants standing on the ground (cleared out of a yard), and air. */
		static boolean cover(BlockState state) {
			return state.isAir() || state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.LEAVES)
					|| state.is(net.minecraft.tags.BlockTags.REPLACEABLE) || state.is(Blocks.VINE) || state.is(Blocks.BEE_NEST)
					|| state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
					|| state.is(net.minecraft.tags.BlockTags.FLOWERS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.PUMPKIN);
		}

		/** Clears trees and plants from the ground up to {@code height} above it, in this column. */
		void clearCover(int x, int z, int height) {
			int g = ground(x, z);
			if (g == Integer.MIN_VALUE) {
				return;
			}
			for (int y = g + 1; y <= g + height; y++) {
				if (inside(x, y, z) && !level.getBlockState(new BlockPos(x, y, z)).isAir() && cover(level.getBlockState(new BlockPos(x, y, z)))) {
					put(x, y, z, Blocks.AIR.defaultBlockState());
				}
			}
		}

		void put(int x, int y, int z, BlockState state) {
			if (inside(x, y, z)) {
				level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
			}
		}

		void putIfAir(int x, int y, int z, BlockState state) {
			if (inside(x, y, z) && level.getBlockState(new BlockPos(x, y, z)).isAir()) {
				put(x, y, z, state);
			}
		}

		void clearAbove(int x, int y, int z, int height) {
			for (int dy = 1; dy <= height; dy++) {
				if (inside(x, y + dy, z) && !level.getBlockState(new BlockPos(x, y + dy, z)).isAir()) {
					put(x, y + dy, z, Blocks.AIR.defaultBlockState());
				}
			}
		}

		void text(int x, int y, int z, SignText text) {
			if (inside(x, y, z) && level.getBlockEntity(new BlockPos(x, y, z)) instanceof GravestoneBlockEntity stone) {
				stone.carve(text, level.registryAccess());
			}
		}

		void sleeper(int x, int y, int z, UndeadVariant variant, SleeperPose pose, Direction facing, RandomSource random, boolean crypt) {
			if (!inside(x, y, z)) {
				return;
			}
			SleeperBlock block = ModBlocks.sleeper(variant);
			put(x, y, z, block.defaultBlockState().setValue(SleeperBlock.POSE, pose).setValue(SleeperBlock.FACING, facing));
			if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof SleeperBlockEntity sleeper) {
				if (variant == UndeadVariant.SKELETON && random.nextFloat() < 0.5F) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				} else if (variant == UndeadVariant.ZOMBIE && random.nextFloat() < 0.25F) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SHOVEL));
				}
				if (crypt && random.nextFloat() < 0.3F) {
					sleeper.setGear(EquipmentSlot.HEAD, new ItemStack(random.nextBoolean() ? Items.CHAINMAIL_HELMET : Items.IRON_HELMET));
				}
			}
		}

		void display(int x, int y, int z, DisplayPlacement placement, Direction facing, ItemStack item) {
			if (!inside(x, y, z)) {
				return;
			}
			put(x, y, z, ModBlocks.LOOT_DISPLAY.defaultBlockState().setValue(LootDisplayBlock.PLACEMENT, placement).setValue(LootDisplayBlock.FACING, facing));
			if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof LootDisplayBlockEntity display) {
				display.setItem(item);
			}
		}

		void loot(int x, int y, int z, net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table, RandomSource random) {
			if (inside(x, y, z)) {
				RandomizableContainer.setBlockEntityLootTable(level, random, new BlockPos(x, y, z), table);
			}
		}
	}
}

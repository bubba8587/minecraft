// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	/** Its state of breakdown (owner: kept, overgrown, sunken, collapsed, nearly gone). */
	private final YardKinds.State state;
	/** Its crypt's wealth (meaningful with a crypt). */
	private final YardKinds.Wealth wealth;
	/** Its climate: which undead lie here (husks in deserts, strays in snow). */
	private final YardKinds.Climate climate;

	public CryptYardPiece(long seed, BlockPos origin, int width, int depth, boolean crypt, YardTrees trees) {
		this(seed, origin, width, depth, crypt, trees, YardKinds.State.OVERGROWN, YardKinds.Wealth.FAMILY, YardKinds.Climate.TEMPERATE);
	}

	public CryptYardPiece(long seed, BlockPos origin, int width, int depth, boolean crypt, YardTrees trees, YardKinds.State state,
			YardKinds.Wealth wealth, YardKinds.Climate climate) {
		super(Graveyards.CRYPT_YARD_PIECE, 0, box(origin, width, depth, crypt, wealth));
		this.climate = climate;
		this.state = state;
		this.wealth = wealth;
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
		// Yards saved before step 2 had no state or wealth: they were overgrown family yards.
		this.state = YardKinds.State.values()[Math.floorMod(tag.getIntOr("State", YardKinds.State.OVERGROWN.ordinal()), YardKinds.State.values().length)];
		this.wealth = YardKinds.Wealth.values()[Math.floorMod(tag.getIntOr("Wealth", YardKinds.Wealth.FAMILY.ordinal()), YardKinds.Wealth.values().length)];
		this.climate = YardKinds.Climate.values()[Math.floorMod(tag.getIntOr("Climate", YardKinds.Climate.TEMPERATE.ordinal()), YardKinds.Climate.values().length)];
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Seed", seed);
		tag.putLong("Origin", origin.asLong());
		tag.putInt("Width", width);
		tag.putInt("Depth", depth);
		tag.putBoolean("Crypt", crypt);
		tag.putInt("Trees", trees.ordinal());
		tag.putInt("State", state.ordinal());
		tag.putInt("Wealth", wealth.ordinal());
		tag.putInt("Climate", climate.ordinal());
	}

	// --- the vault's size and place (beyond the stairs, under the ground south of the yard) -------------

	private static final int VAULT_HALF_WIDTH = 3;
	private static final int VAULT_LENGTH = 9;
	private static final int VAULT_HEIGHT = 4;
	/** The pauper's ossuary: a tunnel this long. */
	private static final int PAUPER_LENGTH = 11;
	/** The noble crypt: an entry hall this long, then a chamber this long, with side halls reaching this far out. */
	private static final int NOBLE_ENTRY = 4;
	private static final int NOBLE_CHAMBER = 9;
	private static final int NOBLE_HALF = 4;
	private static final int NOBLE_HALL_END = 11;

	private static int gateX(BlockPos origin, int width) {
		return origin.getX() + width / 2;
	}

	/** First stair (top step) z: four rows in from the far (south) fence. */
	private static int stairsZ(BlockPos origin, int depth) {
		return origin.getZ() + depth - 5;
	}

	/** How far the crypt reaches either side of the gate's line (outer walls included), by wealth. */
	private static int cryptHalfWidth(YardKinds.Wealth wealth) {
		return switch (wealth) {
			case PAUPER -> 3;
			case FAMILY -> VAULT_HALF_WIDTH + 2;
			case NOBLE -> NOBLE_HALL_END + 1;
		};
	}

	/** How long the crypt is beyond its doorway (outer wall included), by wealth. */
	private static int cryptLength(YardKinds.Wealth wealth) {
		return switch (wealth) {
			case PAUPER -> PAUPER_LENGTH + 1;
			case FAMILY -> VAULT_LENGTH + 1;
			case NOBLE -> NOBLE_ENTRY + NOBLE_CHAMBER + 1;
		};
	}

	private static BoundingBox box(BlockPos origin, int width, int depth, boolean crypt, YardKinds.Wealth wealth) {
		int minY = crypt ? origin.getY() - GraveTuning.CRYPT_DEPTH - 1 : origin.getY() - 4;
		int maxZ = crypt ? stairsZ(origin, depth) + GraveTuning.CRYPT_DEPTH + 1 + cryptLength(wealth) : origin.getZ() + depth - 1;
		int minX = crypt ? Math.min(origin.getX(), gateX(origin, width) - cryptHalfWidth(wealth)) : origin.getX();
		int maxX = crypt ? Math.max(origin.getX() + width - 1, gateX(origin, width) + cryptHalfWidth(wealth)) : origin.getX() + width - 1;
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
		fence(b, RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0xFE11CEL));
		path(b, RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0x9A7AL));
		graves(b, RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0x6A4E5L));
		if (crypt) {
			vault(b, RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0xC4F97L));
			wardStatues(b);
		}
	}

	// --- warding statues ------------------------------------------------------------------------------

	/** Bubba's Totems & Artifacts' statue block, looked up by id (absent without that mod). */
	private static final net.minecraft.resources.Identifier STATUE_ID =
			net.minecraft.resources.Identifier.fromNamespaceAndPath("bubbas_totems_artifacts", "totem_statue");

	/** One warding statue's floor cell and facing. */
	public record WardSpot(BlockPos pos, Direction facing) {
	}

	/** Where this crypt's warding statues stand, one roll per wing; empty without a crypt. */
	public List<WardSpot> wardSpots() {
		List<WardSpot> spots = new ArrayList<>();
		if (!crypt) {
			return spots;
		}
		RandomSource random = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0x3A2D5L);
		int gate = gateX(origin, width);
		int floor = origin.getY() - GraveTuning.CRYPT_DEPTH + 1;
		int start = stairsZ(origin, depth) + (origin.getY() - floor) + 2;
		switch (wealth) {
			case FAMILY -> {
				if (random.nextFloat() < GraveTuning.CRYPT_STATUE_FAMILY) {
					spots.add(new WardSpot(new BlockPos(gate, floor, start + VAULT_LENGTH / 2), Direction.NORTH));
				}
			}
			case PAUPER -> {
				if (random.nextFloat() < GraveTuning.CRYPT_STATUE_PAUPER) {
					spots.add(new WardSpot(new BlockPos(gate - 1, floor, start + PAUPER_LENGTH / 2), Direction.NORTH));
				}
			}
			case NOBLE -> {
				int chamber = start + NOBLE_ENTRY;
				int mid = chamber + NOBLE_CHAMBER / 2;
				if (random.nextFloat() < GraveTuning.CRYPT_STATUE_NOBLE) {
					spots.add(new WardSpot(new BlockPos(gate, floor, chamber + 1), Direction.NORTH));
				}
				for (int side = -1; side <= 1; side += 2) {
					if (random.nextFloat() < GraveTuning.CRYPT_STATUE_NOBLE) {
						spots.add(new WardSpot(new BlockPos(gate + side * (NOBLE_HALF + 4), floor, mid), side < 0 ? Direction.EAST : Direction.WEST));
					}
				}
			}
		}
		return spots;
	}

	/** The warding statue state facing {@code facing}, or null without Bubba's Totems & Artifacts. */
	public static @org.jspecify.annotations.Nullable BlockState wardStatue(Direction facing) {
		Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(STATUE_ID).orElse(null);
		if (block == null) {
			return null;
		}
		BlockState state = block.defaultBlockState();
		state = withValue(state, "totem", "warding");
		return withValue(state, "facing", facing.getSerializedName());
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, String name, String value) {
		@SuppressWarnings("unchecked")
		net.minecraft.world.level.block.state.properties.Property<T> property =
				(net.minecraft.world.level.block.state.properties.Property<T>) state.getBlock().getStateDefinition().getProperty(name);
		if (property == null) {
			return state;
		}
		return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
	}

	private void wardStatues(Build b) {
		for (WardSpot spot : wardSpots()) {
			BlockState statue = wardStatue(spot.facing());
			BlockPos pos = spot.pos();
			if (statue == null || !b.inside(pos.getX(), pos.getY(), pos.getZ())) {
				continue;
			}
			b.put(pos.getX(), pos.getY(), pos.getZ(), statue);
			if (wealth == YardKinds.Wealth.PAUPER) {
				b.put(pos.getX() + 1, pos.getY(), pos.getZ(), Blocks.AIR.defaultBlockState());
			}
			b.level().scheduleTick(pos, statue.getBlock(), 1);
		}
	}

	/** Whether this yard has stairs down to a crypt. */
	/** Where wisps lead: the top of the crypt stairs, or the gate without a crypt. */
	public BlockPos wispTarget() {
		return crypt ? new BlockPos(gateX(origin, width), origin.getY() + 1, stairsZ(origin, depth)) : gate().above();
	}

	public boolean hasCrypt() {
		return crypt;
	}

	/** This yard's own seed (everything in it is decided from it). */
	public long seed() {
		return seed;
	}

	/** A noble crypt's far wall, its middle at floor height; null for any other yard. */
	public @org.jspecify.annotations.Nullable BlockPos nobleFarWall() {
		if (!crypt || wealth != YardKinds.Wealth.NOBLE) {
			return null;
		}
		int floor = origin.getY() - GraveTuning.CRYPT_DEPTH + 1;
		int start = stairsZ(origin, depth) + (origin.getY() - floor) + 2;
		return new BlockPos(gateX(origin, width), floor, start + NOBLE_ENTRY + NOBLE_CHAMBER);
	}

	/** The middle of the vault's floor (feet height), for previews and tests; meaningful only with a crypt. */
	public BlockPos vaultCentre() {
		int floor = origin.getY() - GraveTuning.CRYPT_DEPTH + 1;
		int start = stairsZ(origin, depth) + (origin.getY() - floor) + 2;
		int middle = switch (wealth) {
			case PAUPER -> PAUPER_LENGTH / 2;
			case FAMILY -> VAULT_LENGTH / 2;
			case NOBLE -> NOBLE_ENTRY + NOBLE_CHAMBER / 2;
		};
		return new BlockPos(gateX(origin, width), floor, start + middle);
	}

	/** The gate's ground block (the middle of the north fence). */
	public BlockPos gate() {
		return new BlockPos(gateX(origin, width), origin.getY(), origin.getZ());
	}

	public YardKinds.State state() {
		return state;
	}

	public YardKinds.Climate climate() {
		return climate;
	}

	public YardKinds.Wealth wealth() {
		return wealth;
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
		int lanternSide = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0x1A7E2L).nextBoolean() ? 2 : -2;
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
			float mossRoll = random.nextFloat();
			float brokenRoll = random.nextFloat();
			float goneRoll = random.nextFloat();
			BlockState brick = mossRoll < state.mossyPosts ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
					: brokenRoll < state.brokenPosts ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
			int g = b.ground(x, z);
			if (g == Integer.MIN_VALUE || goneRoll < state.postsGone) {
				continue;
			}
			b.put(x, g + 1, z, brick);
			if (brokenRoll < state.brokenPosts) {
				continue; // broken off: one high, no lantern
			}
			b.put(x, g + 2, z, brick);
			// One soul lantern in the whole yard (owner, 2026-11-22), on one gate post (which one is the seed's); none
			// left in a yard that's nearly gone.
			if (z == minZ() && x == gate + lanternSide && state != YardKinds.State.NEARLY_GONE) {
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
			if (roll < state.barsGone) {
				continue; // a gap
			}
			BlockState state = Blocks.IRON_BARS.defaultBlockState();
			for (Direction d : Direction.Plane.HORIZONTAL) {
				long next = BlockPos.asLong(x + d.getStepX(), 0, z + d.getStepZ());
				if (bars.contains(next) || posts.contains(next)) {
					state = state.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(d), true);
				}
			}
			b.put(x, g + 1, z, state);
			if (roll >= this.state.barsGone + this.state.oneHigh) {
				b.put(x, g + 2, z, state); // the rest stand two high; some have lost their top
			}
		}
	}

	private void path(Build b, RandomSource random) {
		int gate = gateX(origin, width);
		int end = crypt ? stairsZ(origin, depth) - 1 : maxZ() - 1;
		for (int z = minZ(); z <= end; z++) {
			for (int x = gate - 1; x <= gate + 1; x++) {
				float edgeRoll = random.nextFloat();
				float r = random.nextFloat();
				boolean skip = switch (state) {
					case KEPT -> false;
					case NEARLY_GONE -> true;
					case COLLAPSED -> r < 0.5F;
					default -> x != gate && edgeRoll < 0.5F;
				};
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE || skip) {
					continue;
				}
				BlockState block = switch (state) {
					case KEPT -> x == gate ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
					case SUNKEN -> r < 0.5F ? Blocks.MUD.defaultBlockState() : r < 0.8F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
					case COLLAPSED -> r < 0.7F ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
					default -> r < 0.55F ? Blocks.COARSE_DIRT.defaultBlockState() : r < 0.75F ? Blocks.GRAVEL.defaultBlockState()
							: r < 0.9F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
				};
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
				// A lone family plot (9 wide or less) has no path between its graves.
				if (width <= 9 || Math.abs(x - gate) > 2) {
					slots.add(new int[] { x, head });
				}
			}
		}
		// One or two grave spots hold a tree instead (none in deserts), picked from the seed.
		Set<Long> treeSlots = new HashSet<>();
		RandomSource treeRandom = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0x7EE5L);
		int treeCount = trees == YardTrees.NONE || slots.isEmpty() ? 0 : treeRandom.nextFloat() < 0.35F ? 2 : treeRandom.nextFloat() < 0.75F ? 1 : 0;
		for (int i = 0; i < treeCount; i++) {
			int[] slot = slots.get(treeRandom.nextInt(slots.size()));
			treeSlots.add(BlockPos.asLong(slot[0], 0, slot[1]));
		}
		for (int[] slot : slots) {
			if (treeSlots.contains(BlockPos.asLong(slot[0], 0, slot[1]))) {
				tree(b, slot[0], slot[1] - 1, RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ BlockPos.asLong(slot[0], 0, slot[1])));
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
		random.nextFloat(); // was the skeleton or zombie roll; kept so the rolls after it stay the same
		boolean buried = random.nextFloat() < GraveTuning.BURIED_LOOT_CHANCE;
		int itemRoll = random.nextInt(4);
		long plotSeed = random.nextLong();
		SignText text = GraveText.random(random, GraveTuning.BLANK_STONE_CHANCE);
		float stoneRoll = random.nextFloat();
		float openRoll = random.nextFloat();
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
		// The stone: by the yard's state, gone, fallen (a stone brick slab in the grass), turned askew or upright.
		if (stoneRoll < state.stonesGone) {
			// nothing left of it
		} else if (condition < state.fallen) {
			b.put(x, py + 1, head, Blocks.STONE_BRICK_SLAB.defaultBlockState());
		} else {
			int rotation = condition < state.fallen + state.askew ? (plot.nextBoolean() ? 1 : 15) : 0;
			b.put(x, py + 1, head, ModBlocks.GRAVESTONE.defaultBlockState().setValue(StandingSignBlock.ROTATION, (8 + rotation) % 16));
			b.text(x, py + 1, head, text);
		}
		// The plot: two blocks of coarse or rooted dirt, some with weeds or a flower left on them. A yard nearly gone has
		// grassed its plots over; a sunken grave has settled a block (a hollow); a collapsed yard has open graves.
		boolean open = openRoll < state.openGraves;
		int sleeperY = state == YardKinds.State.SUNKEN ? py - 2 : py - 1;
		for (int z = head - 2; z <= head - 1; z++) {
			float soil = plot.nextFloat();
			float plant = plot.nextFloat();
			if (state == YardKinds.State.NEARLY_GONE) {
				continue; // grass over it, as it was
			}
			if (open) {
				b.put(x, py, z, Blocks.AIR.defaultBlockState());
				continue;
			}
			if (state == YardKinds.State.SUNKEN) {
				b.put(x, py, z, Blocks.AIR.defaultBlockState());
				b.put(x, py - 1, z, soil < 0.6F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState());
				continue;
			}
			b.put(x, py, z, soil < 0.65F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState());
			if (plant < state.flowers) {
				b.put(x, py + 1, z, trees.flower(plot));
			} else if (plant < state.flowers + 0.25F) {
				b.put(x, py + 1, z, trees == YardTrees.NONE ? Blocks.DEAD_BUSH.defaultBlockState()
						: plot.nextBoolean() ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState());
			}
		}
		// The sleeper, under the plot, lying with its head at the stone.
		if (sleeper) {
			// The resting dead (owner, 2026-11-22): a yard grave holds a skeleton that never wakes, often in dyed leather.
			b.resting(x, sleeperY, head - 1, climate.resting(), plot);
		}
		// Something buried with them, showing in the plot's far end.
		if (buried && !open && state != YardKinds.State.SUNKEN) {
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

		int start = top + steps + 2; // the landing (start - 1) is the crypt's doorway
		switch (wealth) {
			case PAUPER -> pauper(b, random, gate, floor, start);
			case NOBLE -> noble(b, random, gate, floor, start);
			case FAMILY -> family(b, random, gate, floor, start);
		}
	}

	/** The family vault (the first crypt): VAULT_LENGTH long, 2*VAULT_HALF_WIDTH+1 wide, VAULT_HEIGHT high inside. */
	private void family(Build b, RandomSource random, int gate, int floor, int start) {
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
				b.put(wx + side, floor, z, Blocks.AIR.defaultBlockState());
				b.put(wx + side * 2, floor, z, bricks(random));
				b.put(wx, floor + 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
				b.put(wx + side, floor + 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
				if (random.nextFloat() < 0.8F) {
					UndeadVariant variant = climate.crypt(random.nextFloat(), 0.6F);
					b.sleeper(wx, floor, z, variant, SleeperPose.LYING, side < 0 ? Direction.EAST : Direction.WEST, random, true);
					b.encase(wx, floor, z, variant);
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

	/**
	 * The pauper's ossuary: a cramped tunnel dug through the ground, three wide and three high, walled in cobblestone
	 * and packed dirt with bone blocks set in, lined both sides with niches nearly every block, nearly all holding a
	 * sleeper with little or nothing on them. A candle or two, most out; one chest of poor things at the end.
	 */
	private void pauper(Build b, RandomSource random, int gate, int floor, int start) {
		int end = start + PAUPER_LENGTH - 1;
		for (int z = start - 1; z <= end + 1; z++) {
			for (int x = gate - 3; x <= gate + 3; x++) {
				for (int y = floor - 1; y <= floor + 3; y++) {
					boolean inside = Math.abs(x - gate) <= 1 && y >= floor && y <= floor + 2 && z >= start && z <= end;
					boolean doorway = z == start - 1 && Math.abs(x - gate) <= 1 && y >= floor && y <= floor + 2;
					BlockState shell = y == floor - 1 ? (random.nextFloat() < 0.5F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState())
							: ossuaryWall(random);
					b.put(x, y, z, inside || doorway ? Blocks.AIR.defaultBlockState() : shell);
				}
			}
		}
		// Niches: both walls, every block but the first and last, at the floor and (now and then) one above it.
		for (int z = start + 1; z <= end - 1; z++) {
			for (int side = -1; side <= 1; side += 2) {
				for (int level = 0; level <= 1; level++) {
					float roll = random.nextFloat();
					float who = random.nextFloat();
					if (level == 1 && roll > 0.35F) {
						continue;
					}
					int wx = gate + side * 2;
					int y = floor + level * 2;
					b.put(wx, y, z, Blocks.AIR.defaultBlockState());
					b.put(wx + side, y, z, Blocks.AIR.defaultBlockState());
					if (roll < 0.85F || level == 1) {
						UndeadVariant variant = climate.crypt(who, 0.7F);
						b.sleeper(wx, y, z, variant, SleeperPose.LYING, side < 0 ? Direction.EAST : Direction.WEST, random, false, 0.4F);
						b.encase(wx, y, z, variant);
					}
				}
			}
		}
		b.put(gate, floor, end, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
		b.loot(gate, floor, end, GraveTuning.CRYPT_PAUPER_LOOT, random);
		b.put(gate - 1, floor, end, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1).setValue(CandleBlock.LIT, true));
		b.put(gate + 1, floor, start + PAUPER_LENGTH / 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, false));
		for (int i = 0; i < 5; i++) {
			int x = gate + random.nextInt(3) - 1;
			int z = start + random.nextInt(PAUPER_LENGTH);
			b.putIfAir(x, floor + 2, z, Blocks.COBWEB.defaultBlockState());
		}
		for (int i = 0; i < 4; i++) {
			int x = gate + random.nextInt(3) - 1;
			int z = start + random.nextInt(PAUPER_LENGTH);
			b.putIfAir(x, floor, z, random.nextBoolean() ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.CANDLE.defaultBlockState());
		}
	}

	private static BlockState ossuaryWall(RandomSource random) {
		float r = random.nextFloat();
		return r < 0.4F ? Blocks.COBBLESTONE.defaultBlockState() : r < 0.65F ? Blocks.PACKED_MUD.defaultBlockState()
				: r < 0.85F ? Blocks.COARSE_DIRT.defaultBlockState() : r < 0.95F ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
	}

	/**
	 * The noble crypt, in polished and chiseled stone: an entry hall, then a pillared chamber with a sarcophagus in the
	 * middle (an armoured sleeper under a stone lid) and the family's two chests at the far wall, and from the chamber's
	 * middle a side hall each way lined with niches of armoured sleepers. Candles only.
	 */
	private void noble(Build b, RandomSource random, int gate, int floor, int start) {
		int chamber = start + NOBLE_ENTRY;
		int end = chamber + NOBLE_CHAMBER - 1;
		int mid = chamber + NOBLE_CHAMBER / 2;
		// The shell: everything from the doorway to the far wall, the side halls included, solid to begin with.
		for (int z = start - 1; z <= end + 1; z++) {
			for (int x = gate - NOBLE_HALL_END - 1; x <= gate + NOBLE_HALL_END + 1; x++) {
				boolean inHall = Math.abs(z - mid) <= 2 && Math.abs(x - gate) > NOBLE_HALF;
				boolean inBody = z >= start - 1 && (z < chamber ? Math.abs(x - gate) <= 2 : Math.abs(x - gate) <= NOBLE_HALF + 1);
				if (!inHall && !inBody) {
					continue;
				}
				for (int y = floor - 1; y <= floor + VAULT_HEIGHT; y++) {
					b.put(x, y, z, y == floor - 1 ? nobleFloor(random) : nobleWall(random));
				}
			}
		}
		// Hollow it out: the entry hall (3 wide), the chamber (9 wide), the side halls (3 wide).
		for (int z = start - 1; z <= end; z++) {
			int half = z < chamber ? 1 : NOBLE_HALF;
			int top = z == start - 1 ? floor + 2 : floor + VAULT_HEIGHT - 1;
			for (int x = gate - half; x <= gate + half; x++) {
				for (int y = floor; y <= top; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (int x = gate - NOBLE_HALL_END + 1; x <= gate + NOBLE_HALL_END - 1; x++) {
			if (Math.abs(x - gate) <= NOBLE_HALF) {
				continue;
			}
			for (int z = mid - 1; z <= mid + 1; z++) {
				for (int y = floor; y <= floor + VAULT_HEIGHT - 2; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
			}
		}
		// Pillars at the chamber's corners, chiseled.
		for (int dx = -NOBLE_HALF + 1; dx <= NOBLE_HALF - 1; dx += 2 * (NOBLE_HALF - 1)) {
			for (int z : new int[] { chamber + 1, end - 1 }) {
				for (int y = floor; y < floor + VAULT_HEIGHT; y++) {
					b.put(gate + dx, y, z, y == floor || y == floor + VAULT_HEIGHT - 1 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
							: Blocks.POLISHED_ANDESITE.defaultBlockState());
				}
				b.put(gate + dx + (dx < 0 ? 1 : -1), floor, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2 + random.nextInt(3))
						.setValue(CandleBlock.LIT, random.nextFloat() < 0.6F));
			}
		}
		// The sarcophagus: a stone box three long, an armoured sleeper inside, a slab lid (upper half: the candle stands on it).
		for (int z = mid - 1; z <= mid + 1; z++) {
			b.put(gate - 1, floor, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
			b.put(gate + 1, floor, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
			b.put(gate, floor + 1, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
		}
		b.put(gate, floor, mid - 1, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		b.put(gate, floor, mid + 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
		b.sleeper(gate, floor, mid, climate.crypt(random.nextFloat(), 0.6F), SleeperPose.LYING, Direction.NORTH,
				random, true, 1.0F);
		b.put(gate, floor + 2, mid - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		// The family's chests at the far wall.
		for (int side = -1; side <= 1; side += 2) {
			b.put(gate + side * 2, floor, end, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
			b.loot(gate + side * 2, floor, end, GraveTuning.CRYPT_NOBLE_LOOT, random);
		}
		b.put(gate, floor, end, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		b.put(gate, floor + 1, end, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		// Side halls: niches in both long walls every other block, an armoured sleeper in most.
		for (int side = -1; side <= 1; side += 2) {
			for (int k = NOBLE_HALF + 2; k <= NOBLE_HALL_END - 1; k += 2) {
				int x = gate + side * k;
				for (int wz : new int[] { mid - 2, mid + 2 }) {
					float roll = random.nextFloat();
					int back = wz < mid ? wz - 1 : wz + 1;
					b.put(x, floor, wz, Blocks.AIR.defaultBlockState());
					b.put(x, floor, back, Blocks.AIR.defaultBlockState());
					b.put(x, floor + 1, wz, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState());
					b.put(x, floor + 1, back, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState());
					if (roll < 0.85F) {
						b.sleeper(x, floor, wz, climate.crypt(random.nextFloat(), 0.5F), SleeperPose.LYING,
								wz < mid ? Direction.SOUTH : Direction.NORTH, random, true, 0.85F);
					}
				}
			}
			b.put(gate + side * (NOBLE_HALL_END - 1), floor, mid, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + random.nextInt(2))
					.setValue(CandleBlock.LIT, random.nextBoolean()));
		}
		for (int i = 0; i < 6; i++) {
			int x = gate + random.nextInt(2 * NOBLE_HALF + 1) - NOBLE_HALF;
			int z = chamber + random.nextInt(NOBLE_CHAMBER);
			b.putIfAir(x, floor + VAULT_HEIGHT - 1, z, Blocks.COBWEB.defaultBlockState());
		}
	}

	private static BlockState nobleWall(RandomSource random) {
		float r = random.nextFloat();
		return r < 0.55F ? Blocks.POLISHED_ANDESITE.defaultBlockState() : r < 0.8F ? Blocks.STONE_BRICKS.defaultBlockState()
				: r < 0.92F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
	}

	private static BlockState nobleFloor(RandomSource random) {
		return random.nextFloat() < 0.8F ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState();
	}

	/** Muted burial colours for the resting dead's leather: undyed brown, grey, charcoal, ash, a dark green, a dull blue, black, linen. */
	static final int[] BURIAL_COLOURS = { 0xA06540, 0x6B6B6B, 0x3A3A3A, 0x9D9D97, 0x3F5133, 0x3C4A63, 0x1D1D21, 0xC8BBA0 };

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
	private static final class Build extends io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild {
		Build(WorldGenLevel level, BoundingBox chunk) {
			super(level, chunk);
		}

		void text(int x, int y, int z, SignText text) {
			if (inside(x, y, z) && level.getBlockEntity(new BlockPos(x, y, z)) instanceof GravestoneBlockEntity stone) {
				stone.carve(text, level.registryAccess());
			}
		}

		void sleeper(int x, int y, int z, UndeadVariant variant, SleeperPose pose, Direction facing, RandomSource random, boolean crypt) {
			sleeper(x, y, z, variant, pose, facing, random, crypt, crypt ? 0.3F : 0.0F);
		}

		/**
		 * A sleeper with gear: a bow or a tool as before, and with chance {@code armour} armour (a helmet; at 0.8 and up a
		 * noble's full set of iron, chain or now and then gold, ).
		 */
		void sleeper(int x, int y, int z, UndeadVariant variant, SleeperPose pose, Direction facing, RandomSource random, boolean crypt, float armour) {
			float weaponRoll = random.nextFloat();
			float weaponKind = random.nextFloat();
			float armourRoll = random.nextFloat();
			float metal = random.nextFloat();
			float enchant = random.nextFloat();
			if (!inside(x, y, z)) {
				return;
			}
			SleeperBlock block = ModBlocks.sleeper(variant);
			put(x, y, z, block.defaultBlockState().setValue(SleeperBlock.POSE, pose).setValue(SleeperBlock.FACING, facing));
			if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof SleeperBlockEntity sleeper) {
				boolean noble = armour >= 0.8F;
				if (variant.isSkeleton() && weaponRoll < 0.5F) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				} else if (!variant.isSkeleton() && weaponRoll < (noble ? 0.6F : 0.25F)) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(noble ? (weaponKind < 0.5F ? Items.IRON_SWORD : Items.IRON_AXE)
							: weaponKind < 0.5F ? Items.STONE_SWORD : Items.IRON_SHOVEL));
				}
				if (armourRoll < armour) {
					if (noble) {
						boolean gold = metal < 0.15F;
						boolean chain = metal >= 0.15F && metal < 0.55F;
						sleeper.setGear(EquipmentSlot.HEAD, new ItemStack(gold ? Items.GOLDEN_HELMET : chain ? Items.CHAINMAIL_HELMET : Items.IRON_HELMET));
						sleeper.setGear(EquipmentSlot.CHEST, new ItemStack(gold ? Items.GOLDEN_CHESTPLATE : chain ? Items.CHAINMAIL_CHESTPLATE : Items.IRON_CHESTPLATE));
						sleeper.setGear(EquipmentSlot.LEGS, new ItemStack(gold ? Items.GOLDEN_LEGGINGS : chain ? Items.CHAINMAIL_LEGGINGS : Items.IRON_LEGGINGS));
						sleeper.setGear(EquipmentSlot.FEET, new ItemStack(gold ? Items.GOLDEN_BOOTS : chain ? Items.CHAINMAIL_BOOTS : Items.IRON_BOOTS));
					} else {
						sleeper.setGear(EquipmentSlot.HEAD, new ItemStack(metal < 0.5F ? Items.CHAINMAIL_HELMET : Items.IRON_HELMET));
					}
				}
				if (noble) {
					NobleFinery.apply(level.getLevel(), sleeper, new BlockPos(x, y, z), seedOf(x, y, z));
				}
			}
		}

		private static long seedOf(int x, int y, int z) {
			return BlockPos.asLong(x, y, z) * 0x9E3779B97F4A7C15L + 0x6E0B1EL;
		}

		/**
		 * Covers a crypt niche's sleeper with what it lies in: sand over a husk, clear ice over a stray (SleeperBreakout
		 * clears it when it wakes). Others keep their slab.
		 */
		void encase(int x, int y, int z, UndeadVariant variant) {
			if (variant == UndeadVariant.HUSK) {
				put(x, y + 1, z, Blocks.SAND.defaultBlockState());
			} else if (variant == UndeadVariant.STRAY) {
				put(x, y + 1, z, Blocks.ICE.defaultBlockState()); // clear: you see it frozen inside
			}
		}

		/**
		 * One of the resting dead: a skeleton kind (a skeleton, or a husk in the desert, a stray in snow) lying in a yard
		 * grave that never wakes, usually in muted dyed leather.
		 */
		void resting(int x, int y, int z, UndeadVariant variant, RandomSource random) {
			float dressed = random.nextFloat();
			int colour = BURIAL_COLOURS[random.nextInt(BURIAL_COLOURS.length)];
			float legs = random.nextFloat();
			float boots = random.nextFloat();
			float bow = random.nextFloat();
			if (!inside(x, y, z)) {
				return;
			}
			put(x, y, z, ModBlocks.sleeper(variant).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.LYING)
					.setValue(SleeperBlock.FACING, Direction.NORTH).setValue(SleeperBlock.RESTING, true));
			if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof SleeperBlockEntity sleeper) {
				if (dressed < GraveTuning.RESTING_LEATHER_CHANCE) {
					sleeper.setGear(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, colour));
					sleeper.setGear(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, colour));
					if (legs < 0.6F) {
						sleeper.setGear(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, colour));
					}
					if (boots < 0.4F) {
						sleeper.setGear(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, colour));
					}
				}
				if (bow < 0.15F && variant.isSkeleton()) {
					sleeper.setGear(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				}
			}
		}

		private static ItemStack dyed(net.minecraft.world.item.Item item, int rgb) {
			ItemStack stack = new ItemStack(item);
			stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(rgb));
			return stack;
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

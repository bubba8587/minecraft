// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.traderpost;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One trader post, built in code (specs/monster-camps.md "Trader posts"). Laid out in a local frame round its centre:
 * u across (right), w toward the front (the path out). Everything is decided from the seed before any block is read.
 */
public class TraderPostPiece extends StructurePiece {
	/** Plaza half-width across, and its back and front edges (local). */
	static final int HALF_U = 9;
	static final int BACK_W = -8;
	static final int FRONT_W = 10;
	/** Blocks of sloped ground round the plaza. */
	static final int SKIRT = 4;
	private static final int REACH = 15;

	private final long seed;
	private final BlockPos centre;
	private final Plan plan;

	public TraderPostPiece(long seed, BlockPos centre) {
		super(TraderPosts.PIECE, 0, box(centre));
		this.seed = seed;
		this.centre = centre;
		this.plan = new Plan(seed);
	}

	public TraderPostPiece(CompoundTag tag) {
		super(TraderPosts.PIECE, tag);
		this.seed = tag.getLongOr("Seed", 0L);
		this.centre = BlockPos.of(tag.getLongOr("Centre", 0L));
		this.plan = new Plan(seed);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Seed", seed);
		tag.putLong("Centre", centre.asLong());
	}

	static BoundingBox box(BlockPos c) {
		return new BoundingBox(c.getX() - REACH, c.getY() - 8, c.getZ() - REACH, c.getX() + REACH, c.getY() + 16, c.getZ() + REACH);
	}

	public BlockPos centre() {
		return centre;
	}

	public long seed() {
		return seed;
	}

	public Direction front() {
		return plan.front;
	}

	public boolean hasWagon() {
		return plan.wagon;
	}

	public int penCount() {
		return plan.pens.size();
	}

	// --- local frame -------------------------------------------------------------------------------

	private Direction right() {
		return plan.mirror ? plan.front.getCounterClockWise() : plan.front.getClockWise();
	}

	int wx(int u, int w) {
		return centre.getX() + right().getStepX() * u + plan.front.getStepX() * w;
	}

	int wz(int u, int w) {
		return centre.getZ() + right().getStepZ() * u + plan.front.getStepZ() * w;
	}

	/** The world block at local (u, dy above the plaza floor, w). */
	public BlockPos at(int u, int dy, int w) {
		return new BlockPos(wx(u, w), centre.getY() + dy, wz(u, w));
	}

	/** Local u of a world column. */
	private int lu(int x, int z) {
		Direction r = right();
		return r.getStepX() * (x - centre.getX()) + r.getStepZ() * (z - centre.getZ());
	}

	private int lw(int x, int z) {
		return plan.front.getStepX() * (x - centre.getX()) + plan.front.getStepZ() * (z - centre.getZ());
	}

	private Direction back() {
		return plan.front.getOpposite();
	}

	/** Local +u, or -u for a mirrored piece (pens on the right are built mirrored). */
	private Direction side(int sign) {
		return sign > 0 ? right() : right().getOpposite();
	}

	// --- the plan ------------------------------------------------------------------------------------

	/** One piece's ground cells, in world columns (inclusive). */
	public record Footprint(String what, int minX, int minZ, int maxX, int maxZ) {
		public boolean clearOf(Footprint other, int margin) {
			return minX > other.maxX + margin || other.minX > maxX + margin || minZ > other.maxZ + margin || other.minZ > maxZ + margin;
		}
	}

	private record Rect(String what, int u0, int w0, int u1, int w1) {
	}

	/** Every piece's footprint (world columns), for tests. */
	public List<Footprint> footprints() {
		List<Footprint> out = new ArrayList<>();
		for (Rect r : plan.rects) {
			int ax = wx(r.u0, r.w0);
			int az = wz(r.u0, r.w0);
			int bx = wx(r.u1, r.w1);
			int bz = wz(r.u1, r.w1);
			out.add(new Footprint(r.what, Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)));
		}
		return out;
	}

	/** The plaza (flat floor) in world columns. */
	public Footprint plaza() {
		int ax = wx(-HALF_U, BACK_W);
		int az = wz(-HALF_U, BACK_W);
		int bx = wx(HALF_U, FRONT_W);
		int bz = wz(HALF_U, FRONT_W);
		return new Footprint("plaza", Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz));
	}

	private static final class Plan {
		final Direction front;
		final boolean mirror;
		final DyeColor primary;
		final DyeColor secondary;
		final DyeColor[] stripes;
		/** Pen sides: -1 left, +1 right. */
		final List<Integer> pens = new ArrayList<>();
		final boolean wagon;
		final List<Rect> rects = new ArrayList<>();

		Plan(long seed) {
			RandomSource random = RandomSource.create(RandomSupport.mixStafford13(seed));
			front = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			mirror = random.nextBoolean();
			boolean blue = random.nextBoolean();
			primary = blue ? DyeColor.BLUE : DyeColor.RED;
			secondary = blue ? DyeColor.RED : DyeColor.BLUE;
			stripes = switch (random.nextInt(3)) {
				case 0 -> new DyeColor[] { primary, DyeColor.WHITE, secondary, DyeColor.WHITE };
				case 1 -> new DyeColor[] { primary, secondary };
				default -> new DyeColor[] { DyeColor.LIGHT_BLUE, DyeColor.WHITE, DyeColor.RED, DyeColor.WHITE };
			};
			pens.add(-1);
			if (random.nextFloat() < 0.6F) {
				pens.add(1);
			}
			wagon = random.nextFloat() < 0.6F;
			rects.add(new Rect("stall", -3, -7, 3, -3));
			rects.add(new Rect("fire", -2, 1, 2, 3));
			for (int s : pens) {
				rects.add(new Rect("pen", s * 5, -3, s * 9, 3));
			}
			if (pens.size() == 1) {
				rects.add(new Rect("cargo", 5, -2, 7, 0));
			}
			rects.add(new Rect("rail", 4, 6, 8, 7));
			rects.add(new Rect("signpost", 2, 6, 2, 8));
			if (wagon) {
				rects.add(new Rect("wagon", -8, 5, -4, 8));
			}
			rects.add(new Rect("banner", -2, FRONT_W, -2, FRONT_W));
			rects.add(new Rect("banner", 2, FRONT_W, 2, FRONT_W));
		}

		DyeColor stripe(int u) {
			return stripes[Math.floorMod(u, stripes.length)];
		}
	}

	// --- spots for arrivals and tests ------------------------------------------------------------

	/** Where the trader arrives: behind the counter. */
	public BlockPos traderSpot() {
		return at(0, 1, -5);
	}

	/** Llama spots: two per post, in the pens (both in the one pen when there is one). */
	public List<BlockPos> llamaSpots() {
		List<BlockPos> spots = new ArrayList<>();
		if (plan.pens.size() == 1) {
			int s = plan.pens.getFirst();
			spots.add(at(s * 7, 1, 0));
			spots.add(at(s * 6, 1, -1));
		} else {
			for (int s : plan.pens) {
				spots.add(at(s * 7, 1, 0));
			}
		}
		return spots;
	}

	/** The fence each llama spot is tethered to (same order as {@link #llamaSpots}). */
	public List<BlockPos> tetherPosts() {
		List<BlockPos> posts = new ArrayList<>();
		if (plan.pens.size() == 1) {
			int s = plan.pens.getFirst();
			posts.add(at(s * 9, 1, 0));
			posts.add(at(s * 9, 1, -1));
		} else {
			for (int s : plan.pens) {
				posts.add(at(s * 9, 1, 0));
			}
		}
		return posts;
	}

	/** The pen gates (fence gate blocks). */
	public List<BlockPos> gates() {
		List<BlockPos> out = new ArrayList<>();
		for (int s : plan.pens) {
			out.add(at(s * 5, 1, 0));
		}
		return out;
	}

	/** Where a traveller walks in: the path at the front edge. */
	public BlockPos entrance() {
		return at(0, 1, FRONT_W + 2);
	}

	/** Each pen's outside, in front of its gate. */
	public List<BlockPos> gateFronts() {
		List<BlockPos> out = new ArrayList<>();
		for (int s : plan.pens) {
			out.add(at(s * 4, 1, 0));
		}
		return out;
	}

	/** The pens' insides (world boxes, a block above the floor), in {@link #llamaSpots} order of pens. */
	public List<net.minecraft.world.phys.AABB> penInsides() {
		List<net.minecraft.world.phys.AABB> out = new ArrayList<>();
		for (int s : plan.pens) {
			BlockPos a = at(s * 6, 1, -2);
			BlockPos b = at(s * 8, 1, 2);
			out.add(new net.minecraft.world.phys.AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
					Math.max(a.getX(), b.getX()) + 1, a.getY() + 2, Math.max(a.getZ(), b.getZ()) + 1));
		}
		return out;
	}

	// --- building ------------------------------------------------------------------------------------

	private final class Build {
		final ClippedBuild b;
		final int y0 = centre.getY();
		final HolderLookup.Provider registries;
		boolean sandy;

		Build(ClippedBuild b) {
			this.b = b;
			this.registries = b.level().registryAccess();
		}

		void put(int u, int dy, int w, BlockState state) {
			b.put(wx(u, w), y0 + dy, wz(u, w), state);
		}

		BlockState get(int u, int dy, int w) {
			return b.level().getBlockState(at(u, dy, w));
		}

		boolean inside(int u, int dy, int w) {
			return b.inside(wx(u, w), y0 + dy, wz(u, w));
		}

		float roll(int u, int w, long salt) {
			return RandomSource.create(seed ^ salt ^ (u * 73428767L) ^ (w * 912931L)).nextFloat();
		}
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource chunkRandom, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos) {
		Build k = new Build(new ClippedBuild(level, chunkBB));
		ground(k);
		stall(k);
		fire(k);
		for (int s : plan.pens) {
			pen(k, s);
		}
		if (plan.pens.size() == 1) {
			cargo(k);
		}
		rail(k);
		signpost(k);
		if (plan.wagon) {
			wagon(k);
		}
		entranceBanners(k);
		k.b.tidyFences(getBoundingBox());
	}

	/** Levels the plaza to the centre's height, slopes the skirt a block per block, clears trees and plants, treads the ground. */
	private void ground(Build k) {
		ClippedBuild b = k.b;
		BlockState centreTop = null;
		for (int x = centre.getX() - REACH; x <= centre.getX() + REACH; x++) {
			for (int z = centre.getZ() - REACH; z <= centre.getZ() + REACH; z++) {
				if (!b.inside(x, centre.getY(), z)) {
					continue;
				}
				int u = lu(x, z);
				int w = lw(x, z);
				int du = Math.max(0, Math.abs(u) - HALF_U);
				int dw = w < BACK_W ? BACK_W - w : Math.max(0, w - FRONT_W);
				int d = Math.max(du, dw);
				if (d > SKIRT) {
					continue;
				}
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				int t = d == 0 ? k.y0 : Math.clamp(g, k.y0 - d, k.y0 + d);
				BlockState top = b.level().getBlockState(new BlockPos(x, g, z));
				boolean sand = top.is(BlockTags.SAND);
				boolean soil = top.is(BlockTags.DIRT) || top.is(BlockTags.GRASS_BLOCKS);
				if (!top.isCollisionShapeFullBlock(b.level(), new BlockPos(x, g, z))) {
					top = Blocks.GRASS_BLOCK.defaultBlockState();
					soil = true;
				}
				if (u == 0 && w == 0) {
					centreTop = top;
				}
				BlockState filler = sand ? Blocks.SANDSTONE.defaultBlockState() : soil ? Blocks.DIRT.defaultBlockState() : top;
				for (int y = g + 1; y < t; y++) {
					b.put(x, y, z, filler);
				}
				for (int y = t + 1; y <= g; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				BlockState surface = top;
				if (!soil && !sand && d == 0) {
					surface = Blocks.COARSE_DIRT.defaultBlockState();
				} else if (soil && d == 0) {
					surface = trodden(k, u, w, top);
				} else if (soil && path(u, w)) {
					surface = Blocks.DIRT_PATH.defaultBlockState();
				}
				if (t != g || surface != top) {
					b.put(x, t, z, surface);
				}
				for (int y = Math.max(t, g) + 1; y <= t + (d == 0 ? 14 : 8); y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState s = b.level().getBlockState(pos);
					if (!s.isAir() && b.inside(x, y, z) && (ClippedBuild.cover(s) || d == 0 && y <= t + 6)) {
						b.put(x, y, z, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		k.sandy = centreTop != null && centreTop.is(BlockTags.SAND);
	}

	/** Path cells: out from the fire to the front edge and on past it. */
	private static boolean path(int u, int w) {
		return w >= 4 && w <= FRONT_W + SKIRT && Math.abs(u) <= 1 || w >= -2 && w <= 0 && Math.abs(u) <= 2;
	}

	private BlockState trodden(Build k, int u, int w, BlockState top) {
		float r = k.roll(u, w, 0x7EADL);
		if (path(u, w) && (u == 0 || r < 0.7F)) {
			return Blocks.DIRT_PATH.defaultBlockState();
		}
		boolean busy = Math.abs(u) <= 4 && w >= -3 && w <= 5;
		if (r < (busy ? 0.3F : 0.12F)) {
			return Blocks.COARSE_DIRT.defaultBlockState();
		}
		return top.is(Blocks.DIRT) || top.is(Blocks.COARSE_DIRT) ? Blocks.GRASS_BLOCK.defaultBlockState() : top;
	}

	// --- pieces ----------------------------------------------------------------------------------

	private BlockState wool(DyeColor c) {
		return Blocks.WOOL.pick(c).defaultBlockState();
	}

	private BlockState stairs(DyeColor c, Direction facing) {
		return Blocks.WOOL_STAIRS.pick(c).defaultBlockState().setValue(StairBlock.FACING, facing);
	}

	private static BlockState fence() {
		return Blocks.SPRUCE_FENCE.defaultBlockState();
	}

	private static BlockState lantern(boolean hanging) {
		return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging);
	}

	private static BlockState barrel(Direction facing) {
		return Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing);
	}

	private BlockState log(Direction.Axis axis) {
		return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private static BlockState hay(Direction.Axis axis) {
		return Blocks.HAY_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	/**
	 * The stall: four fence posts under a gabled roof of striped wool (stairs at the eaves, an overhang over the
	 * counter), a striped back wall over barrels and hay, a log-and-barrel counter dressed with a flower pot, cloth,
	 * candles and a lantern, a rug, a lantern under the ridge, and banners on the front posts and back wall.
	 */
	private void stall(Build k) {
		for (int u : new int[] { -3, 3 }) {
			for (int w : new int[] { -7, -4 }) {
				for (int dy = 1; dy <= 3; dy++) {
					k.put(u, dy, w, fence());
				}
			}
		}
		for (int u = -3; u <= 3; u++) {
			DyeColor c = plan.stripe(u);
			k.put(u, 4, -7, stairs(c, front()));
			k.put(u, 5, -6, wool(c));
			k.put(u, 5, -5, wool(c));
			k.put(u, 4, -4, stairs(c, back()));
			k.put(u, 3, -3, stairs(c, back()));
			if (Math.abs(u) == 3) {
				k.put(u, 4, -6, wool(c));
				k.put(u, 4, -5, wool(c));
			}
		}
		for (int u = -2; u <= 2; u++) {
			k.put(u, 2, -7, wool(plan.stripe(u)));
			k.put(u, 3, -7, wool(plan.stripe(u)));
		}
		// back row
		k.put(-2, 1, -7, barrel(front()));
		k.put(-1, 1, -7, barrel(front()));
		if (k.inside(-1, 1, -7)) {
			RandomizableContainer.setBlockEntityLootTable(k.b.level(), RandomSource.create(seed ^ 0xBA22E1L), at(-1, 1, -7), TraderPosts.LOOT);
		}
		k.put(0, 1, -7, hay(side(1).getAxis()));
		k.put(1, 1, -7, barrel(Direction.UP));
		k.put(2, 1, -7, barrel(front()));
		// counter
		k.put(-2, 1, -4, barrel(Direction.UP));
		for (int u = -1; u <= 1; u++) {
			k.put(u, 1, -4, log(side(1).getAxis()));
		}
		k.put(2, 1, -4, barrel(Direction.UP));
		k.put(-2, 2, -4, Blocks.POTTED_BLUE_ORCHID.defaultBlockState());
		k.put(-1, 2, -4, Blocks.CARPET.pick(plan.secondary).defaultBlockState());
		k.put(0, 2, -4, Blocks.CARPET.pick(plan.primary).defaultBlockState());
		k.put(1, 2, -4, Blocks.DYED_CANDLE.pick(plan.primary).defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		k.put(2, 2, -4, lantern(false));
		// rug and lights
		for (int u = -2; u <= 2; u++) {
			k.put(u, 1, -6, Blocks.CARPET.pick(Math.floorMod(u, 2) == 0 ? plan.primary : plan.secondary).defaultBlockState());
		}
		k.put(0, 4, -5, lantern(true));
		// banners
		wallBanner(k, -3, 2, -3, front(), plan.primary);
		wallBanner(k, 3, 2, -3, front(), plan.secondary);
		wallBanner(k, 0, 3, -6, front(), plan.primary);
	}

	/** A lit campfire with two log benches. */
	private void fire(Build k) {
		k.put(0, 1, 2, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.FACING, back()));
		k.put(-2, 1, 2, log(front().getAxis()));
		k.put(2, 1, 2, log(front().getAxis()));
	}

	/**
	 * A llama pen on side {@code s}: a ring of spruce fence 5 x 7 with a gate toward the plaza and lanterns either side
	 * of it, hay bales in one back corner and a water trough of two cauldrons in the other.
	 */
	private void pen(Build k, int s) {
		for (int a = 5; a <= 9; a++) {
			for (int w = -3; w <= 3; w++) {
				int u = s * a;
				boolean ring = a == 5 || a == 9 || Math.abs(w) == 3;
				if (ring) {
					if (a == 5 && w == 0) {
						k.put(u, 1, w, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, side(s)));
					} else {
						k.put(u, 1, w, fence());
					}
				} else if (k.inside(u, 0, w) && !k.sandy && (k.get(u, 0, w).is(BlockTags.DIRT) || k.get(u, 0, w).is(BlockTags.GRASS_BLOCKS))) {
					float r = k.roll(u, w, 0x9E4L);
					k.put(u, 0, w, r < 0.45F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
				}
			}
		}
		k.put(s * 5, 2, -1, lantern(false));
		k.put(s * 5, 2, 1, lantern(false));
		k.put(s * 8, 1, -2, hay(Direction.Axis.Y));
		k.put(s * 8, 2, -2, hay(Direction.Axis.Y));
		k.put(s * 7, 1, -2, hay(side(1).getAxis()));
		BlockState water = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
		k.put(s * 8, 1, 2, water);
		k.put(s * 7, 1, 2, water);
	}

	/** Where a second pen would be: a pile of hay and barrels. */
	private void cargo(Build k) {
		k.put(5, 1, -2, hay(front().getAxis()));
		k.put(6, 1, -2, hay(Direction.Axis.Y));
		k.put(6, 2, -2, hay(Direction.Axis.Y));
		k.put(7, 1, -1, barrel(Direction.UP));
		k.put(7, 1, 0, barrel(side(-1)));
		k.put(6, 1, 0, Blocks.POTTED_RED_TULIP.defaultBlockState());
	}

	/** A hitching rail (two posts and a top bar) with a water trough and a hay bale in front. */
	private void rail(Build k) {
		for (int u : new int[] { 4, 8 }) {
			k.put(u, 1, 6, fence());
			k.put(u, 2, 6, fence());
		}
		for (int u = 5; u <= 7; u++) {
			k.put(u, 2, 6, fence());
		}
		BlockState water = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
		k.put(5, 1, 7, water);
		k.put(6, 1, 7, water);
		k.put(7, 1, 7, hay(side(1).getAxis()));
	}

	/** A log post with a lantern on a fence on top and a sign either side. */
	private void signpost(Build k) {
		k.put(2, 1, 7, log(Direction.Axis.Y));
		k.put(2, 2, 7, log(Direction.Axis.Y));
		k.put(2, 3, 7, fence());
		k.put(2, 4, 7, lantern(false));
		sign(k, 2, 2, 8, front());
		sign(k, 2, 2, 6, back());
	}

	private void sign(Build k, int u, int dy, int w, Direction facing) {
		k.put(u, dy, w, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
		if (k.inside(u, dy, w) && k.b.level().getBlockEntity(at(u, dy, w)) instanceof SignBlockEntity sign) {
			List<Component> lines = List.of(Component.empty(), Component.translatableWithFallback(TraderPosts.SIGN_LINE_1, "Trader Post"),
					Component.translatableWithFallback(TraderPosts.SIGN_LINE_2, "All welcome"), Component.empty());
			CompoundTag tag = new CompoundTag();
			tag.put("front_text", SignText.CODEC.encodeStart(k.registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
					new SignText(lines, lines, DyeColor.BLUE, false)).getOrThrow());
			tag.putBoolean("is_waxed", true);
			sign.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, k.registries, tag));
			sign.setChanged();
		}
	}

	/** An open cart: a slab bed on dark wheels, cargo on board and a shaft of fences. */
	private void wagon(Build k) {
		for (int u = -8; u <= -5; u++) {
			for (int w = 6; w <= 7; w++) {
				k.put(u, 1, w, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
			}
		}
		for (int u : new int[] { -8, -5 }) {
			k.put(u, 1, 5, wheel(back()));
			k.put(u, 1, 8, wheel(front()));
		}
		k.put(-8, 2, 6, barrel(Direction.UP));
		if (k.inside(-8, 2, 6)) {
			RandomizableContainer.setBlockEntityLootTable(k.b.level(), RandomSource.create(seed ^ 0xCA27L), at(-8, 2, 6), TraderPosts.CART_LOOT);
		}
		k.put(-8, 2, 7, hay(side(1).getAxis()));
		k.put(-7, 2, 6, barrel(front()));
		k.put(-7, 2, 7, Blocks.CARPET.pick(plan.primary).defaultBlockState());
		k.put(-4, 1, 6, fence());
		k.put(-4, 1, 7, fence());
	}

	private static BlockState wheel(Direction facing) {
		return Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.OPEN, true)
				.setValue(TrapDoorBlock.HALF, Half.BOTTOM);
	}

	/** Two standing banners either side of the path at the front edge. */
	private void entranceBanners(Build k) {
		int rotation = RotationSegment.convertToSegment(front());
		for (int u : new int[] { -2, 2 }) {
			DyeColor c = u < 0 ? plan.primary : plan.secondary;
			k.put(u, 1, FRONT_W, Blocks.BANNER.pick(c).defaultBlockState().setValue(BannerBlock.ROTATION, rotation));
			paint(k, u, 1, FRONT_W, c);
		}
	}

	private void wallBanner(Build k, int u, int dy, int w, Direction facing, DyeColor c) {
		k.put(u, dy, w, Blocks.WALL_BANNER.pick(c).defaultBlockState().setValue(WallBannerBlock.FACING, facing));
		paint(k, u, dy, w, c);
	}

	private void paint(Build k, int u, int dy, int w, DyeColor base) {
		if (k.inside(u, dy, w) && k.b.level().getBlockEntity(at(u, dy, w)) instanceof BannerBlockEntity banner) {
			ItemStack stack = new ItemStack(Blocks.BANNER.pick(base).asItem());
			stack.set(DataComponents.BANNER_PATTERNS, traderPattern(base, k.registries));
			banner.applyComponentsFromItemStack(stack);
			banner.setChanged();
		}
	}

	/** The trader's banner: a gold diamond with a zigzag hem in the other colour (and a zigzag top on red). */
	public static BannerPatternLayers traderPattern(DyeColor base, HolderLookup.Provider registries) {
		var patterns = registries.lookupOrThrow(Registries.BANNER_PATTERN);
		DyeColor other = base == DyeColor.BLUE ? DyeColor.RED : DyeColor.BLUE;
		BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
		add(builder, patterns, BannerPatterns.RHOMBUS_MIDDLE, DyeColor.YELLOW);
		add(builder, patterns, BannerPatterns.TRIANGLES_BOTTOM, other);
		if (base != DyeColor.BLUE) {
			add(builder, patterns, BannerPatterns.TRIANGLES_TOP, other);
		}
		return builder.build();
	}

	private static void add(BannerPatternLayers.Builder builder, HolderLookup.RegistryLookup<BannerPattern> patterns, ResourceKey<BannerPattern> key,
			DyeColor color) {
		builder.add(patterns.getOrThrow(key), color);
	}
}

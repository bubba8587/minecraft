// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One monster camp, built in code (specs/monster-camps.md). Its layout is planned from its own seed before any block
 * is read (so every chunk it spans builds its part of the same camp); each column then follows the ground there.
 *
 * <p>A round clearing of trodden ground (coarse dirt and gravel; sand, snow or mud by kind) with weeds at its edge; a
 * cold (unlit) soul campfire in the middle ringed by log seats, a chest of camp loot beside it; scrappy lookout towers (log posts,
 * a patchy plank platform with fence railing, a ladder, a banner on a corner post); standing banners about the camp;
 * bone-and-skull rock outcrops and skulls on pikes at the edge; in medium and large camps a lean-to over bedrolls.
 */
public class CampPiece extends StructurePiece {
	private final long seed;
	private final BlockPos centre;
	private final CampKind kind;
	private final CampTuning.Size size;
	private final Plan plan;

	public CampPiece(long seed, BlockPos centre, CampKind kind, CampTuning.Size size) {
		super(Camps.CAMP_PIECE, 0, box(centre, size));
		this.seed = seed;
		this.centre = centre;
		this.kind = kind;
		this.size = size;
		this.plan = new Plan(seed, centre, size);
	}

	public CampPiece(CompoundTag tag) {
		super(Camps.CAMP_PIECE, tag);
		this.seed = tag.getLongOr("Seed", 0L);
		this.centre = BlockPos.of(tag.getLongOr("Centre", 0L));
		this.kind = CampKind.values()[Math.floorMod(tag.getIntOr("Kind", 0), CampKind.values().length)];
		this.size = CampTuning.Size.values()[Math.floorMod(tag.getIntOr("Size", 0), CampTuning.Size.values().length)];
		this.plan = new Plan(seed, centre, size);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Seed", seed);
		tag.putLong("Centre", centre.asLong());
		tag.putInt("Kind", kind.ordinal());
		tag.putInt("Size", size.ordinal());
	}

	static BoundingBox box(BlockPos centre, CampTuning.Size size) {
		// clearing plus edge outcrops (up to 3 past the radius), or the stair tunnel
		int r = Math.max(size.radius + 5, size.maxRoomHalf + 10);
		return new BoundingBox(centre.getX() - r, centre.getY() - 6, centre.getZ() - r, centre.getX() + r, centre.getY() + 22, centre.getZ() + r);
	}

	public BlockPos centre() {
		return centre;
	}

	public CampKind kind() {
		return kind;
	}

	public CampTuning.Size size() {
		return size;
	}

	/** Archer spots: the middle of each tower's platform, one above the planks. */
	public List<BlockPos> towerSpots() {
		List<BlockPos> spots = new ArrayList<>();
		for (Tower tower : plan.towers) {
			spots.add(new BlockPos(tower.x, centre.getY() + tower.height + 1, tower.z));
		}
		if (plan.winding != null) {
			spots.add(windingTop());
		}
		return spots;
	}

	/** Ground spots round the fire (columns; the height comes from the world when the roster appears). */
	public List<BlockPos> groundSpots() {
		List<BlockPos> spots = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			double a = plan.spawnAngle + i * Math.PI * 2.0 / 7.0;
			double r = 3.0 + (i % 3);
			spots.add(new BlockPos(centre.getX() + (int) Math.round(Math.cos(a) * r), 0, centre.getZ() + (int) Math.round(Math.sin(a) * r)));
		}
		return spots;
	}

	// --- the plan: everything decided from the seed, before the world is read ----------------------

	private record Tower(int x, int z, int height, Direction ladder, boolean roof, int bannerCorner, Direction ramp, Direction bridge) {
		boolean opens(Direction d) {
			return d == ramp || d == bridge;
		}
	}

	private record Bridge(Tower from, Direction dir, int length) {
	}

	private record Skull(int x, int z, Direction facing, boolean buried, long seed) {
	}

	private record Ribs(int x, int z, Direction along, int length, long seed) {
		Footprint footprint() {
			Direction across = along.getClockWise();
			int ax = x - along.getStepX() - across.getStepX() * 3;
			int az = z - along.getStepZ() - across.getStepZ() * 3;
			int bx = x + along.getStepX() * length + across.getStepX() * 3;
			int bz = z + along.getStepZ() * length + across.getStepZ() * 3;
			return new Footprint("ribcage", Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz));
		}
	}

	private record Winding(int x, int z, int risers, int start, long seed) {
	}

	/** A cell out from a tower's platform edge: k steps out on side d (0 is the edge itself), row w (0, 1 the middle rows; -1, 2 beside them). */
	private static int[] towerCell(int tx, int tz, Direction d, int k, int w) {
		int x0 = tx - 1;
		int z0 = tz - 1;
		if (d.getAxis() == Direction.Axis.X) {
			return new int[] { d == Direction.EAST ? x0 + 3 + k : x0 - k, z0 + 1 + w };
		}
		return new int[] { x0 + 1 + w, d == Direction.SOUTH ? z0 + 3 + k : z0 - k };
	}

	private static Footprint towerStrip(String what, int tx, int tz, Direction d, int k0, int k1, int w0, int w1) {
		int[] a = towerCell(tx, tz, d, k0, w0);
		int[] b = towerCell(tx, tz, d, k1, w1);
		return new Footprint(what, Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[0], b[0]), Math.max(a[1], b[1]));
	}

	private record Spot(int x, int z, int size, long seed) {
	}

	/** One piece's ground cells (inclusive, columns), for the footprint plan and tests. */
	public record Footprint(String what, int minX, int minZ, int maxX, int maxZ) {
		public boolean clearOf(Footprint other, int margin) {
			return minX > other.maxX + margin || other.minX > maxX + margin || minZ > other.maxZ + margin || other.minZ > maxZ + margin;
		}

		static Footprint around(String what, int x, int z, int reach) {
			return new Footprint(what, x - reach, z - reach, x + reach, z + reach);
		}
	}

	/** Every planned piece's footprint: the fire, the stair tunnel, towers, lean-tos, outcrops, pikes and banners. */
	public List<Footprint> footprints() {
		return List.copyOf(plan.footprints);
	}

	private static final class Plan {
		static final int MARGIN = 1;
		static final int TRIES = 16;
		final List<Footprint> footprints = new ArrayList<>();
		final List<Tower> towers = new ArrayList<>();
		final List<Spot> banners = new ArrayList<>();
		final List<Spot> outcrops = new ArrayList<>();
		final List<Spot> pikes = new ArrayList<>();
		final List<Spot> leanTos = new ArrayList<>();
		final List<Bridge> bridges = new ArrayList<>();
		Skull skull;
		Ribs ribs;
		Winding winding;
		final Direction stairs;
		final boolean stairsClockwise;
		final Direction chestSide;
		final double spawnAngle;
		final int bannerRotation;
		final int roomHalfX;
		final int roomHalfZ;
		final int roomDx;
		final int roomDz;

		Plan(long seed, BlockPos centre, CampTuning.Size size) {
			RandomSource random = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed));
			int r = size.radius;
			int cx = centre.getX();
			int cz = centre.getZ();
			// the room and its stairs first: size, shape, offset, stair side
			stairs = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			int max = size.maxRoomHalf;
			int reach = size == CampTuning.Size.LARGE ? 3 : 2;
			boolean alongX = stairs.getAxis() == Direction.Axis.X;
			int hx = 1 + random.nextInt(alongX ? reach : max);
			int hz = 1 + random.nextInt(alongX ? max : reach);
			int dx = random.nextInt(3) - 1;
			int dz = random.nextInt(3) - 1;
			if (alongX && dx * stairs.getStepX() > 0) {
				dx = 0;
			}
			if (!alongX && dz * stairs.getStepZ() > 0) {
				dz = 0;
			}
			roomHalfX = hx;
			roomHalfZ = hz;
			roomDx = dx;
			roomDz = dz;
			stairsClockwise = random.nextBoolean();
			chestSide = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			spawnAngle = random.nextDouble() * Math.PI * 2.0;
			bannerRotation = random.nextInt(16);
			// reserved first: the fire with its seats and chest, and the stair tunnel where it opens to the surface, with its mouth and walls
			footprints.add(Footprint.around("fire", cx, cz, 2));
			Direction side = stairsClockwise ? stairs.getClockWise() : stairs.getCounterClockWise();
			int h = alongX ? hx : hz;
			int fx = cx + dx;
			int fz = cz + dz;
			int ax = fx + stairs.getStepX() * (h + 4) - side.getStepX();
			int az = fz + stairs.getStepZ() * (h + 4) - side.getStepZ();
			int bx = fx + stairs.getStepX() * (h + 10) + side.getStepX() * 2;
			int bz = fz + stairs.getStepZ() * (h + 10) + side.getStepZ() * 2;
			footprints.add(new Footprint("stairs", Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)));
			// then each piece on free cells only, its spot re-rolled until one fits, or dropped
			// rare centrepieces of large camps: a skull rock at the edge, a winding tower
			if (size == CampTuning.Size.LARGE && random.nextInt(3) == 0) {
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int x = cx + (int) Math.round(Math.cos(a) * 10);
					int z = cz + (int) Math.round(Math.sin(a) * 10);
					if (claim(Footprint.around("skull", x, z, 6))) {
						skull = new Skull(x, z, Direction.getApproximateNearest(cx - x, 0.0, cz - z), (pieceSeed & 1L) == 0L, pieceSeed);
						break;
					}
				}
			}
			if (size == CampTuning.Size.LARGE && random.nextInt(4) == 0) {
				int risers = 12 + random.nextInt(3);
				int start = random.nextInt(4);
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int x = cx + (int) Math.round(Math.cos(a) * (r - 3));
					int z = cz + (int) Math.round(Math.sin(a) * (r - 3));
					if (claim(Footprint.around("winding tower", x, z, 3))) {
						winding = new Winding(x, z, risers, start, pieceSeed);
						break;
					}
				}
			}
			if (size == CampTuning.Size.LARGE && random.nextInt(3) == 0) {
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int x = cx + (int) Math.round(Math.cos(a) * (r - 4));
					int z = cz + (int) Math.round(Math.sin(a) * (r - 4));
					Direction along = Direction.getApproximateNearest(-Math.sin(a), 0.0, Math.cos(a));
					Ribs candidate = new Ribs(x - along.getStepX() * 3, z - along.getStepZ() * 3, along, 7, pieceSeed);
					if (claim(candidate.footprint())) {
						ribs = candidate;
						break;
					}
				}
			}
			int leanToCount = 1 + random.nextInt(size == CampTuning.Size.SMALL ? 1 : 2);
			double la = random.nextDouble() * Math.PI * 2.0;
			for (int i = 0; i < leanToCount; i++) {
				double base = la + i * Math.PI * (0.7 + random.nextDouble() * 0.6);
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = t == 0 ? base : random.nextDouble() * Math.PI * 2.0;
					int d = r - 3 + (t & 1);
					int x = cx + (int) Math.round(Math.cos(a) * d);
					int z = cz + (int) Math.round(Math.sin(a) * d);
					Spot spot = new Spot(x, z, 0, pieceSeed);
					if (claim(leanToFootprint(spot, centre))) {
						leanTos.add(spot);
						break;
					}
				}
			}
			double towerAngle = random.nextDouble() * Math.PI * 2.0;
			int towerCount = random.nextInt(size.maxTowers + 1);
			for (int i = 0; i < towerCount; i++) {
				double base = towerAngle + i * (Math.PI * 2.0 / towerCount);
				int height = 5 + random.nextInt(3);
				Direction ladder = Direction.Plane.HORIZONTAL.getRandomDirection(random);
				int corner = random.nextInt(4);
				for (int t = 0; t < TRIES; t++) {
					double a = t == 0 ? base + (random.nextDouble() - 0.5) * 0.8 : random.nextDouble() * Math.PI * 2.0;
					int x = cx + (int) Math.round(Math.cos(a) * (r - 2));
					int z = cz + (int) Math.round(Math.sin(a) * (r - 2));
					Footprint own = new Footprint("tower", x - 2, z - 2, x + 3, z + 3);
					if (claim(own)) {
						Direction ramp = random.nextFloat() < RAMP_CHANCE ? ramp(random, x, z, height, cx, cz, r, own) : null;
						Tower tower = new Tower(x, z, height, ladder, true, corner, ramp, null);
						towers.add(tower);
						if (size != CampTuning.Size.SMALL && random.nextFloat() < BRIDGE_CHANCE) {
							twin(random, tower, own, cx, cz, r);
						}
						break;
					}
				}
			}
			int outcropCount = random.nextInt(size.outcrops + 2);
			for (int i = 0; i < outcropCount; i++) {
				int m = 1 + random.nextInt(2);
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int d = r - 2 + random.nextInt(2);
					int x = cx + (int) Math.round(Math.cos(a) * d);
					int z = cz + (int) Math.round(Math.sin(a) * d);
					if (claim(Footprint.around("outcrop", x, z, m + 1))) {
						outcrops.add(new Spot(x, z, m, pieceSeed));
						break;
					}
				}
			}
			int pikeCount = random.nextInt(size.outcrops + 3);
			for (int i = 0; i < pikeCount; i++) {
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int x = cx + (int) Math.round(Math.cos(a) * (r - 1));
					int z = cz + (int) Math.round(Math.sin(a) * (r - 1));
					if (claim(Footprint.around("pike", x, z, 0))) {
						pikes.add(new Spot(x, z, 0, pieceSeed));
						break;
					}
				}
			}
			// banners: the first flies from the first tower when there is one
			for (int i = towers.isEmpty() ? 0 : 1; i < size.banners; i++) {
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int d = 3 + random.nextInt(Math.max(1, r - 4));
					int x = cx + (int) Math.round(Math.cos(a) * d);
					int z = cz + (int) Math.round(Math.sin(a) * d);
					if (claim(Footprint.around("banner", x, z, 0))) {
						banners.add(new Spot(x, z, 0, pieceSeed));
						break;
					}
				}
			}
		}

		static final float RAMP_CHANCE = 0.35F;
		static final float BRIDGE_CHANCE = 0.4F;

		/** A ramp of stairs from the ground up to a tower's platform, on a side whose foot stays in the clearing; null when none fits. */
		private Direction ramp(RandomSource random, int x, int z, int height, int cx, int cz, int r, Footprint own) {
			List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
			net.minecraft.util.Util.shuffle(sides, random);
			for (Direction d : sides) {
				int[] foot = towerCell(x, z, d, height, 0);
				if (Math.hypot(foot[0] - cx, foot[1] - cz) <= r - 1 && claim(towerStrip("ramp", x, z, d, 2, height, 0, 1), own)) {
					return d;
				}
			}
			return null;
		}

		/** A second tower the same height a few blocks off one side of the first, a plank bridge between their platforms. */
		private void twin(RandomSource random, Tower a, Footprint aFootprint, int cx, int cz, int r) {
			List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
			net.minecraft.util.Util.shuffle(sides, random);
			int gap = 4 + random.nextInt(4);
			for (Direction d : sides) {
				if (d == a.ramp) {
					continue;
				}
				int bx = a.x + d.getStepX() * (4 + gap);
				int bz = a.z + d.getStepZ() * (4 + gap);
				if (Math.hypot(bx - cx, bz - cz) > r - 2) {
					continue;
				}
				Footprint bFootprint = new Footprint("tower", bx - 2, bz - 2, bx + 3, bz + 3);
				if (!claim(bFootprint)) {
					continue;
				}
				if (!claim(towerStrip("bridge", a.x, a.z, d, 2, gap - 1, -1, 2), aFootprint, bFootprint)) {
					footprints.remove(bFootprint);
					continue;
				}
				Tower withBridge = new Tower(a.x, a.z, a.height, a.ladder, a.roof, a.bannerCorner, a.ramp, d);
				towers.set(towers.indexOf(a), withBridge);
				towers.add(new Tower(bx, bz, a.height, Direction.Plane.HORIZONTAL.getRandomDirection(random), true, random.nextInt(4), null, d.getOpposite()));
				bridges.add(new Bridge(withBridge, d, gap));
				return;
			}
		}

		/**
		 * Adds the footprint when it keeps the margin from every planned one (a lean-to may open right onto the fire's
		 * seats; a ramp or bridge meets the towers it joins).
		 */
		private boolean claim(Footprint footprint, Footprint... joins) {
			for (Footprint other : footprints) {
				boolean joined = false;
				for (Footprint j : joins) {
					joined |= j == other;
				}
				int margin = joined || footprint.what().equals("lean-to") && other.what().equals("fire") ? 0 : MARGIN;
				if (!footprint.clearOf(other, margin)) {
					return false;
				}
			}
			footprints.add(footprint);
			return true;
		}
	}

	/** A lean-to's cells. */
	private static Footprint leanToFootprint(Spot spot, BlockPos centre) {
		Direction facing = leanToFacing(spot, centre);
		Direction along = facing.getClockWise();
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (int k = -1; k <= 1; k++) {
			for (int row = 0; row <= 2; row++) {
				int x = spot.x + along.getStepX() * k - facing.getStepX() * row;
				int z = spot.z + along.getStepZ() * k - facing.getStepZ() * row;
				minX = Math.min(minX, x);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxZ = Math.max(maxZ, z);
			}
		}
		return new Footprint("lean-to", minX, minZ, maxX, maxZ);
	}

	private static Direction leanToFacing(Spot spot, BlockPos centre) {
		return Direction.getApproximateNearest(centre.getX() - spot.x, 0.0, centre.getZ() - spot.z);
	}

	// --- building ---------------------------------------------------------------------------------

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource chunkRandom, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos) {
		ClippedBuild b = new ClippedBuild(level, chunkBB);
		int r = size.radius;
		int cx = centre.getX();
		int cz = centre.getZ();
		// Clearing: trees and plants out, trodden ground, weeds at the edge.
		for (int x = cx - r - 1; x <= cx + r + 1; x++) {
			for (int z = cz - r - 1; z <= cz + r + 1; z++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				if (d > r + 0.5) {
					continue;
				}
				RandomSource column = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ (x * 341873128712L) ^ (z * 132897987541L));
				float tread = column.nextFloat();
				float what = column.nextFloat();
				float weed = column.nextFloat();
				b.clearCover(x, z, 20);
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				BlockPos at = new BlockPos(x, g, z);
				if (tread < (d < r - 1.5 ? 0.6F : 0.25F) && natural(level.getBlockState(at))) {
					b.put(x, g, z, kind.trodden(what));
				} else if (d > r - 2.5 && weed < 0.15F && kind.weed(what).canSurvive(level, at.above())) {
					b.putIfAir(x, g + 1, z, kind.weed(what)); // only where it can grow (grass on gravel would pop)
				}
			}
		}
		fire(b, cx, cz);
		for (Tower tower : plan.towers) {
			tower(b, tower);
			if (tower.ramp != null) {
				ramp(b, tower);
			}
		}
		for (Bridge bridge : plan.bridges) {
			bridge(b, bridge);
		}
		if (plan.skull != null) {
			skull(b, plan.skull);
		}
		if (plan.winding != null) {
			winding(b, plan.winding);
		}
		if (plan.ribs != null) {
			ribcage(b, plan.ribs);
		}
		for (Spot banner : plan.banners) {
			standingBanner(b, banner.x, banner.z, plan.bannerRotation);
		}
		for (Spot outcrop : plan.outcrops) {
			outcrop(b, outcrop);
		}
		for (Spot pike : plan.pikes) {
			pike(b, pike);
		}
		for (Spot leanTo : plan.leanTos) {
			leanTo(b, leanTo);
		}
		if (roomFloor() > level.getMinY()) {
			room(b); // (a superflat world is too shallow for it)
		}
		b.tidyFences(getBoundingBox());
	}

	/** A floor block mobs can't sink into: mud becomes packed mud. */
	private static BlockState fullBlock(BlockState state) {
		return state.is(Blocks.MUD) ? Blocks.PACKED_MUD.defaultBlockState() : state;
	}

	private static boolean natural(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MUD)
				|| state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM);
	}

	/** The unlit soul campfire, a stone ring round it, log seats on three sides and the loot chest. */
	private void fire(ClippedBuild b, int cx, int cz) {
		int g = b.ground(cx, cz);
		if (g != Integer.MIN_VALUE) {
			b.put(cx, g, cz, Blocks.COBBLESTONE.defaultBlockState());
			b.put(cx, g + 1, cz, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
			b.clearAbove(cx, g + 1, cz, 4);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int x = cx + side.getStepX();
			int z = cz + side.getStepZ();
			int gs = b.ground(x, z);
			if (gs != Integer.MIN_VALUE) {
				b.put(x, gs, z, Blocks.COBBLESTONE.defaultBlockState());
			}
			if (side == plan.chestSide.getOpposite()) {
				continue; // the open side
			}
			// A seat: a log lying across, two out.
			int sx = cx + side.getStepX() * 2;
			int sz = cz + side.getStepZ() * 2;
			Direction.Axis along = side.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
			for (int k = -1; k <= 0; k++) {
				int x2 = sx + (along == Direction.Axis.X ? k : 0);
				int z2 = sz + (along == Direction.Axis.Z ? k : 0);
				int g2 = b.ground(x2, z2);
				if (g2 != Integer.MIN_VALUE) {
					b.put(x2, g2 + 1, z2, kind.log().trySetValue(RotatedPillarBlock.AXIS, along));
				}
			}
		}
		// The chest, at the seat's end on the chest side.
		int x = cx + plan.chestSide.getStepX() * 2 + plan.chestSide.getClockWise().getStepX() * 2;
		int z = cz + plan.chestSide.getStepZ() * 2 + plan.chestSide.getClockWise().getStepZ() * 2;
		int gc = b.ground(x, z);
		if (gc != Integer.MIN_VALUE && b.inside(x, gc + 1, z)) {
			b.put(x, gc + 1, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, plan.chestSide.getOpposite()));
			RandomizableContainer.setBlockEntityLootTable(b.level(), RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0xC4E57L), new BlockPos(x, gc + 1, z), Camps.CAMP_LOOT);
		}
	}

	/**
	 * A scrappy lookout tower on stilts: four thin log posts at the corners of a 4x4, a fence brace part way up, a plank
	 * platform with a few planks sagging to slabs, connected fence railing with gaps, a ladder up the outside of one
	 * post, sometimes a slab roof on fence posts, and the camp's banner flying from another post.
	 */
	private void tower(ClippedBuild b, Tower t) {
		RandomSource random = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ (t.x * 31L + t.z) ^ 0x70E7L);
		int top = centre.getY() + t.height;
		int x0 = t.x - 1;
		int z0 = t.z - 1;
		boolean[] sag = new boolean[16];
		boolean[] railGone = new boolean[16];
		for (int i = 0; i < 16; i++) {
			sag[i] = random.nextFloat() < 0.15F;
			railGone[i] = random.nextFloat() < 0.25F;
		}
		int braceSide = random.nextInt(4);
		int braceY = centre.getY() + 2 + random.nextInt(2);
		int ladderCorner = (t.bannerCorner + 1 + random.nextInt(3)) % 4;
		boolean ladderAlongX = random.nextBoolean();
		int[][] corners = { { 0, 0 }, { 3, 0 }, { 3, 3 }, { 0, 3 } };
		// Posts, each down to its own column's ground.
		for (int[] c : corners) {
			int x = x0 + c[0];
			int z = z0 + c[1];
			int g = b.ground(x, z);
			if (g != Integer.MIN_VALUE) {
				for (int y = g + 1; y <= top; y++) {
					b.put(x, y, z, kind.log());
				}
			}
		}
		// A brace between two posts on one side.
		int[] from = corners[braceSide];
		int[] to = corners[(braceSide + 1) % 4];
		for (int k = 1; k <= 2; k++) {
			int x = x0 + from[0] + (to[0] - from[0]) / 3 * k;
			int z = z0 + from[1] + (to[1] - from[1]) / 3 * k;
			boolean alongX = from[1] == to[1];
			b.put(x, braceY, z, fence(alongX, !alongX, alongX, !alongX));
		}
		// Platform and railing.
		for (int dx = 0; dx <= 3; dx++) {
			for (int dz = 0; dz <= 3; dz++) {
				int i = dx * 4 + dz;
				boolean corner = (dx == 0 || dx == 3) && (dz == 0 || dz == 3);
				int x = x0 + dx;
				int z = z0 + dz;
				if (!corner) {
					b.put(x, top, z, sag[i] && dx > 0 && dx < 3 && dz > 0 && dz < 3 ? kind.slab().setValue(SlabBlock.TYPE, SlabType.TOP) : kind.planks());
				}
				boolean edge = dx == 0 || dx == 3 || dz == 0 || dz == 3;
				if (!edge) {
					continue;
				}
				int ci = corner ? cornerIndex(dx, dz) : -1;
				if (ci == t.bannerCorner) {
					b.put(x, top + 1, z, kind.fence());
					b.put(x, top + 2, z, CampBanners.standing(kind).defaultBlockState().setValue(BannerBlock.ROTATION, plan.bannerRotation));
					if (b.inside(x, top + 2, z)) {
						CampBanners.paint(b.level().getBlockEntity(new BlockPos(x, top + 2, z)), kind, b.level().registryAccess());
					}
					continue;
				}
				Direction edgeSide = dx == 3 ? Direction.EAST : dx == 0 ? Direction.WEST : dz == 0 ? Direction.NORTH : Direction.SOUTH;
				if (ci == ladderCorner || (!corner && (railGone[i] || t.opens(edgeSide)))) {
					continue; // where the ladder, a ramp or a bridge comes up, and gaps
				}
				// Connected railing along the edge.
				boolean alongX = dz == 0 || dz == 3;
				boolean alongZ = dx == 0 || dx == 3;
				b.put(x, top + 1, z, fence(alongX && dx < 3, alongZ && dz < 3, alongX && dx > 0, alongZ && dz > 0));
				if (t.roof && corner) {
					b.put(x, top + 2, z, kind.fence());
					b.put(x, top + 3, z, kind.fence()); // roof posts tall enough to clear a banner
				}
			}
		}
		if (t.roof) {
			for (int dx = 0; dx <= 3; dx++) {
				for (int dz = 0; dz <= 3; dz++) {
					b.put(x0 + dx, top + 4, z0 + dz, kind.slab()); // whole: the lookout's guaranteed shade
				}
			}
		}
		// The ladder: up the outer face of a corner post to its top.
		int[] lc = corners[ladderCorner];
		Direction out = ladderAlongX ? (lc[0] == 0 ? Direction.WEST : Direction.EAST) : (lc[1] == 0 ? Direction.NORTH : Direction.SOUTH);
		if (out == t.bridge) {
			out = !ladderAlongX ? (lc[0] == 0 ? Direction.WEST : Direction.EAST) : (lc[1] == 0 ? Direction.NORTH : Direction.SOUTH);
		}
		int lx = x0 + lc[0] + out.getStepX();
		int lz = z0 + lc[1] + out.getStepZ();
		int gl = b.ground(lx, lz);
		if (gl != Integer.MIN_VALUE) {
			// Up to the post's top, never past it (a rung with no post behind it would pop off); you step from the
			// top rung onto the open post top. Below the post's foot the ground in its column backs the ladder.
			for (int y = gl + 1; y <= top; y++) {
				b.put(lx, y, lz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, out));
			}
		}
	}

	/** A ramp of stairs from the ground up to a tower's platform, 2 wide, on log stilts. */
	private void ramp(ClippedBuild b, Tower t) {
		int top = centre.getY() + t.height;
		Direction d = t.ramp;
		for (int w = 0; w <= 1; w++) {
			for (int k = 1; k <= t.height; k++) {
				int[] c = towerCell(t.x, t.z, d, k, w);
				int y = top - k + 1;
				int g = b.ground(c[0], c[1]);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				if (k % 2 == 1) {
					for (int py = g + 1; py < y; py++) {
						b.put(c[0], py, c[1], kind.log());
					}
				}
				b.put(c[0], y, c[1], kind.woodStairs().setValue(StairBlock.FACING, d.getOpposite()));
				b.clearAbove(c[0], y, c[1], 3);
			}
		}
	}

	/** A sagging plank bridge between two towers' platforms, chain rope railing strung between log posts on side beams. */
	private void bridge(ClippedBuild b, Bridge bridge) {
		Tower t = bridge.from;
		int top = centre.getY() + t.height;
		Direction d = bridge.dir;
		int n = bridge.length;
		for (int k = 1; k <= n; k++) {
			boolean post = k == 1 || k == n || k % 4 == 1 && n - k >= 3;
			boolean sag = k > 1 + n / 4 && k < n - n / 4;
			for (int w = 0; w <= 1; w++) {
				int[] c = towerCell(t.x, t.z, d, k, w);
				b.put(c[0], top, c[1], sag ? kind.slab() : kind.planks());
				b.clearAbove(c[0], top, c[1], 2);
			}
			// side beams under the railing, posts on them, chain rope between posts
			for (int w : new int[] { -1, 2 }) {
				int[] c = towerCell(t.x, t.z, d, k, w);
				b.put(c[0], top, c[1], kind.log().trySetValue(RotatedPillarBlock.AXIS, d.getAxis()));
				b.put(c[0], top + 1, c[1], post ? kind.log() : Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis()));
			}
		}
	}

	/** Skull rock front, top row first (skull-local dy 12 down to 0, u -6 to 6): # bone, E eye socket, N nose, M mouth. */
	private static final String[] SKULL_FACE = {
			"....#####....",
			"..#########..",
			".###########.",
			"#############",
			"##EEE###EEE##",
			"##EEE###EEE##",
			"##EEE###EEE##",
			".#####N#####.",
			"..###NNN###..",
			"...#######...",
			"...#MMMMM#...",
			"...#MMMMM#...",
			"...#######...", };
	/** How deep a half-buried skull sits: its eye sockets just off the ground, a step up into one. */
	static final int SKULL_BURIED_SINK = 4;

	private static char skullFace(int u, int dy) {
		if (u < -6 || u > 6 || dy < 0 || dy > 12) {
			return '.';
		}
		return SKULL_FACE[12 - dy].charAt(u + 6);
	}

	/** The skull's solid envelope (skull-local u across, w toward the fire, dy up from the skull's base), openings filled. */
	private static boolean skullShape(int u, int w, int dy) {
		if (skullFace(u, dy) == '.') {
			return false;
		}
		int au = Math.abs(u);
		int front = dy == 9 && au >= 1 && au <= 5 ? 5 : au == 6 || dy == 12 ? 3 : 4; // brow ridge out over the eyes
		if (w > front) {
			return false;
		}
		return w >= 0 || sq(u / 6.6) + sq(w / 6.2) + sq((dy - 6.5) / 7.0) <= 1.0;
	}

	/** The skull's hollow: shape cells with shape all round them. */
	private static boolean skullHollow(int u, int w, int dy) {
		if (dy < 1) {
			return false;
		}
		for (int a = -1; a <= 1; a++) {
			for (int c = -1; c <= 1; c++) {
				for (int e = -1; e <= 1; e++) {
					if (!skullShape(u + a, w + c, dy + e)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Skull cell: 0 outside, 1 bone shell, 2 air (hollow or an opening). */
	private static int skullCell(int u, int w, int dy, boolean buried) {
		if (!skullShape(u, w, dy)) {
			return 0;
		}
		char c = skullFace(u, dy);
		boolean door = c == 'E' && buried && u > 0;
		if ((c == 'E' || c == 'N') && !door && w >= 1) {
			return 2;
		}
		if (c == 'M' && w >= -1 || door && w >= 1 || skullHollow(u, w, dy)) {
			return 2;
		}
		return 1;
	}

	/** Skull shell material: quartz pillar brow, bone block mostly, some calcite and quartz. */
	private static BlockState skullBone(int u, int w, int dy, float roll, Direction.Axis across) {
		if (dy == 9 && w == 5) {
			return Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, across);
		}
		if (roll < 0.7F) {
			return Blocks.BONE_BLOCK.defaultBlockState();
		}
		if (roll < 0.85F) {
			return Blocks.CALCITE.defaultBlockState();
		}
		return roll < 0.95F ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.QUARTZ_BLOCK.defaultBlockState();
	}

	private static double sq(double v) {
		return v * v;
	}

	/**
	 * A big skull rock facing the fire: a round bone cranium over a narrower jaw, a brow over two big hollow eye
	 * sockets, a nose hole, the mouth a doorway into the hollow. Half-buried, only the cranium shows and an eye is the way in.
	 */
	private void skull(ClippedBuild b, Skull s) {
		Direction f = s.facing;
		Direction across = f.getClockWise();
		int y0 = centre.getY();
		int sink = s.buried ? SKULL_BURIED_SINK : 0;
		for (int u = -6; u <= 6; u++) {
			for (int w = -6; w <= 5; w++) {
				boolean any = false;
				for (int dy = sink; dy <= 12 && !any; dy++) {
					any = skullShape(u, w, dy);
				}
				if (!any) {
					continue;
				}
				int x = s.x + across.getStepX() * u + f.getStepX() * w;
				int z = s.z + across.getStepZ() * u + f.getStepZ() * w;
				b.clearCover(x, z, 20);
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				RandomSource cell = RandomSource.create(s.seed ^ (x * 73428767L) ^ (z * 912931L));
				for (int y = g + 1; y < y0; y++) {
					b.put(x, y, z, kind.rock(cell.nextFloat()));
				}
				for (int y = y0 + 1; y <= g; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				boolean floorInside = skullCell(u, w, sink + 1, s.buried) == 2;
				b.put(x, y0, z, floorInside ? fullBlock(kind.trodden(cell.nextFloat())) : skullShape(u, w, sink) ? Blocks.BONE_BLOCK.defaultBlockState()
						: kind.rock(cell.nextFloat()));
				for (int dy = sink + 1; dy <= 12; dy++) {
					int y = y0 + dy - sink;
					int what = skullCell(u, w, dy, s.buried);
					float roll = cell.nextFloat();
					if (what == 0) {
						continue;
					}
					b.put(x, y, z, what == 2 ? Blocks.AIR.defaultBlockState() : what == 3 ? Blocks.COAL_BLOCK.defaultBlockState()
							: roll < 0.9F ? Blocks.CALCITE.defaultBlockState() : Blocks.BONE_BLOCK.defaultBlockState());
				}
			}
		}
		if (s.buried) {
			for (int u = 2; u <= 4; u++) {
				b.put(s.x + across.getStepX() * u + f.getStepX() * 5, y0 + 1, s.z + across.getStepZ() * u + f.getStepZ() * 5, Blocks.BONE_BLOCK.defaultBlockState());
			}
		}
		// a skull on the floor inside
		RandomSource random = RandomSource.create(s.seed);
		int x = s.x - f.getStepX() * 2;
		int z = s.z - f.getStepZ() * 2;
		b.put(x, y0 + 1, z, kind.skull().setValue(SkullBlock.ROTATION, random.nextInt(16)));
	}

	/** Ribcage arch: one rib's cells across the path (|u|, dy above the ground), from the spine down to the ground. */
	private static final int[][] RIB = { { 1, 5 }, { 2, 5 }, { 3, 4 }, { 3, 3 }, { 3, 2 }, { 2, 1 } };

	/** A giant's ribcage lying over a path into the camp: a bone spine five up, ribs arching down either side every other block. */
	private void ribcage(ClippedBuild b, Ribs ribs) {
		Direction d = ribs.along;
		Direction across = d.getClockWise();
		int y0 = centre.getY();
		for (int k = -1; k <= ribs.length; k++) {
			for (int u = -3; u <= 3; u++) {
				int x = ribs.x + d.getStepX() * k + across.getStepX() * u;
				int z = ribs.z + d.getStepZ() * k + across.getStepZ() * u;
				b.clearCover(x, z, 12);
				for (int dy = 1; dy <= 6; dy++) {
					b.put(x, y0 + dy, z, Blocks.AIR.defaultBlockState());
				}
				int g = b.ground(x, z);
				if (g != Integer.MIN_VALUE) {
					for (int y = g + 1; y <= y0; y++) {
						b.put(x, y, z, kind.rock(0.5F));
					}
				}
			}
			int sx = ribs.x + d.getStepX() * k;
			int sz = ribs.z + d.getStepZ() * k;
			b.put(sx, y0 + 5, sz, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis()));
			if (k < 0 || k >= ribs.length || k % 2 == 1) {
				continue;
			}
			for (int side = -1; side <= 1; side += 2) {
				for (int[] c : RIB) {
					int x = sx + across.getStepX() * c[0] * side;
					int z = sz + across.getStepZ() * c[0] * side;
					Direction.Axis axis = c[0] >= 2 && c[1] <= 4 || c[1] == 1 ? Direction.Axis.Y : across.getAxis();
					b.put(x, y0 + c[1], z, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
				}
			}
		}
	}

	/** The winding tower's ring: the 24 cells round its 5x5 core, clockwise from the start corner, as {dx, dz}. */
	private static int[][] ring(int start) {
		int[][] cells = new int[24][];
		int i = 0;
		int[][] corners = { { -3, -3 }, { 3, -3 }, { 3, 3 }, { -3, 3 } };
		for (int side = 0; side < 4; side++) {
			int[] c = corners[(side + start) % 4];
			int[] n = corners[(side + start + 1) % 4];
			int sx = Integer.signum(n[0] - c[0]);
			int sz = Integer.signum(n[1] - c[1]);
			for (int k = 1; k <= 6; k++) {
				cells[i++] = new int[] { c[0] + sx * k, c[1] + sz * k };
			}
		}
		return cells;
	}

	private static boolean ringCorner(int[] cell) {
		return Math.abs(cell[0]) == 3 && Math.abs(cell[1]) == 3;
	}

	/** Each ring cell's tread height above the camp's ground (0 where no stair is). */
	private static int[] ringHeights(Winding wt) {
		int[] heights = new int[24];
		int h = 0;
		int[][] cells = ring(wt.start);
		for (int i = 0; i < 24 && h < wt.risers; i++) {
			if (!ringCorner(cells[i])) {
				h++;
			}
			heights[i] = h;
		}
		return heights;
	}

	/** The last stair of the winding tower's ring. */
	private static int ringLast(Winding wt) {
		int[] heights = ringHeights(wt);
		int last = 0;
		for (int i = 0; i < 24; i++) {
			if (heights[i] > 0) {
				last = i;
			}
		}
		return last;
	}

	/** The core cell a ring cell stands against. */
	private static int[] ringInner(int[] cell) {
		return new int[] { Integer.signum(cell[0]) * Math.min(2, Math.abs(cell[0])), Integer.signum(cell[1]) * Math.min(2, Math.abs(cell[1])) };
	}

	/**
	 * A tall winding tower: a 5x5 log-and-plank core with floors inside, a stair that winds round the outside up to a
	 * railed, roofed top, doorways off the stair onto the floors it passes.
	 */
	private void winding(ClippedBuild b, Winding wt) {
		int cy = centre.getY();
		int top = cy + wt.risers;
		int[][] cells = ring(wt.start);
		int[] heights = ringHeights(wt);
		RandomSource random = RandomSource.create(wt.seed);
		// core walls and floors
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int x = wt.x + dx;
				int z = wt.z + dz;
				b.clearCover(x, z, 24);
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				boolean cornerPost = Math.abs(dx) == 2 && Math.abs(dz) == 2;
				boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				for (int y = Math.min(g + 1, cy); y < top; y++) {
					boolean floor = y <= cy || (y - cy) % 5 == 0;
					boolean band = wall && !cornerPost && y > cy && (y - cy) % 5 == 0;
					boolean window = wall && (dx == 0 || dz == 0) && y > cy + 1 && (y - cy) % 5 == 2;
					Direction.Axis along = Math.abs(dz) == 2 ? Direction.Axis.X : Direction.Axis.Z;
					b.put(x, y, z, cornerPost ? kind.log() : band ? kind.log().trySetValue(RotatedPillarBlock.AXIS, along)
							: window ? fence(along == Direction.Axis.X, along == Direction.Axis.Z, along == Direction.Axis.X, along == Direction.Axis.Z)
							: wall || floor ? kind.planks() : Blocks.AIR.defaultBlockState());
				}
				b.put(x, top, z, cornerPost ? kind.log() : kind.planks());
			}
		}
		// the stair round the outside, posts under its corner landings, doorways onto the floors it passes
		for (int i = 0; i < 24; i++) {
			if (heights[i] == 0) {
				continue;
			}
			int x = wt.x + cells[i][0];
			int z = wt.z + cells[i][1];
			int y = cy + heights[i];
			b.clearCover(x, z, 24);
			int g = b.ground(x, z);
			if (g == Integer.MIN_VALUE) {
				continue;
			}
			if (ringCorner(cells[i])) {
				for (int py = g + 1; py < y; py++) {
					b.put(x, py, z, kind.log());
				}
				b.put(x, y, z, kind.planks());
			} else {
				int[] next = cells[(i + 1) % 24];
				Direction up = Direction.getApproximateNearest(next[0] - cells[i][0], 0, next[1] - cells[i][1]);
				b.put(x, y, z, kind.woodStairs().setValue(StairBlock.FACING, up));
				if (y - 1 > g && random.nextInt(3) == 0) {
					b.put(x, y - 1, z, kind.woodStairs().setValue(StairBlock.FACING, up.getOpposite()).setValue(StairBlock.HALF, Half.TOP));
				}
				int[] wall = ringInner(cells[i]);
				if (y < top && (y - cy) % 5 == 0 && (Math.abs(wall[0]) <= 1 || Math.abs(wall[1]) <= 1)) {
					b.put(wt.x + wall[0], y + 1, wt.z + wall[1], Blocks.AIR.defaultBlockState());
					b.put(wt.x + wall[0], y + 2, wt.z + wall[1], Blocks.AIR.defaultBlockState());
				}
			}
			b.clearAbove(x, y, z, 3);
		}
		// the top: railing round the rim (open where the stair arrives), roof posts and a slab roof
		int[] arrive = ringInner(cells[ringLast(wt)]);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int x = wt.x + dx;
				int z = wt.z + dz;
				boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				boolean cornerPost = Math.abs(dx) == 2 && Math.abs(dz) == 2;
				boolean open = Math.abs(dx - arrive[0]) + Math.abs(dz - arrive[1]) <= 1;
				if (cornerPost && !(dx == arrive[0] && dz == arrive[1])) {
					for (int y = top + 1; y <= top + 3; y++) {
						b.put(x, y, z, kind.fence());
					}
				} else if (rim && !open) {
					boolean alongX = Math.abs(dz) == 2;
					b.put(x, top + 1, z, fence(alongX, !alongX, alongX, !alongX));
				}
				b.put(x, top + 4, z, kind.slab());
			}
		}
	}

	/** The winding tower's top, one above the platform's middle (archer spot, tests). */
	public BlockPos windingTop() {
		return plan.winding == null ? null : new BlockPos(plan.winding.x, centre.getY() + plan.winding.risers + 1, plan.winding.z);
	}

	/** Ground just outside the winding tower's first stair (tests). */
	public BlockPos windingFoot() {
		if (plan.winding == null) {
			return null;
		}
		int[][] cells = ring(plan.winding.start);
		int[] first = cells[0];
		int[] before = cells[23];
		return new BlockPos(plan.winding.x + 2 * before[0] - first[0], centre.getY() + 1, plan.winding.z + 2 * before[1] - first[1]);
	}

	/** Inside the skull rock, and the ground before its mouth (tests); null without one. */
	public BlockPos skullInside() {
		if (plan.skull == null) {
			return null;
		}
		Direction f = plan.skull.facing;
		return new BlockPos(plan.skull.x - f.getStepX(), centre.getY() + 1, plan.skull.z - f.getStepZ());
	}

	/** Whether the camp's skull rock is the half-buried one (tests); false without one. */
	public boolean skullBuried() {
		return plan.skull != null && plan.skull.buried;
	}

	/** The ribcage's two path ends on the ground, just outside its first and last rib (tests); null without one. */
	public BlockPos[] ribcageEnds() {
		if (plan.ribs == null) {
			return null;
		}
		Ribs ribs = plan.ribs;
		Direction d = ribs.along;
		int y = centre.getY() + 1;
		return new BlockPos[] { new BlockPos(ribs.x - d.getStepX() * 2, y, ribs.z - d.getStepZ() * 2),
				new BlockPos(ribs.x + d.getStepX() * (ribs.length + 1), y, ribs.z + d.getStepZ() * (ribs.length + 1)) };
	}

	public BlockPos skullMouth() {
		if (plan.skull == null) {
			return null;
		}
		Direction f = plan.skull.facing;
		int u = plan.skull.buried ? 3 : 0;
		Direction across = f.getClockWise();
		return new BlockPos(plan.skull.x + f.getStepX() * 6 + across.getStepX() * u, centre.getY() + 1, plan.skull.z + f.getStepZ() * 6 + across.getStepZ() * u);
	}

	/** The first ramped tower: the ground at its ramp's foot and its platform (tests); null without one. */
	public BlockPos[] rampEnds() {
		for (Tower t : plan.towers) {
			if (t.ramp != null) {
				int[] foot = towerCell(t.x, t.z, t.ramp, t.height + 1, 0);
				return new BlockPos[] { new BlockPos(foot[0], centre.getY() + 1, foot[1]), new BlockPos(t.x, centre.getY() + t.height + 1, t.z) };
			}
		}
		return null;
	}

	/** The first bridge's two platforms (tests); null without one. */
	public BlockPos[] bridgeEnds() {
		if (plan.bridges.isEmpty()) {
			return null;
		}
		Bridge bridge = plan.bridges.getFirst();
		Tower a = bridge.from;
		int y = centre.getY() + a.height + 1;
		int[] far = towerCell(a.x, a.z, bridge.dir, bridge.length + 2, 0);
		return new BlockPos[] { new BlockPos(a.x, y, a.z), new BlockPos(far[0], y, far[1]) };
	}

	/** The kind's fence, connected east, south, west and north as given (placed without neighbour updates, so set here). */
	private BlockState fence(boolean east, boolean south, boolean west, boolean north) {
		return kind.fence().setValue(net.minecraft.world.level.block.CrossCollisionBlock.EAST, east)
				.setValue(net.minecraft.world.level.block.CrossCollisionBlock.SOUTH, south)
				.setValue(net.minecraft.world.level.block.CrossCollisionBlock.WEST, west)
				.setValue(net.minecraft.world.level.block.CrossCollisionBlock.NORTH, north);
	}

	/** Corner of a 4x4 (0..3 each way): 0 north-west, 1 north-east, 2 south-east, 3 south-west. */
	private static int cornerIndex(int dx, int dz) {
		return dx == 0 ? (dz == 0 ? 0 : 3) : (dz == 0 ? 1 : 2);
	}

	/** A standing camp banner on a short fence post. */
	private void standingBanner(ClippedBuild b, int x, int z, int rotation) {
		int g = b.ground(x, z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		b.clearAbove(x, g, z, 3);
		b.put(x, g + 1, z, kind.fence());
		b.put(x, g + 2, z, CampBanners.standing(kind).defaultBlockState().setValue(BannerBlock.ROTATION, rotation));
		if (b.inside(x, g + 2, z)) {
			CampBanners.paint(b.level().getBlockEntity(new BlockPos(x, g + 2, z)), kind, b.level().registryAccess());
		}
	}

	/** A bone-and-skull rock outcrop: a rough mound of the kind's rock, bone blocks jutting out, skulls on and round it. */
	private void outcrop(ClippedBuild b, Spot spot) {
		RandomSource random = RandomSource.create(spot.seed);
		int m = spot.size;
		float[] rolls = new float[(2 * m + 3) * (2 * m + 3) * 3];
		for (int i = 0; i < rolls.length; i++) {
			rolls[i] = random.nextFloat();
		}
		int bones = 2 + random.nextInt(3);
		int[][] boneAt = new int[bones][3];
		for (int i = 0; i < bones; i++) {
			boneAt[i][0] = random.nextInt(2 * m + 3) - m - 1;
			boneAt[i][1] = random.nextInt(2 * m + 3) - m - 1;
			boneAt[i][2] = random.nextInt(3);
		}
		int skullRot = random.nextInt(16);
		int ri = 0;
		for (int dx = -m - 1; dx <= m + 1; dx++) {
			for (int dz = -m - 1; dz <= m + 1; dz++) {
				float roll = rolls[ri++];
				float shape = rolls[ri++];
				float kindRoll = rolls[ri++];
				double d = Math.sqrt(dx * dx + dz * dz);
				int h = (int) Math.round(m + 1.5 - d + (shape - 0.5F));
				if (h <= 0) {
					continue;
				}
				int x = spot.x + dx;
				int z = spot.z + dz;
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				for (int y = g + 1; y <= g + h; y++) {
					b.put(x, y, z, kind.rock(kindRoll * 0.7F + roll * 0.3F));
				}
			}
		}
		// Bones jutting out, and a skull on top.
		for (int[] bone : boneAt) {
			int x = spot.x + bone[0];
			int z = spot.z + bone[1];
			int g = b.ground(x, z);
			if (g == Integer.MIN_VALUE) {
				continue;
			}
			Direction.Axis axis = Direction.Axis.values()[bone[2]];
			b.put(x, g + 1, z, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
		}
		int g = b.ground(spot.x, spot.z);
		if (g != Integer.MIN_VALUE) {
			b.putIfAir(spot.x, g + 1, spot.z, kind.skull().setValue(SkullBlock.ROTATION, skullRot));
		}
	}

	/** A skull on a pike: a fence post two high with a skull on top. */
	private void pike(ClippedBuild b, Spot spot) {
		int rot = (int) Math.floorMod(spot.seed, 16L);
		int g = b.ground(spot.x, spot.z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		b.clearAbove(spot.x, g, spot.z, 4);
		b.put(spot.x, g + 1, spot.z, kind.fence());
		b.put(spot.x, g + 2, spot.z, kind.fence());
		b.put(spot.x, g + 3, spot.z, kind.skull().setValue(SkullBlock.ROTATION, rot));
	}

	/** The foot of the room's stair tunnel, outside the room: a cell to stand in at its i-th stair (tests). */
	public BlockPos tunnelStep(int i) {
		int h = roomHalf();
		return roomCentre().relative(plan.stairs, h + 1 + i).atY(roomFloor() + i + 1);
	}

	/** The lean-to's front middle (screenshots, tests). */
	public BlockPos leanToSpot() {
		Spot first = plan.leanTos.getFirst();
		return new BlockPos(first.x, centre.getY() + 1, first.z);
	}

	/** Which way a lean-to opens: toward the fire. */
	private Direction leanToFacing(Spot spot) {
		return leanToFacing(spot, centre);
	}

	/**
	 * A lean-to, the camp's guaranteed shade above ground: open toward the fire, 3 wide and 2 deep under a slab roof
	 * four up (three blocks of headroom), fence posts at the front corners, a plank back wall, bedrolls (carpet) on the floor.
	 */
	private void leanTo(ClippedBuild b, Spot spot) {
		RandomSource random = RandomSource.create(spot.seed);
		boolean grey = random.nextBoolean();
		Direction facing = leanToFacing(spot);
		Direction along = facing.getClockWise();
		int roofY = centre.getY() + 4; // three blocks of headroom
		for (int k = -1; k <= 1; k++) {
			for (int row = 0; row <= 2; row++) {
				int x = spot.x + along.getStepX() * k - facing.getStepX() * row;
				int z = spot.z + along.getStepZ() * k - facing.getStepZ() * row;
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				for (int y = g + 1; y < roofY; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState()); // headroom under the roof
				}
				if (row == 2) {
					for (int y = g + 1; y < roofY; y++) {
						b.put(x, y, z, kind.planks()); // the back wall
					}
				} else if (row == 0 && k != 0) {
					for (int y = g + 1; y < roofY; y++) {
						b.put(x, y, z, kind.fence()); // front posts
					}
				} else if (row == 1 && (k != 0 || grey)) {
					b.put(x, g + 1, z, Blocks.CARPET.pick(grey ? net.minecraft.world.item.DyeColor.GRAY : net.minecraft.world.item.DyeColor.BROWN)
							.defaultBlockState());
				}
				b.put(x, roofY, z, kind.slab());
			}
		}
	}

	// --- the underground room ---------------------------------------------------------------------

	/** The room's interior half-width toward the stairs. */
	private int roomHalf() {
		return plan.stairs.getAxis() == Direction.Axis.X ? plan.roomHalfX : plan.roomHalfZ;
	}

	/** The middle of the room's floor-level air (screenshots, tests). */
	public BlockPos roomSpot() {
		return roomCentre().atY(roomFloor() + 1);
	}

	private BlockPos roomCentre() {
		return centre.offset(plan.roomDx, 0, plan.roomDz);
	}

	/** The room's floor block level (its air is the three above). */
	private int roomFloor() {
		return centre.getY() - 5;
	}

	/**
	 * One small-to-medium room under the camp: dark, rock walls,
	 * a beamed plank ceiling under the fire, trodden floor, cobwebs in the corners and a few bones, and a 2-wide stair
	 * tunnel up to the surface on the side away from the first tower. Carved last, so nothing built above blocks it.
	 */
	private void room(ClippedBuild b) {
		int h = roomHalf();
		int hx = plan.roomHalfX;
		int hz = plan.roomHalfZ;
		int floor = roomFloor();
		int cx = roomCentre().getX();
		int cz = roomCentre().getZ();
		Direction out = plan.stairs;
		Direction side = plan.stairsClockwise ? out.getClockWise() : out.getCounterClockWise();
		Direction.Axis beams = out.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		for (int dx = -hx - 1; dx <= hx + 1; dx++) {
			for (int dz = -hz - 1; dz <= hz + 1; dz++) {
				int x = cx + dx;
				int z = cz + dz;
				boolean wall = Math.abs(dx) == hx + 1 || Math.abs(dz) == hz + 1;
				RandomSource cell = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ (x * 73428767L) ^ (z * 912931L) ^ 0x900DL);
				float roll = cell.nextFloat();
				float wallRoll = cell.nextFloat();
				b.put(x, floor, z, wall ? kind.rock(roll) : fullBlock(kind.trodden(roll)));
				for (int y = floor + 1; y <= floor + 3; y++) {
					b.put(x, y, z, wall ? kind.rock(wallRoll) : Blocks.AIR.defaultBlockState());
				}
				boolean beam = !wall && Math.floorMod(out.getAxis() == Direction.Axis.X ? dx : dz, 2) == 0;
				b.put(x, floor + 4, z, wall ? kind.rock(roll) : beam ? kind.log().trySetValue(RotatedPillarBlock.AXIS, beams) : kind.planks());
				boolean corner = Math.abs(dx) == hx && Math.abs(dz) == hz;
				if (corner && roll < 0.7F) {
					b.put(x, floor + 3, z, Blocks.COBWEB.defaultBlockState());
				} else if (!wall && !corner && roll < 0.06F) {
					b.put(x, floor + 1, z, Blocks.BONE_BLOCK.defaultBlockState());
				}
			}
		}
		// room chest: the far side from the stairs, in a corner
		int chestX = cx - out.getStepX() * h + (side.getStepX() != 0 ? -side.getStepX() * hx : 0);
		int chestZ = cz - out.getStepZ() * h + (side.getStepZ() != 0 ? -side.getStepZ() * hz : 0);
		if (b.inside(chestX, floor + 1, chestZ)) {
			b.put(chestX, floor + 1, chestZ, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, out));
			RandomizableContainer.setBlockEntityLootTable(b.level(), RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0x500DL), new BlockPos(chestX, floor + 1, chestZ),
					Camps.CAMP_ROOM_LOOT);
		}
		// The stair tunnel: from the room's edge up to the ground, 2 wide, walled either side.
		for (int w = 0; w <= 1; w++) {
			for (int i = 0; i <= 6; i++) {
				int x = cx + out.getStepX() * (h + 1 + i) + side.getStepX() * w;
				int z = cz + out.getStepZ() * (h + 1 + i) + side.getStepZ() * w;
				int step = floor + i;
				if (i >= 1 && i <= 5) {
					b.put(x, step, z, kind.stairs().setValue(net.minecraft.world.level.block.StairBlock.FACING, out));
				} else if (i == 0) {
					b.put(x, step, z, fullBlock(kind.trodden(0.1F)));
				}
				int clearTo = i >= 3 ? Math.max(step + 3, centre.getY() + 2) : step + 3;
				for (int y = step + 1; y <= clearTo; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				int wx = x + side.getStepX() * (w == 0 ? -1 : 1);
				int wz = z + side.getStepZ() * (w == 0 ? -1 : 1);
				for (int y = step; y <= Math.min(step + 3, centre.getY()); y++) {
					if (b.inside(wx, y, wz) && !b.level().getBlockState(new BlockPos(wx, y, wz)).isSolid()) {
						b.put(wx, y, wz, kind.rock(0.1F));
					}
				}
			}
		}
		// the tunnel's mouth: trees and plants cleared round it, back to the clearing
		for (int i = 3; i <= 9; i++) {
			for (int w = -1; w <= 2; w++) {
				b.clearCover(cx + out.getStepX() * (h + 1 + i) + side.getStepX() * w, cz + out.getStepZ() * (h + 1 + i) + side.getStepZ() * w, 20);
			}
		}
		for (int i = 0; i <= h + 1; i++) {
			for (int w = -1; w <= 2; w++) {
				b.clearCover(centre.getX() + out.getStepX() * (size.radius + i) + side.getStepX() * w,
						centre.getZ() + out.getStepZ() * (size.radius + i) + side.getStepZ() * w, 20);
			}
		}
	}

	/**
	 * Shade spots: cells a mob can stand in out of the sun. The room's floor cells (exact
	 * height), under the lean-to and under each tower's platform (height near the camp's ground), and on each roofed
	 * platform. Heights are resolved against the world when used (camp/CampShade).
	 */
	public List<BlockPos> shadeSpots() {
		List<BlockPos> spots = new ArrayList<>();
		BlockPos room = roomCentre();
		for (int dx = -plan.roomHalfX; dx <= plan.roomHalfX; dx++) {
			for (int dz = -plan.roomHalfZ; dz <= plan.roomHalfZ; dz++) {
				spots.add(new BlockPos(room.getX() + dx, roomFloor() + 1, room.getZ() + dz));
			}
		}
		for (Spot leanTo : plan.leanTos) {
			Direction facing = leanToFacing(leanTo);
			Direction along = facing.getClockWise();
			for (int k = -1; k <= 1; k++) {
				for (int row = 0; row <= 1; row++) {
					if (row == 0 && k != 0) {
						continue;
					}
					spots.add(new BlockPos(leanTo.x + along.getStepX() * k - facing.getStepX() * row, centre.getY() + 1,
							leanTo.z + along.getStepZ() * k - facing.getStepZ() * row));
				}
			}
		}
		if (plan.skull != null) {
			Direction f = plan.skull.facing;
			Direction across = f.getClockWise();
			int sink = plan.skull.buried ? SKULL_BURIED_SINK : 0;
			for (int u = -2; u <= 2; u++) {
				for (int w = -3; w <= 1; w++) {
					if (skullHollow(u, w, sink + 1) && skullHollow(u, w, sink + 3)) {
						spots.add(new BlockPos(plan.skull.x + across.getStepX() * u + f.getStepX() * w, centre.getY() + 1,
								plan.skull.z + across.getStepZ() * u + f.getStepZ() * w));
					}
				}
			}
		}
		if (plan.winding != null) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					spots.add(windingTop().offset(dx, 0, dz));
				}
			}
		}
		for (Tower t : plan.towers) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					spots.add(new BlockPos(t.x + dx, centre.getY() + 1, t.z + dz));
					spots.add(new BlockPos(t.x + dx, centre.getY() + t.height + 1, t.z + dz));
				}
			}
		}
		return spots;
	}
}

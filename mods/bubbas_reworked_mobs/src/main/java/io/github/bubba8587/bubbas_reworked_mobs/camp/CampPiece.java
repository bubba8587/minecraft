// dte:C1,B15,B17,B21
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.bosspiglin.ThroneDesign;
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
		super(Camps.CAMP_PIECE, 0, box(centre, size, kind));
		this.seed = seed;
		this.centre = centre;
		this.kind = kind;
		this.size = size;
		this.plan = new Plan(seed, centre, size, kind);
	}

	public CampPiece(CompoundTag tag) {
		super(Camps.CAMP_PIECE, tag);
		this.seed = tag.getLongOr("Seed", 0L);
		this.centre = BlockPos.of(tag.getLongOr("Centre", 0L));
		this.kind = CampKind.values()[Math.floorMod(tag.getIntOr("Kind", 0), CampKind.values().length)];
		this.size = CampTuning.Size.values()[Math.floorMod(tag.getIntOr("Size", 0), CampTuning.Size.values().length)];
		this.plan = new Plan(seed, centre, size, kind);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Seed", seed);
		tag.putLong("Centre", centre.asLong());
		tag.putInt("Kind", kind.ordinal());
		tag.putInt("Size", size.ordinal());
	}

	static BoundingBox box(BlockPos centre, CampTuning.Size size, CampKind kind) {
		// clearing plus edge outcrops (up to 3 past the radius), or the stair tunnel
		int r = Math.max(size.radius + 5, size.maxRoomHalf + 10) + (kind.nether() ? 3 : 0);
		if (size == CampTuning.Size.LARGE && !kind.nether()) {
			r = Math.max(r, 10 + Plan.SKULL_YARD_CLEARING + 1); // a skull rock 10 out and its yard
		}
		return new BoundingBox(centre.getX() - r, centre.getY() - (kind.nether() ? 8 : 6), centre.getZ() - r, centre.getX() + r, centre.getY() + 22, centre.getZ() + r);
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

	/** A throne: its front-left floor cell and the way it faces. */
	private record ThroneSpot(int x, int z, Direction facing) {
	}

	/** A throne's cells: every column of its design, plus the two rows in front it gets up into. */
	private static Footprint throneFootprint(int x, int z, Direction facing) {
		int minX = x;
		int minZ = z;
		int maxX = x;
		int maxZ = z;
		for (ThroneDesign.Placed placed : ThroneDesign.blocks(facing)) {
			minX = Math.min(minX, x + placed.offset().getX());
			minZ = Math.min(minZ, z + placed.offset().getZ());
			maxX = Math.max(maxX, x + placed.offset().getX());
			maxZ = Math.max(maxZ, z + placed.offset().getZ());
		}
		Direction right = facing.getClockWise();
		for (int u = 0; u <= 1; u++) {
			int fx = x + facing.getStepX() * 2 + right.getStepX() * u;
			int fz = z + facing.getStepZ() * 2 + right.getStepZ() * u;
			minX = Math.min(minX, fx);
			minZ = Math.min(minZ, fz);
			maxX = Math.max(maxX, fx);
			maxZ = Math.max(maxZ, fz);
		}
		return new Footprint("throne", minX, minZ, maxX, maxZ);
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
		/** The Nether outpost's gate (size: the direction it faces out, 2D data value) and hoglin pens. */
		Spot gate;
		final List<Spot> pens = new ArrayList<>();
		/** The stable (front middle, facing the fire), or null. */
		Spot stable;
		/** The Nether outpost's throne (its front-left floor cell, facing the fire), or null. */
		ThroneSpot throne;
		final Direction stairs;
		final boolean stairsClockwise;
		final Direction chestSide;
		final double spawnAngle;
		final int bannerRotation;
		final int roomHalfX;
		final int roomHalfZ;
		final int roomDx;
		final int roomDz;

		Plan(long seed, BlockPos centre, CampTuning.Size size, CampKind kind) {
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
			// never toward the stairs; centred when one wide toward them
			if (alongX && (dx * stairs.getStepX() > 0 || hx == 1)) {
				dx = 0;
			}
			if (!alongX && (dz * stairs.getStepZ() > 0 || hz == 1)) {
				dz = 0;
			}
			roomHalfX = kind.nether() ? NETHER_ROOM_HALF : hx;
			roomHalfZ = kind.nether() ? NETHER_ROOM_HALF : hz;
			roomDx = dx;
			roomDz = dz;
			stairsClockwise = random.nextBoolean();
			chestSide = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			spawnAngle = random.nextDouble() * Math.PI * 2.0;
			bannerRotation = random.nextInt(16);
			// reserved first: the fire with its seats and chest, and the stair tunnel where it opens to the surface, with its mouth and walls
			footprints.add(Footprint.around("fire", cx, cz, 2));
			Direction side = stairsClockwise ? stairs.getClockWise() : stairs.getCounterClockWise();
			int h = alongX ? roomHalfX : roomHalfZ;
			int fx = cx + dx;
			int fz = cz + dz;
			int ax = fx + stairs.getStepX() * (h + 2) - side.getStepX();
			int az = fz + stairs.getStepZ() * (h + 2) - side.getStepZ();
			int bx = fx + stairs.getStepX() * (h + 10 + (kind.nether() ? 2 : 0)) + side.getStepX() * 2;
			int bz = fz + stairs.getStepZ() * (h + 10 + (kind.nether() ? 2 : 0)) + side.getStepZ() * 2;
			footprints.add(new Footprint("stairs", Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz)));
			// then each piece on free cells only, its spot re-rolled until one fits, or dropped
			// rare centrepieces of large camps: a skull rock at the edge, a winding tower
			if (!kind.nether() && size == CampTuning.Size.LARGE && random.nextInt(3) == 0) {
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
				if (skull != null) {
					skullYard(random, cx, cz);
				}
			}
			if (!kind.nether() && size == CampTuning.Size.LARGE && random.nextInt(4) == 0) {
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
			if (!kind.nether() && size == CampTuning.Size.LARGE && random.nextInt(3) == 0) {
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
			// Nether outposts: a gilded gate in a stretch of rampart at the edge, and a hoglin pen
			if (kind.nether()) {
				List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
				net.minecraft.util.Util.shuffle(sides, random);
				for (Direction d : sides) {
					int x = cx + d.getStepX() * r;
					int z = cz + d.getStepZ() * r;
					Direction across = d.getClockWise();
					int gax = x - across.getStepX() * GATE_REACH;
					int gaz = z - across.getStepZ() * GATE_REACH;
					int gbx = x + across.getStepX() * GATE_REACH;
					int gbz = z + across.getStepZ() * GATE_REACH;
					if (claim(new Footprint("gate", Math.min(gax, gbx), Math.min(gaz, gbz), Math.max(gax, gbx), Math.max(gaz, gbz)))) {
						gate = new Spot(x, z, d.get2DDataValue(), random.nextLong());
						break;
					}
				}
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES * 2; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int x = cx + (int) Math.round(Math.cos(a) * r);
					int z = cz + (int) Math.round(Math.sin(a) * r);
					if (claim(Footprint.around("pen", x, z, PEN_HALF))) {
						pens.add(new Spot(x, z, PEN_HALF, pieceSeed));
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
			// the stable, from its own random stream
			RandomSource stableRandom = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed ^ 0x57AB1EL));
			if (!kind.nether() && (size == CampTuning.Size.LARGE || size == CampTuning.Size.MEDIUM && stableRandom.nextFloat() < CampTuning.MEDIUM_STABLE_CHANCE)) {
				long pieceSeed = stableRandom.nextLong();
				for (int t = 0; t < TRIES * 5; t++) {
					double a = stableRandom.nextDouble() * Math.PI * 2.0;
					int d = r - 4 + t % 5;
					Spot spot = new Spot(cx + (int) Math.round(Math.cos(a) * d), cz + (int) Math.round(Math.sin(a) * d), 0, pieceSeed);
					if (claim(stableFootprint(spot, centre))) {
						stable = spot;
						break;
					}
				}
			}
			// the throne (medium and large outposts): after the lean-tos and stable, before towers and dressing; its own random stream
			if (kind.nether() && size != CampTuning.Size.SMALL) {
				RandomSource throneRandom = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed ^ 0x7420E5L));
				int span = Math.max(1, r - THRONE_MIN_DISTANCE);
				for (int t = 0; t < TRIES * 16 && throne == null; t++) {
					double a = throneRandom.nextDouble() * Math.PI * 2.0;
					int d = THRONE_MIN_DISTANCE + t % span;
					int x = cx + (int) Math.round(Math.cos(a) * d);
					int z = cz + (int) Math.round(Math.sin(a) * d);
					Direction facing = Direction.getApproximateNearest(cx - x, 0.0, cz - z);
					if (claim(throneFootprint(x, z, facing))) {
						throne = new ThroneSpot(x, z, facing);
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
				for (int t = 0; t < TRIES * 4; t++) {
					double a = random.nextDouble() * Math.PI * 2.0;
					int d = (t < TRIES ? 3 : 4) + random.nextInt(Math.max(1, r - 4));
					int x = cx + (int) Math.round(Math.cos(a) * d);
					int z = cz + (int) Math.round(Math.sin(a) * d);
					if (claim(Footprint.around("banner", x, z, 0))) {
						banners.add(new Spot(x, z, 0, pieceSeed));
						break;
					}
				}
			}
		}

		/**
		 * Dressing round a skull rock's sides and back (its front faces the fire): pikes, small outcrops and a banner
		 * SKULL_YARD_REACH out from its middle, so the skull sits in the camp rather than at its edge.
		 */
		private void skullYard(RandomSource random, int cx, int cz) {
			double front = Math.atan2(cz - skull.z, cx - skull.x);
			int count = SKULL_YARD_MIN + random.nextInt(SKULL_YARD_MAX - SKULL_YARD_MIN + 1);
			for (int i = 0; i < count; i++) {
				// spread over the sides and back: from 70 degrees off the front round to 290
				double base = front + Math.toRadians(70.0 + 220.0 * (i + 0.5) / count);
				int what = i == 0 ? 2 : random.nextInt(2);
				long pieceSeed = random.nextLong();
				for (int t = 0; t < TRIES; t++) {
					double a = base + (random.nextDouble() - 0.5) * 0.5;
					int d = SKULL_YARD_REACH + random.nextInt(2);
					int x = skull.x + (int) Math.round(Math.cos(a) * d);
					int z = skull.z + (int) Math.round(Math.sin(a) * d);
					if (what == 1 && claim(Footprint.around("outcrop", x, z, 2))) {
						outcrops.add(new Spot(x, z, 1, pieceSeed));
						break;
					}
					if (what == 2 && claim(Footprint.around("banner", x, z, 0))) {
						banners.add(new Spot(x, z, 0, pieceSeed));
						break;
					}
					if (what == 0 && claim(Footprint.around("pike", x, z, 0))) {
						pikes.add(new Spot(x, z, 0, pieceSeed));
						break;
					}
				}
			}
		}

		static final int SKULL_YARD_MIN = 4;
		static final int SKULL_YARD_MAX = 6;
		static final int SKULL_YARD_REACH = 8;
		/** Trodden ground round a skull rock's middle, out to this. */
		static final int SKULL_YARD_CLEARING = 10;
		static final int NETHER_ROOM_HALF = 3;
		static final int GATE_REACH = 5;
		static final int PEN_HALF = 2;
		/** The throne's front-left cell is at least this far from the fire. */
		static final int THRONE_MIN_DISTANCE = 3;
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

	/** A stable's cells: STABLE_HALF either side of its front middle, STABLE_DEPTH rows back from the fire. */
	private static Footprint stableFootprint(Spot spot, BlockPos centre) {
		Direction facing = leanToFacing(spot, centre);
		Direction along = facing.getClockWise();
		int[] a = { spot.x - along.getStepX() * STABLE_HALF, spot.z - along.getStepZ() * STABLE_HALF };
		int[] b = { spot.x + along.getStepX() * STABLE_HALF - facing.getStepX() * (STABLE_DEPTH - 1),
				spot.z + along.getStepZ() * STABLE_HALF - facing.getStepZ() * (STABLE_DEPTH - 1) };
		return new Footprint("stable", Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[0], b[0]), Math.max(a[1], b[1]));
	}

	/** Stable size: 7 wide (3 either side of the gate), 5 deep, ring included. */
	static final int STABLE_HALF = 3;
	static final int STABLE_DEPTH = 5;

	private static Direction leanToFacing(Spot spot, BlockPos centre) {
		return Direction.getApproximateNearest(centre.getX() - spot.x, 0.0, centre.getZ() - spot.z);
	}

	// --- building ---------------------------------------------------------------------------------

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource chunkRandom, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos) {
		ClippedBuild b = kind.nether() ? new NetherBuild(level, chunkBB, centre.getY()) : new ClippedBuild(level, chunkBB);
		int r = size.radius;
		int cx = centre.getX();
		int cz = centre.getZ();
		// Clearing: trees and plants out, trodden ground, weeds at the edge (round the fire, and round a skull rock).
		int reach = plan.skull == null ? r + 1 : 10 + Plan.SKULL_YARD_CLEARING + 1;
		for (int x = cx - reach; x <= cx + reach; x++) {
			for (int z = cz - reach; z <= cz + reach; z++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				if (plan.skull != null) {
					double ds = Math.sqrt((x - plan.skull.x) * (x - plan.skull.x) + (z - plan.skull.z) * (z - plan.skull.z));
					d = Math.min(d, ds - Plan.SKULL_YARD_CLEARING + r);
				}
				if (b instanceof NetherBuild nether && d <= r + 1.5) {
					nether.seal(x, z, centre.getY() - 9, centre.getY() + HEADROOM);
				}
				if (d > r + 0.5) {
					continue;
				}
				RandomSource column = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ (x * 341873128712L) ^ (z * 132897987541L));
				float tread = column.nextFloat();
				float what = column.nextFloat();
				float weed = column.nextFloat();
				b.clearCover(x, z, 20);
				if (b instanceof NetherBuild nether) {
					nether.carve(x, z, centre.getY() + HEADROOM);
					levelWalk(nether, x, z, what);
				}
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
		if (plan.stable != null) {
			stable(b, plan.stable);
		}
		if (plan.gate != null) {
			gate(b, plan.gate);
		}
		for (Spot pen : plan.pens) {
			pen(b, pen);
		}
		if (roomFloor() > level.getMinY()) {
			room(b); // (a superflat world is too shallow for it)
		}
		b.tidyFences(getBoundingBox());
		if (plan.throne != null) {
			throne(b, plan.throne); // after the fence tidy: the design's own blocks stay as drawn
		}
	}

	/** The boss's throne (bosspiglin/ThroneDesign) on levelled ground, room cleared round and over it. */
	private void throne(ClippedBuild b, ThroneSpot t) {
		int y0 = centre.getY();
		Footprint area = throneFootprint(t.x, t.z, t.facing);
		for (int x = area.minX(); x <= area.maxX(); x++) {
			for (int z = area.minZ(); z <= area.maxZ(); z++) {
				b.clearCover(x, z, 6);
				if (b instanceof NetherBuild nether) {
					nether.carve(x, z, y0 + 5);
				}
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				for (int y = g + 1; y < y0; y++) {
					b.put(x, y, z, Blocks.NETHERRACK.defaultBlockState()); // levelled
				}
				for (int y = y0 + 1; y <= y0 + 4; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				if (b.inside(x, y0, z) && !b.level().getBlockState(new BlockPos(x, y0, z)).isFaceSturdy(b.level(), new BlockPos(x, y0, z), Direction.UP)) {
					b.put(x, y0, z, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
				}
			}
		}
		for (ThroneDesign.Placed placed : ThroneDesign.blocks(t.facing)) {
			BlockPos at = new BlockPos(t.x, y0 + 1, t.z).offset(placed.offset());
			b.put(at.getX(), at.getY(), at.getZ(), placed.state());
		}
	}

	/** The throne, or null (small outposts, other kinds). */
	public CampSite.Throne throne() {
		if (plan.throne == null) {
			return null;
		}
		return new CampSite.Throne(new BlockPos(plan.throne.x, centre.getY() + 1, plan.throne.z), plan.throne.facing, java.util.Optional.of(portalInside()));
	}

	/** Where the seated boss's feet go on its throne (tests); null without one. */
	public net.minecraft.world.phys.Vec3 throneSeat() {
		CampSite.Throne throne = throne();
		return throne == null ? null : throne.seat();
	}

	/** A cell inside the portal's frame, at its foot (Nether outposts). */
	public BlockPos portalInside() {
		return portalBase().relative(portalAlong(), 1).above();
	}

	/** A floor block mobs can't sink into: mud becomes packed mud. */
	private static BlockState fullBlock(BlockState state) {
		return state.is(Blocks.MUD) ? Blocks.PACKED_MUD.defaultBlockState() : state;
	}

	private static boolean natural(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MUD)
				|| state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
				|| state.is(Blocks.NETHERRACK) || state.is(BlockTags.NYLIUM) || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.BLACKSTONE)
				|| state.is(Blocks.GRAVEL) || state.is(Blocks.MAGMA_BLOCK);
	}

	/** The unlit soul campfire, a stone ring round it, log seats on three sides and the loot chest. */
	private void fire(ClippedBuild b, int cx, int cz) {
		int g = b.ground(cx, cz);
		if (g != Integer.MIN_VALUE) {
			b.put(cx, g, cz, kind.hearth());
			b.put(cx, g + 1, cz, (kind.nether() ? Blocks.CAMPFIRE : Blocks.SOUL_CAMPFIRE).defaultBlockState().setValue(CampfireBlock.LIT, false));
			b.clearAbove(cx, g + 1, cz, 4);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			int x = cx + side.getStepX();
			int z = cz + side.getStepZ();
			int gs = b.ground(x, z);
			if (gs != Integer.MIN_VALUE) {
				b.put(x, gs, z, kind.hearth());
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
			RandomizableContainer.setBlockEntityLootTable(b.level(), RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed) ^ 0xC4E57L), new BlockPos(x, gc + 1, z), kind.loot(false));
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
				if (!corner && !railAt(t, alongX ? dx - 1 : dx, alongX ? dz : dz - 1, ladderCorner, railGone)
						&& !railAt(t, alongX ? dx + 1 : dx, alongX ? dz : dz + 1, ladderCorner, railGone)) {
					continue; // no lone rail segment
				}
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
			if (kind.nether()) {
				hang(b, x0 + 2, top + 3, z0 + 2, random.nextBoolean());
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

	/** Whether a tower's platform edge cell (0..3 each way) carries a fence one above the planks (railing, corner or banner post). */
	private static boolean railAt(Tower t, int dx, int dz, int ladderCorner, boolean[] railGone) {
		boolean corner = (dx == 0 || dx == 3) && (dz == 0 || dz == 3);
		if (corner) {
			return cornerIndex(dx, dz) != ladderCorner;
		}
		Direction edgeSide = dx == 3 ? Direction.EAST : dx == 0 ? Direction.WEST : dz == 0 ? Direction.NORTH : Direction.SOUTH;
		return !railGone[dx * 4 + dz] && !t.opens(edgeSide);
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
				b.put(c[0], top, c[1], kind.deck(sag));
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
	/** How high a Nether outpost clears its clearing above its ground. */
	static final int HEADROOM = 12;

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
					b.put(x, y, z, what == 2 ? Blocks.AIR.defaultBlockState() : skullBone(u, w, dy, roll, across.getAxis()));
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
		// the skull chest, at the back inside
		BlockPos chest = skullChest();
		if (chest != null && b.inside(chest.getX(), chest.getY(), chest.getZ())) {
			b.put(chest.getX(), chest.getY(), chest.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, f));
			RandomizableContainer.setBlockEntityLootTable(b.level(), RandomSource.create(s.seed ^ 0x5C011L), chest, Camps.CAMP_SKULL_LOOT);
		}
	}

	/** The skull chest's cell: the hollow floor cell furthest back, beside the middle path (null without a skull). */
	public BlockPos skullChest() {
		Skull s = plan.skull;
		if (s == null) {
			return null;
		}
		int sink = s.buried ? SKULL_BURIED_SINK : 0;
		for (int w = -6; w <= 5; w++) {
			for (int u : new int[] { 1, -1, 2, -2 }) {
				if (skullCell(u, w, sink + 1, s.buried) == 2 && skullCell(u, w, sink + 2, s.buried) == 2 && skullHollow(u, w, sink + 1)) {
					Direction across = s.facing.getClockWise();
					return new BlockPos(s.x + across.getStepX() * u + s.facing.getStepX() * w, centre.getY() + 1, s.z + across.getStepZ() * u + s.facing.getStepZ() * w);
				}
			}
		}
		return null;
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
		// the archer's stash, shut in the core's ground floor (seen through the slits)
		BlockPos chest = windingChest();
		if (b.inside(chest.getX(), chest.getY(), chest.getZ())) {
			b.put(chest.getX(), chest.getY(), chest.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, windingChestFacing()));
			RandomizableContainer.setBlockEntityLootTable(b.level(), RandomSource.create(wt.seed ^ 0x70C4L), chest, Camps.CAMP_TOWER_LOOT);
		}
	}

	/** The winding tower's chest: a corner of its core's ground floor, from the tower's seed (null without one). */
	public BlockPos windingChest() {
		Winding wt = plan.winding;
		if (wt == null) {
			return null;
		}
		int corner = (int) Math.floorMod(wt.seed >>> 7, 4L);
		return new BlockPos(wt.x + (corner == 1 || corner == 2 ? 1 : -1), centre.getY() + 1, wt.z + (corner >= 2 ? 1 : -1));
	}

	private Direction windingChestFacing() {
		BlockPos chest = windingChest();
		return chest.getX() > plan.winding.x ? Direction.WEST : Direction.EAST;
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

	/** The kind's fence joined toward the given sides. */
	private BlockState fenceToward(Direction... sides) {
		java.util.Set<Direction> set = java.util.Set.of(sides);
		return fence(set.contains(Direction.EAST), set.contains(Direction.SOUTH), set.contains(Direction.WEST), set.contains(Direction.NORTH));
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
			b.put(x, g + 1, z, kind.accent().trySetValue(RotatedPillarBlock.AXIS, axis));
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
		b.put(spot.x, g + 3, spot.z, kind.nether() && (spot.seed & 16L) == 0L ? lantern((spot.seed & 32L) == 0L, false)
				: kind.skull().setValue(SkullBlock.ROTATION, rot));
	}

	/** A hanging lantern, only under something it can hang from. */
	private static void hang(ClippedBuild b, int x, int y, int z, boolean soul) {
		BlockPos above = new BlockPos(x, y + 1, z);
		if (b.inside(x, y, z) && b.level().getBlockState(above).isFaceSturdy(b.level(), above, Direction.DOWN)) {
			b.put(x, y, z, lantern(soul, true));
		}
	}

	/** A lantern or soul lantern, standing or hanging. */
	private static BlockState lantern(boolean soul, boolean hanging) {
		return (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, hanging);
	}

	/**
	 * The Nether outpost's gate in a stretch of rampart (bastion-style): two thick polished blackstone brick pillars capped
	 * with gilded blackstone and gold, an arched lintel over a 3-wide opening, wall either side with blackstone wall on top
	 * and soul lanterns hung under the arch.
	 */
	private void gate(ClippedBuild b, Spot gate) {
		Direction out = Direction.from2DDataValue(gate.size);
		Direction across = out.getClockWise();
		int y0 = centre.getY();
		for (int u = -Plan.GATE_REACH; u <= Plan.GATE_REACH; u++) {
			int x = gate.x + across.getStepX() * u;
			int z = gate.z + across.getStepZ() * u;
			int au = Math.abs(u);
			b.clearCover(x, z, 12);
			int g = b.ground(x, z);
			if (g == Integer.MIN_VALUE) {
				continue;
			}
			int top = au <= 1 ? y0 + 5 : au == 2 ? y0 + 6 : y0 + 4 - (au == Plan.GATE_REACH ? 1 : 0);
			for (int y = g + 1; y < y0 + 1; y++) {
				b.put(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()); // footing
			}
			if (au <= 1) {
				b.put(x, y0, z, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				b.put(x, y0 + 4, z, au == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
						: Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, u < 0 ? across : across.getOpposite())
								.setValue(StairBlock.HALF, Half.TOP));
				b.put(x, y0 + 5, z, au == 0 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
				if (au == 1) {
					b.put(x, y0 + 6, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
				}
				continue;
			}
			for (int y = y0 + 1; y <= top; y++) {
				boolean cap = au == 2 && y == top;
				b.put(x, y, z, cap ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : au == 2 || Math.floorMod(u * 13 + y * 7, 10) < 7
						? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
			}
			b.put(x, top + 1, z, au == 2 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.BLACKSTONE_WALL.defaultBlockState());
		}
		// the approach: a walk out from the gate
		if (b instanceof NetherBuild nether) {
			for (int k = 1; k <= 5; k++) {
				for (int u = -2; u <= 2; u++) {
					levelWalk(nether, gate.x + out.getStepX() * k + across.getStepX() * u, gate.z + out.getStepZ() * k + across.getStepZ() * u,
							Math.floorMod(k * 31 + u * 7, 10) / 10.0F);
				}
			}
			for (int u = -1; u <= 1; u++) {
				levelWalk(nether, gate.x - out.getStepX() + across.getStepX() * u, gate.z - out.getStepZ() + across.getStepZ() * u, 0.3F);
			}
		}
		// lanterns hung under the arch, either side
		for (int u : new int[] { -1, 1 }) {
			hang(b, gate.x + across.getStepX() * u, y0 + 3, gate.z + across.getStepZ() * u, true);
		}
	}

	/**
	 * A hoglin pen: a closed ring of crimson fence 5 across round trodden nylium and soul soil, a tether post (fence with a
	 * lantern on top) in the middle for a leashed hoglin, and a crimson fungus or two as fodder.
	 */
	private void pen(ClippedBuild b, Spot pen) {
		int m = pen.size;
		int y0 = centre.getY();
		for (int dx = -m; dx <= m; dx++) {
			for (int dz = -m; dz <= m; dz++) {
				int x = pen.x + dx;
				int z = pen.z + dz;
				b.clearCover(x, z, 6);
				if (b instanceof NetherBuild nether) {
					nether.carve(x, z, y0 + 4);
				}
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				for (int y = g + 1; y < y0; y++) {
					b.put(x, y, z, Blocks.NETHERRACK.defaultBlockState()); // levelled
				}
				for (int y = Math.max(g + 1, y0 + 1); y <= g; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				float roll = RandomSource.create(pen.seed ^ (x * 73428767L) ^ (z * 912931L)).nextFloat();
				b.put(x, y0, z, roll < 0.6F ? Blocks.CRIMSON_NYLIUM.defaultBlockState() : roll < 0.85F ? Blocks.SOUL_SOIL.defaultBlockState()
						: Blocks.NETHERRACK.defaultBlockState());
				boolean ring = Math.abs(dx) == m || Math.abs(dz) == m;
				if (ring) {
					b.put(x, y0 + 1, z, Blocks.CRIMSON_FENCE.defaultBlockState());
				} else if (dx == 0 && dz == 0) {
					b.put(x, y0 + 1, z, Blocks.CRIMSON_FENCE.defaultBlockState());
					b.put(x, y0 + 2, z, lantern(false, false));
				} else if (roll < 0.2F) {
					b.put(x, y0, z, Blocks.CRIMSON_NYLIUM.defaultBlockState());
					b.put(x, y0 + 1, z, Blocks.CRIMSON_FUNGUS.defaultBlockState());
				}
			}
		}
	}

	/** The hoglin pens' tether posts (the fence in each middle); empty for other kinds. */
	public List<BlockPos> tetherPosts() {
		List<BlockPos> posts = new ArrayList<>();
		for (Spot pen : plan.pens) {
			posts.add(new BlockPos(pen.x, centre.getY() + 1, pen.z));
		}
		if (plan.stable != null) {
			posts.add(stableCell(0, STABLE_DEPTH - 2).above());
		}
		return posts;
	}

	/** Whether the camp has a stable (tethered mounts; patrols may ride out). */
	public boolean hasStable() {
		return plan.stable != null;
	}

	/** A stable cell at floor level: u across (0 the gate), row back from the fire (0 the front fence). */
	private BlockPos stableCell(int u, int row) {
		Spot spot = plan.stable;
		Direction facing = leanToFacing(spot);
		Direction along = facing.getClockWise();
		return new BlockPos(spot.x + along.getStepX() * u - facing.getStepX() * row, centre.getY(), spot.z + along.getStepZ() * u - facing.getStepZ() * row);
	}

	/** The stable's middle inside, one above its floor (screenshots, tests); null without one. */
	public BlockPos stableInside() {
		return plan.stable == null ? null : stableCell(0, 2).above();
	}

	/** Which way the stable opens: toward the fire; null without one. */
	public Direction stableFacing() {
		return plan.stable == null ? null : leanToFacing(plan.stable);
	}

	/**
	 * A stable: a roofed pen of the kind's wood, opening toward the fire. Log corner posts, a plank back wall, a fence
	 * rail round the sides and front with a gate in the middle, a slab roof four up, a levelled trodden floor with hay at
	 * the back and a tether post between the bales.
	 */
	private void stable(ClippedBuild b, Spot spot) {
		RandomSource random = RandomSource.create(spot.seed);
		boolean secondBale = random.nextBoolean();
		int baleSide = random.nextBoolean() ? 1 : -1;
		Direction facing = leanToFacing(spot);
		int y0 = centre.getY();
		int roofY = y0 + 4;
		for (int u = -STABLE_HALF; u <= STABLE_HALF; u++) {
			for (int row = 0; row < STABLE_DEPTH; row++) {
				BlockPos cell = stableCell(u, row);
				int x = cell.getX();
				int z = cell.getZ();
				b.clearCover(x, z, 20);
				int g = b.ground(x, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				float roll = RandomSource.create(spot.seed ^ (x * 73428767L) ^ (z * 912931L)).nextFloat();
				for (int y = g + 1; y < y0; y++) {
					b.put(x, y, z, kind.rock(roll)); // levelled
				}
				for (int y = y0 + 1; y < roofY; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				b.put(x, y0, z, fullBlock(kind.trodden(roll)));
				boolean side = Math.abs(u) == STABLE_HALF;
				boolean back = row == STABLE_DEPTH - 1;
				if (side && (row == 0 || back)) {
					for (int y = y0 + 1; y < roofY; y++) {
						b.put(x, y, z, kind.log()); // corner posts
					}
				} else if (back) {
					for (int y = y0 + 1; y < roofY; y++) {
						b.put(x, y, z, kind.planks());
					}
				} else if (row == 0 && u == 0) {
					b.put(x, y0 + 1, z, kind.fenceGate().setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, facing));
				} else if (side) {
					b.put(x, y0 + 1, z, fenceToward(facing, facing.getOpposite())); // side rails, sides preset
				} else if (row == 0) {
					b.put(x, y0 + 1, z, fenceToward(facing.getClockWise(), facing.getCounterClockWise()));
				} else if (row == STABLE_DEPTH - 2 && u == 0) {
					b.put(x, y0 + 1, z, fenceToward(facing.getOpposite())); // the tether post, against the back wall
				} else if (row == STABLE_DEPTH - 2 && (u == 2 * baleSide || secondBale && u == -2 * baleSide)) {
					b.put(x, y0 + 1, z, Blocks.HAY_BLOCK.defaultBlockState());
				}
				b.put(x, roofY, z, kind.slab());
			}
		}
	}

	/** The gate's opening on the ground, and a cell inside the camp in front of it (tests); null without one. */
	public BlockPos[] gateEnds() {
		if (plan.gate == null) {
			return null;
		}
		Direction out = Direction.from2DDataValue(plan.gate.size);
		int y = centre.getY() + 1;
		return new BlockPos[] { new BlockPos(plan.gate.x + out.getStepX() * 2, y, plan.gate.z + out.getStepZ() * 2),
				new BlockPos(plan.gate.x - out.getStepX() * 3, y, plan.gate.z - out.getStepZ() * 3) };
	}

	/** The foot of the room's stair tunnel, outside the room: a cell to stand in at its i-th stair (tests). */
	public BlockPos tunnelStep(int i) {
		int h = roomHalf();
		return roomCentre().relative(plan.stairs, h + 1 + i).atY(roomFloor() + i + 1);
	}

	/** The tunnel's top stair block (its first column; tests). */
	public BlockPos tunnelTop() {
		return roomCentre().relative(plan.stairs, roomHalf() + 1 + roomDepth()).atY(roomFloor() + roomDepth());
	}

	/** Which way the stair tunnel climbs out of the room. */
	public Direction stairsOut() {
		return plan.stairs;
	}

	/** A cell on the clearing beside the fire, on its open side (no seat), one above the ground (tests). */
	public BlockPos fireSide() {
		return centre.relative(plan.chestSide.getOpposite(), 2).above();
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

	/** Air above each tunnel step (and above the room's doorway cell). */
	static final int TUNNEL_HEADROOM = 4;

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
		return centre.getY() - roomDepth();
	}

	/** How far below the ground the room's floor is: deeper in a Nether outpost, for the portal. */
	private int roomDepth() {
		return kind.nether() ? 7 : 5;
	}

	/** The portal's state: lit, whole but unlit, or broken (a frame block or two missing). */
	public enum Portal {
		LIT, UNLIT, BROKEN
	}

	/** The Nether outpost room's portal state, by the camp's seed; null for other kinds. */
	public Portal portal() {
		if (!kind.nether()) {
			return null;
		}
		int roll = (int) Math.floorMod(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed ^ 0x9047A1L), 10L);
		return roll < 4 ? Portal.LIT : roll < 7 ? Portal.UNLIT : Portal.BROKEN;
	}

	/** The portal's bottom frame corner, on the floor at the back of the room, its whole frame in one chunk (tests). */
	public BlockPos portalBase() {
		Direction out = plan.stairs;
		int h = roomHalf();
		BlockPos row = roomCentre().relative(out, -(h - 1)).atY(roomFloor() + 1);
		Direction along = portalAlong();
		for (int s : new int[] { -1, 0, -2, -3 }) {
			BlockPos a = row.relative(along, s);
			BlockPos b = row.relative(along, s + 3);
			if (a.getX() >> 4 == b.getX() >> 4 && a.getZ() >> 4 == b.getZ() >> 4) {
				return a; // the frame in one chunk
			}
		}
		return row.relative(along, -1);
	}

	/** Which way the portal faces: toward the stairs. */
	public Direction portalStairs() {
		return plan.stairs;
	}

	public Direction portalAlong() {
		return plan.stairsClockwise ? plan.stairs.getClockWise() : plan.stairs.getCounterClockWise();
	}

	/**
	 * A Nether portal at the back of the room, facing the stairs (ruined-portal style): an obsidian frame 4 wide and 5
	 * tall, lit, whole and unlit, or broken with a block or two gone; crying obsidian in some of it, and magma and
	 * netherrack scattered at its foot.
	 */
	/** Sets a portal or frame block without shape updates (a portal half-built across a chunk edge would otherwise go out). */
	private static void quiet(ClippedBuild b, BlockPos at, BlockState state) {
		if (b.inside(at.getX(), at.getY(), at.getZ())) {
			b.level().setBlock(at, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
		}
	}

	private void portalFrame(ClippedBuild b) {
		Portal state = portal();
		RandomSource random = RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed ^ 0x9047A2L));
		BlockPos base = portalBase();
		Direction along = portalAlong();
		int missing = state == Portal.BROKEN ? 1 + random.nextInt(2) : 0;
		List<int[]> frame = new ArrayList<>();
		for (int u = 0; u <= 3; u++) {
			for (int v = 0; v <= 4; v++) {
				boolean edge = u == 0 || u == 3 || v == 0 || v == 4;
				if (edge) {
					frame.add(new int[] { u, v });
				}
			}
		}
		int crying = state == Portal.LIT ? 0 : 1 + random.nextInt(3);
		List<int[]> upper = new ArrayList<>(frame.stream().filter(c -> c[1] > 0).toList());
		net.minecraft.util.Util.shuffle(upper, random);
		List<int[]> gone = upper.subList(0, missing);
		for (int[] c : frame) {
			BlockPos at = base.relative(along, c[0]).above(c[1]);
			boolean corner = (c[0] == 0 || c[0] == 3) && (c[1] == 0 || c[1] == 4);
			BlockState block = gone.contains(c) ? Blocks.AIR.defaultBlockState()
					: crying > 0 && !corner && random.nextInt(4) == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
			if (block.is(Blocks.CRYING_OBSIDIAN)) {
				crying--;
			}
			quiet(b, at, block);
		}
		BlockState inside = state == Portal.LIT
				? Blocks.NETHER_PORTAL.defaultBlockState().setValue(net.minecraft.world.level.block.NetherPortalBlock.AXIS, along.getAxis())
				: Blocks.AIR.defaultBlockState();
		for (int u = 1; u <= 2; u++) {
			for (int v = 1; v <= 3; v++) {
				quiet(b, base.relative(along, u).above(v), inside);
			}
		}
		// the ruin at its foot, on the room floor in front
		Direction front = plan.stairs;
		for (int u = -1; u <= 4; u++) {
			BlockPos at = base.relative(along, u).relative(front).below();
			float roll = random.nextFloat();
			if ((u == -1 || u == 4) && roll < 0.5F) {
				b.put(at.getX(), at.getY(), at.getZ(), Blocks.MAGMA_BLOCK.defaultBlockState());
			} else if (roll < 0.5F) {
				b.put(at.getX(), at.getY(), at.getZ(), Blocks.NETHERRACK.defaultBlockState());
			}
		}
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
				for (int y = floor + 1; y <= floor + roomDepth() - 2; y++) {
					b.put(x, y, z, wall ? kind.rock(wallRoll) : Blocks.AIR.defaultBlockState());
				}
				boolean beam = !wall && Math.floorMod(out.getAxis() == Direction.Axis.X ? dx : dz, 2) == 0;
				b.put(x, floor + roomDepth() - 1, z, wall ? kind.rock(roll) : beam ? kind.log().trySetValue(RotatedPillarBlock.AXIS, beams) : kind.planks());
				boolean corner = Math.abs(dx) == hx && Math.abs(dz) == hz;
				if (corner && roll < 0.7F) {
					b.put(x, floor + roomDepth() - 2, z, Blocks.COBWEB.defaultBlockState());
				} else if (!wall && !corner && !kind.nether() && roll < 0.06F && dx * out.getStepX() + dz * out.getStepZ() < h) {
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
					kind.loot(true));
		}
		if (kind.nether()) {
			portalFrame(b);
		}
		// The stair tunnel: from the room's edge up to the ground, 2 wide, walled either side.
		for (int w = 0; w <= 1; w++) {
			for (int i = 0; i <= roomDepth() + 1; i++) {
				int x = cx + out.getStepX() * (h + 1 + i) + side.getStepX() * w;
				int z = cz + out.getStepZ() * (h + 1 + i) + side.getStepZ() * w;
				int step = floor + i;
				if (i >= 1 && i <= roomDepth()) {
					b.put(x, step, z, kind.stairs().setValue(net.minecraft.world.level.block.StairBlock.FACING, out));
				} else if (i == 0) {
					b.put(x, step, z, fullBlock(kind.trodden(0.1F)));
				}
				// headroom
				int clearTo = i >= 3 ? Math.max(step + TUNNEL_HEADROOM, centre.getY() + 2) : step + TUNNEL_HEADROOM;
				for (int y = step + 1; y <= clearTo; y++) {
					b.put(x, y, z, Blocks.AIR.defaultBlockState());
				}
				// no falling ground over the tunnel
				if (b.inside(x, clearTo + 1, z) && b.level().getBlockState(new BlockPos(x, clearTo + 1, z)).getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
					b.put(x, clearTo + 1, z, kind.rock(0.1F));
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
		for (int i = 3; i <= roomDepth() + 4; i++) {
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
		if (!kind.nether()) {
			// the way out: flush with the top step, then a block a cell up to the land
			for (int i = roomDepth() + 1; i <= roomDepth() + MOUTH_LEVEL + MOUTH_SLOPE; i++) {
				int cap = centre.getY() + Math.max(0, i - roomDepth() - MOUTH_LEVEL);
				for (int w = 0; w <= 1; w++) {
					mouthColumn(b, cx + out.getStepX() * (h + 1 + i) + side.getStepX() * w, cz + out.getStepZ() * (h + 1 + i) + side.getStepZ() * w, cap,
							i <= roomDepth() + MOUTH_LEVEL);
				}
			}
		}
		if (b instanceof NetherBuild nether) {
			// under a ceiling: a level walk from the clearing's edge out round the tunnel to its mouth
			int end = h + 1 + roomDepth() + 4;
			for (int k = 0; k <= end; k++) {
				for (int w = -2; w <= 3; w++) {
					int x = cx + out.getStepX() * k + side.getStepX() * w;
					int z = cz + out.getStepZ() * k + side.getStepZ() * w;
					boolean trench = w >= 0 && w <= 1 && k >= h + 1 && k <= h + 1 + roomDepth();
					if (trench || Math.hypot(x - centre.getX(), z - centre.getZ()) <= size.radius - 1) {
						continue;
					}
					levelWalk(nether, x, z, (k * 7 + w) % 10 / 10.0F);
				}
			}
		}
	}

	/** Tunnel cells past the top step kept level with it, then cells over which the way out may rise a block each. */
	static final int MOUTH_LEVEL = 3;
	static final int MOUTH_SLOPE = 6;

	/**
	 * One column of the tunnel's way out: land above {@code cap} cut back to it (only earth, and only where nothing
	 * stands on it), and with {@code fill} a dip filled up to it.
	 */
	private void mouthColumn(ClippedBuild b, int x, int z, int cap, boolean fill) {
		int g = b.ground(x, z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		if (b.inside(x, g + 1, z) && weed(b.level().getBlockState(new BlockPos(x, g + 1, z)))) {
			b.put(x, g + 1, z, Blocks.AIR.defaultBlockState()); // nothing underfoot at the way out
		}
		if (g > cap) {
			for (int y = cap + 1; y <= g + 3; y++) {
				BlockState state = b.level().getBlockState(new BlockPos(x, y, z));
				if (!(y <= g ? earth(state) || ClippedBuild.cover(state) : ClippedBuild.cover(state) || weed(state))) {
					return;
				}
			}
			b.clearCover(x, z, 20);
			for (int y = cap + 1; y <= g + 1; y++) {
				b.put(x, y, z, Blocks.AIR.defaultBlockState());
			}
		} else if (g < cap && fill) {
			for (int y = g + 1; y <= cap + 3; y++) {
				if (!ClippedBuild.cover(b.level().getBlockState(new BlockPos(x, y, z)))) {
					return;
				}
			}
			for (int y = g + 1; y < cap; y++) {
				b.put(x, y, z, kind.rock(0.1F));
			}
			b.put(x, cap, z, fullBlock(kind.trodden(0.1F)));
		}
	}

	/** The camp's own weeds that aren't counted as cover (they need the ground under them). */
	private static boolean weed(BlockState state) {
		return state.is(Blocks.MOSS_CARPET) || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.DEAD_BUSH);
	}

	/** Natural ground and rock (and the camp's trodden ground). */
	private static boolean earth(BlockState state) {
		return natural(state) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.SANDSTONE)
				|| state.is(Blocks.SMOOTH_SANDSTONE) || state.is(Blocks.PACKED_MUD) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.ICE)
				|| state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.CLAY) || state.is(Blocks.MOSS_BLOCK)
				|| state.is(Blocks.CALCITE);
	}

	/** Under a ceiling: one column of walkway at the outpost's own height, its growth and rock cleared over it and filled under it. */
	private void levelWalk(NetherBuild b, int x, int z, float roll) {
		b.clearCover(x, z, 16);
		b.carve(x, z, centre.getY() + 4);
		int g = b.ground(x, z);
		if (g == Integer.MIN_VALUE || g < centre.getY() - NetherBuild.BELOW + 2) {
			g = centre.getY() - 3;
		}
		for (int y = g + 1; y < centre.getY(); y++) {
			b.put(x, y, z, kind.rock(0.7F));
		}
		BlockPos floor = new BlockPos(x, centre.getY(), z);
		if (g < centre.getY() || !natural(b.level().getBlockState(floor))) {
			b.put(x, centre.getY(), z, fullBlock(kind.trodden(roll)));
		}
		for (int y = centre.getY() + 1; y <= Math.max(g, centre.getY() + 4); y++) {
			b.put(x, y, z, Blocks.AIR.defaultBlockState());
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
			BlockPos chest = skullChest();
			for (int u = -2; u <= 2; u++) {
				for (int w = -3; w <= 1; w++) {
					if (skullHollow(u, w, sink + 1) && skullHollow(u, w, sink + 3)) {
						BlockPos spot = new BlockPos(plan.skull.x + across.getStepX() * u + f.getStepX() * w, centre.getY() + 1,
								plan.skull.z + across.getStepZ() * u + f.getStepZ() * w);
						if (!spot.equals(chest)) {
							spots.add(spot);
						}
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

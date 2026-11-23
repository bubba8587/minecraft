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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
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
		int r = Math.max(size.radius + 3, size.maxRoomHalf + 10);
		return new BoundingBox(centre.getX() - r, centre.getY() - 6, centre.getZ() - r, centre.getX() + r, centre.getY() + 18, centre.getZ() + r);
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

	private record Tower(int x, int z, int height, Direction ladder, boolean roof, int bannerCorner) {
	}

	private record Spot(int x, int z, int size, long seed) {
	}

	private static final class Plan {
		final List<Tower> towers = new ArrayList<>();
		final List<Spot> banners = new ArrayList<>();
		final List<Spot> outcrops = new ArrayList<>();
		final List<Spot> pikes = new ArrayList<>();
		final List<Spot> leanTos = new ArrayList<>();
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
			double towerAngle = random.nextDouble() * Math.PI * 2.0;
			int towerCount = random.nextInt(size.maxTowers + 1);
			for (int i = 0; i < towerCount; i++) {
				double a = towerAngle + i * (Math.PI * 2.0 / towerCount) + (random.nextDouble() - 0.5) * 0.8;
				int d = r - 2;
				towers.add(new Tower(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d),
						5 + random.nextInt(3), Direction.Plane.HORIZONTAL.getRandomDirection(random), true, random.nextInt(4)));
			}
			// banners: the first flies from the first tower when there is one
			for (int i = towers.isEmpty() ? 0 : 1; i < size.banners; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				int d = 3 + random.nextInt(Math.max(1, r - 4));
				banners.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d), 0,
						random.nextLong()));
			}
			int outcropCount = random.nextInt(size.outcrops + 2);
			for (int i = 0; i < outcropCount; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				int d = r - 2 + random.nextInt(2);
				outcrops.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d),
						1 + random.nextInt(2), random.nextLong()));
			}
			int pikeCount = random.nextInt(size.outcrops + 3);
			for (int i = 0; i < pikeCount; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				int d = r - 1;
				pikes.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d), 0, random.nextLong()));
			}
			int leanToCount = 1 + random.nextInt(size == CampTuning.Size.SMALL ? 1 : 2);
			double la = random.nextDouble() * Math.PI * 2.0;
			for (int i = 0; i < leanToCount; i++) {
				double a = la + i * Math.PI * (0.7 + random.nextDouble() * 0.6);
				leanTos.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * (r - 3)), centre.getZ() + (int) Math.round(Math.sin(a) * (r - 3)), 0,
						random.nextLong()));
			}
			// the room: size, shape and offset; its stairs come up on any side but the first tower's
			Direction towerSide = towers.isEmpty() ? null : Direction.getApproximateNearest(towers.getFirst().x - centre.getX(), 0.0,
					towers.getFirst().z - centre.getZ());
			Direction pick;
			do {
				pick = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			} while (pick == towerSide);
			stairs = pick;
			// toward the stairs the room keeps the old reach, so the tunnel still surfaces inside the clearing
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
		}
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
				if (ci == ladderCorner || (!corner && railGone[i])) {
					continue; // where the ladder comes up, and gaps
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
		return Direction.getApproximateNearest(centre.getX() - spot.x, 0.0, centre.getZ() - spot.z);
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

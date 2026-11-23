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
 * cold (unlit) soul campfire in the middle ringed by log seats (owner: camps are dark, no light at all), a chest of camp loot beside it; scrappy lookout towers (log posts,
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
		int r = size.radius + 1;
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
		final Spot leanTo;
		final Direction chestSide;
		final double spawnAngle;
		final int bannerRotation;

		Plan(long seed, BlockPos centre, CampTuning.Size size) {
			RandomSource random = RandomSource.create(seed);
			int r = size.radius;
			double towerAngle = random.nextDouble() * Math.PI * 2.0;
			for (int i = 0; i < size.towers; i++) {
				double a = towerAngle + i * (Math.PI * (0.8 + random.nextDouble() * 0.4));
				int d = r - 2; // clear of the fire's seats; the 4x4 stays inside the box
				towers.add(new Tower(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d),
						5 + random.nextInt(3), Direction.Plane.HORIZONTAL.getRandomDirection(random), random.nextFloat() < 0.5F, random.nextInt(4)));
			}
			// The first banner flies from the first tower; the rest stand about the camp.
			for (int i = 1; i < size.banners; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				int d = 3 + random.nextInt(Math.max(1, r - 4));
				banners.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d), 0,
						random.nextLong()));
			}
			for (int i = 0; i < size.outcrops; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				int d = r - 2 + random.nextInt(2);
				outcrops.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d),
						1 + random.nextInt(2), random.nextLong()));
			}
			int pikeCount = 1 + random.nextInt(size.outcrops + 1);
			for (int i = 0; i < pikeCount; i++) {
				double a = random.nextDouble() * Math.PI * 2.0;
				int d = r - 1;
				pikes.add(new Spot(centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d), 0, random.nextLong()));
			}
			double la = random.nextDouble() * Math.PI * 2.0;
			leanTo = size == CampTuning.Size.SMALL ? null
					: new Spot(centre.getX() + (int) Math.round(Math.cos(la) * (r - 4)), centre.getZ() + (int) Math.round(Math.sin(la) * (r - 4)), 0,
							random.nextLong());
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
				RandomSource column = RandomSource.create(seed ^ (x * 341873128712L) ^ (z * 132897987541L));
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
				} else if (d > r - 2.5 && weed < 0.15F && level.getBlockState(at).isFaceSturdy(level, at, Direction.UP)) {
					b.putIfAir(x, g + 1, z, kind.weed(what));
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
		if (plan.leanTo != null) {
			leanTo(b, plan.leanTo);
		}
	}

	private static boolean natural(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MUD)
				|| state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM);
	}

	/** The unlit soul campfire (camps have no light, owner 2026-11-22), a stone ring round it, log seats on three sides and the loot chest. */
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
			RandomizableContainer.setBlockEntityLootTable(b.level(), RandomSource.create(seed ^ 0xC4E57L), new BlockPos(x, gc + 1, z), Camps.CAMP_LOOT);
		}
	}

	/**
	 * A scrappy lookout tower on stilts: four thin log posts at the corners of a 4x4, a fence brace part way up, a plank
	 * platform with a few planks sagging to slabs, connected fence railing with gaps, a ladder up the outside of one
	 * post, sometimes a slab roof on fence posts, and the camp's banner flying from another post.
	 */
	private void tower(ClippedBuild b, Tower t) {
		RandomSource random = RandomSource.create(seed ^ (t.x * 31L + t.z) ^ 0x70E7L);
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
				}
			}
		}
		if (t.roof) {
			for (int dx = 0; dx <= 3; dx++) {
				for (int dz = 0; dz <= 3; dz++) {
					if (random.nextFloat() < 0.75F) {
						b.put(x0 + dx, top + 3, z0 + dz, kind.slab());
					}
				}
			}
		}
		// The ladder: up the outer face of a corner post, arriving beside the open post top.
		int[] lc = corners[ladderCorner];
		Direction out = ladderAlongX ? (lc[0] == 0 ? Direction.WEST : Direction.EAST) : (lc[1] == 0 ? Direction.NORTH : Direction.SOUTH);
		int lx = x0 + lc[0] + out.getStepX();
		int lz = z0 + lc[1] + out.getStepZ();
		int gl = b.ground(lx, lz);
		if (gl != Integer.MIN_VALUE) {
			for (int y = gl + 1; y <= top + 1; y++) {
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

	/** A lean-to: three fence posts, a sloping slab roof, bedrolls (carpet) under it. */
	private void leanTo(ClippedBuild b, Spot spot) {
		RandomSource random = RandomSource.create(spot.seed);
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		boolean grey = random.nextBoolean();
		Direction along = facing.getClockWise();
		int roofY = centre.getY() + 3;
		for (int k = -1; k <= 1; k++) {
			int fx = spot.x + along.getStepX() * k;
			int fz = spot.z + along.getStepZ() * k;
			// Front: posts and the high edge of the roof.
			int g = b.ground(fx, fz);
			if (g != Integer.MIN_VALUE) {
				if (k != 0) {
					for (int y = g + 1; y < roofY; y++) {
						b.put(fx, y, fz, kind.fence());
					}
				}
				b.put(fx, roofY, fz, kind.slab());
			}
			// Back: the low edge, on the ground behind, and a bedroll under the roof.
			int mx = fx - facing.getStepX();
			int mz = fz - facing.getStepZ();
			int gm = b.ground(mx, mz);
			if (gm != Integer.MIN_VALUE) {
				b.put(mx, roofY, mz, kind.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
				b.put(mx, roofY - 1, mz, kind.slab().setValue(SlabBlock.TYPE, SlabType.TOP));
				if (k != 0 || grey) {
					b.putIfAir(mx, gm + 1, mz, grey ? Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState() : Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState());
				}
			}
			int bx = mx - facing.getStepX();
			int bz = mz - facing.getStepZ();
			int gb = b.ground(bx, bz);
			if (gb != Integer.MIN_VALUE) {
				for (int y = gb + 1; y < roofY - 1; y++) {
					b.put(bx, y, bz, kind.planks());
				}
				b.put(bx, roofY - 1, bz, kind.slab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
			}
		}
	}
}

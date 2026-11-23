// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.Vec3;

/**
 * Cave spider nests (specs/spiders.md "Cave nests"): cobwebs only (owner), clinging to the ceiling at the top of a
 * tall cave room, with a few sleepers hanging in them and a {@link SpiderNest} brain at the centre. Placed once per
 * chunk (placement list empty, so the origin is the chunk's corner), rolling {@link SpiderTuning#NEST_CHUNK_CHANCE};
 * then a few random columns are searched top-down for a ceiling with a long open drop under it and open air around it.
 * B15: nothing new as blocks, only vanilla cobwebs.
 */
public record SpiderNestFeature() implements Feature {
	public static final MapCodec<SpiderNestFeature> CODEC = MapCodec.unit(SpiderNestFeature::new);

	@Override
	public MapCodec<SpiderNestFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (random.nextFloat() >= SpiderTuning.NEST_CHUNK_CHANCE) {
			return false;
		}
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		for (int i = 0; i < SpiderTuning.NEST_COLUMN_TRIES; i++) {
			// Keep the nest inside the chunk and its neighbours (worldgen may write one chunk out).
			int x = baseX + 4 + random.nextInt(8);
			int z = baseZ + 4 + random.nextInt(8);
			int drop = findCeiling(level, x, z, random);
			if (drop != Integer.MIN_VALUE) {
				return true;
			}
		}
		return false;
	}

	/** Searches one column for a tall room's ceiling and builds a nest there. Returns the drop, or MIN_VALUE (tests use it too). */
	private static int findCeiling(WorldGenLevel level, int x, int z, RandomSource random) {
		return findCeiling(level, x, z, Math.min(SpiderTuning.NEST_MAX_Y, level.getMaxY() - 1),
				Math.max(SpiderTuning.NEST_MIN_Y, level.getMinY() + 1), random);
	}

	/** The same between {@code top} and {@code bottom} (tests search their own room). */
	public static int findCeiling(WorldGenLevel level, int x, int z, int top, int bottom, RandomSource random) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
		boolean solidAbove = solid(level, pos);
		for (int y = top - 1; y >= bottom; y--) {
			pos.setY(y);
			boolean solidHere = solid(level, pos);
			if (solidAbove && !solidHere && level.getBlockState(pos).isAir()) {
				// pos is the air just under a ceiling: measure the open drop below.
				int drop = 0;
				BlockPos.MutableBlockPos below = pos.mutable();
				while (drop < 40 && below.getY() > level.getMinY() && level.getBlockState(below).isAir()) {
					drop++;
					below.move(Direction.DOWN);
				}
				if (drop >= SpiderTuning.NEST_MIN_DROP && roomTop(level, pos)) {
					build(level, pos.immutable(), drop, random);
					return drop;
				}
				y -= drop; // skip the room we just measured
				solidAbove = false;
				continue;
			}
			solidAbove = solidHere;
		}
		return Integer.MIN_VALUE;
	}

	private static boolean solid(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.isAir() && state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, pos);
	}

	/** Open air a few blocks out on every side just under the ceiling: the top of a room, not a narrow shaft. */
	private static boolean roomTop(WorldGenLevel level, BlockPos top) {
		int w = SpiderTuning.NEST_ROOM_HALF_WIDTH;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos side = top.below(2).relative(d, w);
			if (!level.getBlockState(side).isAir()) {
				return false;
			}
		}
		return true;
	}

	private static void build(WorldGenLevel level, BlockPos centre, int drop, RandomSource random) {
		int r = SpiderTuning.NEST_RADIUS;
		int webs = SpiderTuning.NEST_WEBS_MIN + random.nextInt(SpiderTuning.NEST_WEBS_MAX - SpiderTuning.NEST_WEBS_MIN + 1);
		List<BlockPos> ceilingSpots = new ArrayList<>();
		int placed = 0;
		for (int tries = 0; tries < webs * 4 && placed < webs; tries++) {
			int dx = Math.round((float) random.nextGaussian() * r * 0.55F);
			int dz = Math.round((float) random.nextGaussian() * r * 0.55F);
			if (dx * dx + dz * dz > r * r) {
				continue;
			}
			// Find the ceiling over this spot (it may be a block higher or lower than at the centre).
			BlockPos at = centre.offset(dx, 1, dz);
			int steps = 0;
			while (steps < 4 && !solid(level, at)) {
				at = at.above();
				steps++;
			}
			while (steps > -4 && solid(level, at)) {
				at = at.below();
				steps--;
			}
			if (!level.getBlockState(at).isAir() || !solid(level, at.above())) {
				continue;
			}
			level.setBlock(at, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
			placed++;
			ceilingSpots.add(at);
			// Some webs hang a second block down near the middle.
			if (dx * dx + dz * dz <= 2 && random.nextFloat() < 0.5F && level.getBlockState(at.below()).isAir()) {
				level.setBlock(at.below(), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		if (placed < SpiderTuning.NEST_MIN_WEBS_ALIVE) {
			return;
		}
		var server = level.getLevel();
		SpiderNest nest = Spiders.SPIDER_NEST.create(server, EntitySpawnReason.STRUCTURE);
		if (nest == null) {
			return;
		}
		nest.setPos(centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5);
		nest.setDepth(drop + 1);
		level.addFreshEntity(nest);
		// Sleepers, each under its own ceiling web (distinct spots, nearest the middle first).
		ceilingSpots.sort((a, b) -> Double.compare(a.distSqr(centre), b.distSqr(centre)));
		int sleepers = SpiderTuning.NEST_SLEEPERS_MIN + random.nextInt(SpiderTuning.NEST_SLEEPERS_MAX - SpiderTuning.NEST_SLEEPERS_MIN + 1);
		List<BlockPos> used = new ArrayList<>();
		for (BlockPos spot : ceilingSpots) {
			if (used.size() >= sleepers) {
				break;
			}
			if (used.stream().anyMatch(u -> u.distManhattan(spot) < 2)) {
				continue;
			}
			NestSpider spider = Spiders.NEST_SPIDER.create(server, EntitySpawnReason.STRUCTURE);
			if (spider == null) {
				return;
			}
			float yaw = random.nextFloat() * 360.0F;
			spider.setYRot(yaw);
			spider.setYBodyRot(yaw);
			spider.setYHeadRot(yaw);
			spider.hangFrom(new Vec3(spot.getX() + 0.5, spot.getY() + 1.0, spot.getZ() + 0.5));
			level.addFreshEntity(spider);
			used.add(spot);
		}
	}
}

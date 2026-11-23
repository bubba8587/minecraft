// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A Nether outpost (specs/monster-camps.md "Nether outposts"): the piglin kind of monster camp, built by the same
 * {@link CampPiece}. Under the Nether's ceiling it finds its own floor: a shelf of solid ground well above the lava sea
 * with open air over it, level enough across the clearing, with no lava at or near the surface anywhere in it.
 */
public class NetherOutpostStructure extends Structure {
	public static final MapCodec<NetherOutpostStructure> CODEC = simpleCodec(NetherOutpostStructure::new);
	/** Most the floor may rise or fall across the outpost. */
	static final int MAX_SLOPE = 5;
	/** Open air needed over the centre, and over every other sampled column. */
	static final int CENTRE_HEADROOM = 12;
	static final int HEADROOM = 5;
	/** The floor stands at least this far above the lava sea (room and portal included). */
	static final int ABOVE_LAVA = 10;
	/** Columns sampled this far apart across the clearing. */
	static final int STEP = 3;
	/** Sampled out this far past the clearing (the gate, rampart and pen stand at its edge). */
	static final int EDGE = 4;

	public NetherOutpostStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		CampTuning.Size size = CampTuning.Size.pick(random);
		long seed = random.nextLong();
		ChunkPos chunk = context.chunkPos();
		int cx = chunk.getMiddleBlockX();
		int cz = chunk.getMiddleBlockZ();
		var generator = context.chunkGenerator();
		var heights = context.heightAccessor();
		int lavaTop = generator.getSeaLevel();
		NoiseColumn centre = generator.getBaseColumn(cx, cz, heights, context.randomState());
		List<Integer> floors = new ArrayList<>();
		for (int y = lavaTop + ABOVE_LAVA; y < heights.getMinY() + generator.getGenDepth() - CENTRE_HEADROOM - 8; y++) {
			if (floor(centre, y, CENTRE_HEADROOM)) {
				floors.add(y);
			}
		}
		if (floors.isEmpty()) {
			return Optional.empty();
		}
		int y0 = floors.get(random.nextInt(floors.size()));
		if (!context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(y0 + 1), QuartPos.fromBlock(cz)))) {
			return Optional.empty();
		}
		int r = size.radius + EDGE;
		int low = y0;
		int high = y0;
		for (int dx = -r; dx <= r; dx += STEP) {
			for (int dz = -r; dz <= r; dz += STEP) {
				if (dx * dx + dz * dz > r * r || dx == 0 && dz == 0) {
					continue;
				}
				NoiseColumn column = generator.getBaseColumn(cx + dx, cz + dz, heights, context.randomState());
				int found = Integer.MIN_VALUE;
				for (int d = 0; d <= MAX_SLOPE && found == Integer.MIN_VALUE; d++) {
					if (floor(column, y0 - d, HEADROOM)) {
						found = y0 - d;
					} else if (d > 0 && floor(column, y0 + d, HEADROOM)) {
						found = y0 + d;
					}
				}
				if (found == Integer.MIN_VALUE || lavaNear(column, found)) {
					return Optional.empty();
				}
				low = Math.min(low, found);
				high = Math.max(high, found);
			}
		}
		if (high - low > MAX_SLOPE || lavaNear(centre, y0)) {
			return Optional.empty();
		}
		BlockPos at = new BlockPos(cx, y0, cz);
		return Optional.of(new GenerationStub(at, builder -> builder.addPiece(new CampPiece(seed, at, CampKind.PIGLIN, size))));
	}

	/** Solid ground at y with {@code headroom} blocks of air over it. */
	private static boolean floor(NoiseColumn column, int y, int headroom) {
		if (!solid(column.getBlock(y))) {
			return false;
		}
		for (int dy = 1; dy <= headroom; dy++) {
			if (!column.getBlock(y + dy).isAir()) {
				return false;
			}
		}
		return true;
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty();
	}

	/** Lava anywhere from 8 below the floor (the room) up past it. */
	private static boolean lavaNear(NoiseColumn column, int y) {
		for (int dy = -9; dy <= 2; dy++) {
			if (!column.getBlock(y + dy).getFluidState().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public StructureType<?> type() {
		return Camps.NETHER_OUTPOST;
	}
}

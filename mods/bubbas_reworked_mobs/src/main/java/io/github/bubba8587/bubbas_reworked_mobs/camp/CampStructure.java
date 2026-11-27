// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A monster camp (specs/monster-camps.md), placed by its structure set like any vanilla structure (so /locate finds
 * it). Its kind comes from the biome at its centre; it needs dry, fairly flat ground (bogged camps tolerate a wet edge).
 * The piece ({@link CampPiece}) builds itself from its own seed.
 */
public class CampStructure extends Structure {
	public static final MapCodec<CampStructure> CODEC = simpleCodec(CampStructure::new);
	/** Most a camp's ground may rise or fall across it. */
	private static final int MAX_SLOPE = 6;

	public CampStructure(StructureSettings settings) {
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
		if (!context.couldValidBiomeExistOnTopOfChunkCenter()) {
			return Optional.empty();
		}
		var generator = context.chunkGenerator();
		int r = size.radius;
		int[][] at = { { cx, cz }, { cx - r, cz - r }, { cx + r, cz - r }, { cx - r, cz + r }, { cx + r, cz + r } };
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int wet = 0;
		int centreY = 0;
		for (int i = 0; i < at.length; i++) {
			int surface = generator.getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			int floor = generator.getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			if (!context.validBiome().test(context.noiseBiomeResolver().getNoiseBiome(QuartPos.fromBlock(at[i][0]), QuartPos.fromBlock(surface),
					QuartPos.fromBlock(at[i][1])))) {
				return Optional.empty();
			}
			if (floor != surface) {
				wet++;
				if (i == 0) {
					return Optional.empty(); // the fire stands on dry ground
				}
			}
			low = Math.min(low, floor - 1);
			high = Math.max(high, floor - 1);
			if (i == 0) {
				centreY = surface - 1;
			}
		}
		var biome = context.noiseBiomeResolver().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(centreY), QuartPos.fromBlock(cz));
		CampKind kind = CampKind.forBiome(biome, random);
		if (high - low > MAX_SLOPE || centreY < generator.getSeaLevel() - 1 || wet > (kind == CampKind.BOGGED ? 2 : 0)) {
			return Optional.empty();
		}
		BlockPos centre = new BlockPos(cx, centreY, cz);
		return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new CampPiece(seed, centre, kind, size))));
	}

	@Override
	public StructureType<?> type() {
		return Camps.CAMP;
	}
}

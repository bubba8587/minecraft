// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.traderpost;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * A trader post (specs/monster-camps.md "Trader posts"), placed by its structure set like any vanilla structure (so
 * /locate finds it). Needs dry, nearly flat open ground in its biomes; in a desert only near its edge; never near a
 * monster camp or crypt yard.
 */
public class TraderPostStructure extends Structure {
	public static final MapCodec<TraderPostStructure> CODEC = simpleCodec(TraderPostStructure::new);
	/** Most the ground may rise or fall across the post. */
	static final int MAX_SLOPE = 4;
	/** How far out a desert post looks for another biome (blocks). */
	static final int DESERT_EDGE = 48;
	/** Chunks kept clear of camps and crypt yards. */
	static final int KEEP_CLEAR_CHUNKS = 2;
	private static final ResourceKey<StructureSet> CAMPS = ResourceKey.create(Registries.STRUCTURE_SET, BubbasReworkedMobs.id("monster_camps"));
	private static final ResourceKey<StructureSet> YARDS = ResourceKey.create(Registries.STRUCTURE_SET, BubbasReworkedMobs.id("crypt_yards"));

	public TraderPostStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		long seed = random.nextLong();
		ChunkPos chunk = context.chunkPos();
		int cx = chunk.getMiddleBlockX();
		int cz = chunk.getMiddleBlockZ();
		if (!context.couldValidBiomeExistOnTopOfChunkCenter() || nearCampOrYard(context, chunk)) {
			return Optional.empty();
		}
		var generator = context.chunkGenerator();
		int r = 10;
		int[][] at = { { cx, cz }, { cx - r, cz - r }, { cx + r, cz - r }, { cx - r, cz + r }, { cx + r, cz + r }, { cx, cz - r }, { cx, cz + r },
				{ cx - r, cz }, { cx + r, cz } };
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int sum = 0;
		for (int i = 0; i < at.length; i++) {
			int surface = generator.getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			int floor = generator.getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			if (floor != surface || !context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(at[i][0]), QuartPos.fromBlock(surface),
					QuartPos.fromBlock(at[i][1])))) {
				return Optional.empty();
			}
			low = Math.min(low, floor - 1);
			high = Math.max(high, floor - 1);
			sum += floor - 1;
		}
		int centreY = Math.round(sum / (float) at.length);
		if (high - low > MAX_SLOPE || centreY < generator.getSeaLevel()) {
			return Optional.empty();
		}
		int qy = QuartPos.fromBlock(centreY);
		if (context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(cx), qy, QuartPos.fromBlock(cz)).is(Biomes.DESERT)) {
			boolean edge = false;
			for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
				edge |= !context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(cx + d[0] * DESERT_EDGE), qy, QuartPos.fromBlock(cz + d[1] * DESERT_EDGE))
						.is(Biomes.DESERT);
			}
			if (!edge) {
				return Optional.empty();
			}
		}
		BlockPos centre = new BlockPos(cx, centreY, cz);
		return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new TraderPostPiece(seed, centre))));
	}

	/** Whether a camp or crypt yard could start within a few chunks (their placement grids, before their own checks). */
	private static boolean nearCampOrYard(GenerationContext context, ChunkPos chunk) {
		var sets = context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
		for (ResourceKey<StructureSet> key : java.util.List.of(CAMPS, YARDS)) {
			var set = sets.getValue(key);
			if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement placement)) {
				continue;
			}
			int k = KEEP_CLEAR_CHUNKS;
			for (int dx = -k; dx <= k; dx += k) {
				for (int dz = -k; dz <= k; dz += k) {
					ChunkPos p = placement.getPotentialStructureChunk(context.seed(), chunk.x() + dx, chunk.z() + dz);
					if (Math.abs(p.x() - chunk.x()) <= k && Math.abs(p.z() - chunk.z()) <= k) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public StructureType<?> type() {
		return TraderPosts.TYPE;
	}
}

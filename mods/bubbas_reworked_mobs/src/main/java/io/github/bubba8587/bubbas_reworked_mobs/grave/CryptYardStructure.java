// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

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
 * A crypt yard (specs/graveyards.md): a fenced graveyard on the surface, often with stairs down to a crypt. Placed by
 * its structure set like any vanilla structure (so /locate and the Totem of Seeking's search see it), on fairly flat,
 * dry ground; the piece ({@link CryptYardPiece}) builds itself in code from its own seed. Code-built rather than
 * jigsaw templates: the states of breakdown, sizes and contents are parameters of one builder, with no template files
 * to keep in step.
 */
public class CryptYardStructure extends Structure {
	public static final MapCodec<CryptYardStructure> CODEC = simpleCodec(CryptYardStructure::new);

	public CryptYardStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		int width = odd(GraveTuning.YARD_MIN_WIDTH + random.nextInt(GraveTuning.YARD_MAX_WIDTH - GraveTuning.YARD_MIN_WIDTH + 1));
		int depth = odd(GraveTuning.YARD_MIN_DEPTH + random.nextInt(GraveTuning.YARD_MAX_DEPTH - GraveTuning.YARD_MIN_DEPTH + 1));
		boolean crypt = random.nextFloat() < GraveTuning.CRYPT_CHANCE;
		long seed = random.nextLong();
		ChunkPos chunk = context.chunkPos();
		int minX = chunk.getMinBlockX();
		int minZ = chunk.getMinBlockZ();
		if (!context.couldValidBiomeExistOnTopOfChunkCenter()) {
			return Optional.empty();
		}
		int[] corners = new int[4];
		int[][] at = { { minX, minZ }, { minX + width - 1, minZ }, { minX, minZ + depth - 1 }, { minX + width - 1, minZ + depth - 1 } };
		int low = Integer.MAX_VALUE;
		int high = Integer.MIN_VALUE;
		int sum = 0;
		for (int i = 0; i < 4; i++) {
			// Every corner's biome must allow a yard, not only the start chunk's middle (vanilla checks only that):
			// so a yard doesn't hang over into a river or the sea beside its biome.
			int cornerY = context.chunkGenerator().getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
					context.randomState());
			if (!context.validBiome().test(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(at[i][0]), QuartPos.fromBlock(cornerY),
					QuartPos.fromBlock(at[i][1])))) {
				return Optional.empty();
			}
			int floor = context.chunkGenerator().getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
			int surface = context.chunkGenerator().getFirstOccupiedHeight(at[i][0], at[i][1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (floor != surface) {
				return Optional.empty(); // water on it
			}
			corners[i] = surface - 1;
			low = Math.min(low, corners[i]);
			high = Math.max(high, corners[i]);
			sum += corners[i];
		}
		if (high - low > GraveTuning.YARD_MAX_SLOPE || low < context.chunkGenerator().getSeaLevel()) {
			return Optional.empty();
		}
		BlockPos origin = new BlockPos(minX, Math.round(sum / 4.0F), minZ);
		YardTrees trees = YardTrees.forBiome(context.biomeResolver().getNoiseBiome(QuartPos.fromBlock(minX + width / 2), QuartPos.fromBlock(origin.getY()),
				QuartPos.fromBlock(minZ + depth / 2)));
		return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CryptYardPiece(seed, origin, width, depth, crypt, trees))));
	}

	private static int odd(int n) {
		return n % 2 == 0 ? n + 1 : n;
	}

	@Override
	public StructureType<?> type() {
		return Graveyards.CRYPT_YARD;
	}
}

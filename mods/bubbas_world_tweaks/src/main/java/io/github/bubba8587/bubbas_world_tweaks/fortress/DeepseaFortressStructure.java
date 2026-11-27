// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;

/** A flooded fortress on a deep-ocean floor, laid out by vanilla's Nether fortress generator. */
public class DeepseaFortressStructure extends Structure {
	public static final MapCodec<DeepseaFortressStructure> CODEC = simpleCodec(DeepseaFortressStructure::new);
	/** Half the start crossing's width: its centre from its corner. */
	private static final int START_HALF = 9;
	/** Shallowest floor allowed under the start crossing's centre, in blocks below sea level. */
	public static final int MIN_DEPTH = 22;
	/** Gap kept between the fortress top and sea level. */
	public static final int TOP_GAP = 6;

	public DeepseaFortressStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getBlockX(2);
		int z = chunk.getBlockZ(2);
		int sea = context.chunkGenerator().getSeaLevel();
		int floor = floor(context, x + START_HALF, z + START_HALF);
		if (floor > sea - MIN_DEPTH) {
			return Optional.empty();
		}
		StructurePiecesBuilder builder = new StructurePiecesBuilder();
		layOut(builder, context.random(), x, z, floor, sea);
		return Optional.of(new GenerationStub(new BlockPos(x + START_HALF, floor, z + START_HALF), Either.right(builder)));
	}

	private static int floor(GenerationContext context, int x, int z) {
		return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
				context.randomState());
	}

	/** Runs vanilla's fortress layout at (x, z), seated on the floor at floorY and kept below sea level; returns how far the start sank below the floor to stay under the sea. */
	public static int layOut(StructurePiecesBuilder builder, RandomSource random, int x, int z, int floorY, int seaLevel) {
		NetherFortressPieces.StartPiece start = new NetherFortressPieces.StartPiece(random, x, z);
		builder.addPiece(start);
		start.addChildren(start, builder, random);
		List<StructurePiece> pending = start.pendingChildren;
		while (!pending.isEmpty()) {
			pending.remove(random.nextInt(pending.size())).addChildren(start, builder, random);
		}
		int dy = floorY + 1 - start.getBoundingBox().minY();
		int over = Math.max(0, builder.getBoundingBox().maxY() + dy - (seaLevel - TOP_GAP));
		builder.offsetPiecesVertically(dy - over);
		return over;
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBB, ChunkPos chunkPos, PiecesContainer pieces) {
		ConduitShrine.place(level, chunkBB, pieces.pieces().getFirst().getBoundingBox());
		SandPiles.place(level, chunkBB, DeepseaFortresses.pillarFeet(), pieces.pieces());
	}

	@Override
	public StructureType<?> type() {
		return DeepseaFortresses.TYPE;
	}
}

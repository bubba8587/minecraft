// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
	/** Erosion tears per fortress (plus up to TEARS_EXTRA more), their length, slope from level in degrees, and radius (plus up to 1). */
	private static final int TEARS = 2;
	private static final int TEARS_EXTRA = 1;
	private static final int TEAR_LENGTH_MIN = 28;
	private static final int TEAR_LENGTH_MAX = 48;
	private static final int TEAR_SLOPE_MIN = 25;
	private static final int TEAR_SLOPE_MAX = 50;
	private static final int TEAR_RADIUS = 2;
	/** Pieces tried when centring a tear, and how far a piece's top must stand above the floor to count as exposed. */
	private static final int TEAR_PICKS = 12;
	private static final int TEAR_EXPOSED = 3;

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
		addTears(builder, context.random(), piece -> {
			BlockPos c = piece.getBoundingBox().getCenter();
			return floor(context, c.getX(), c.getZ()) < piece.getBoundingBox().maxY() - TEAR_EXPOSED;
		});
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

	/** A couple of slanted erosion tears through the laid-out fortress, each centred on one of its pieces, preferring pieces that stand out of the floor. */
	public static void addTears(StructurePiecesBuilder builder, RandomSource random, Predicate<StructurePiece> exposed) {
		List<StructurePiece> pieces = builder.build().pieces();
		int count = TEARS + random.nextInt(TEARS_EXTRA + 1);
		for (int i = 0; i < count; i++) {
			StructurePiece centre = pieces.get(random.nextInt(pieces.size()));
			for (int tries = 1; tries < TEAR_PICKS && !exposed.test(centre); tries++) {
				centre = pieces.get(random.nextInt(pieces.size()));
			}
			BlockPos mid = centre.getBoundingBox().getCenter();
			double heading = random.nextDouble() * Math.PI * 2.0;
			double slope = Math.toRadians(TEAR_SLOPE_MIN + random.nextInt(TEAR_SLOPE_MAX - TEAR_SLOPE_MIN + 1));
			double half = (TEAR_LENGTH_MIN + random.nextInt(TEAR_LENGTH_MAX - TEAR_LENGTH_MIN + 1)) / 2.0;
			int dx = (int) Math.round(Math.cos(slope) * Math.cos(heading) * half);
			int dy = (int) Math.round(Math.sin(slope) * half);
			int dz = (int) Math.round(Math.cos(slope) * Math.sin(heading) * half);
			int radius = TEAR_RADIUS + random.nextInt(2);
			builder.addPiece(new TearPiece(mid.offset(-dx, dy, -dz), mid.offset(dx, -dy, dz), radius));
		}
	}

	@Override
	public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBB, ChunkPos chunkPos, PiecesContainer pieces) {
		ConduitShrine.place(level, chunkBB, pieces.pieces().getFirst().getBoundingBox());
		SandPiles.place(level, chunkBB, DeepseaFortresses.pillarFeet(), pieces.pieces());
		CoralClumps.place(level, generator, random, chunkBB, pieces.pieces());
	}

	@Override
	public StructureType<?> type() {
		return DeepseaFortresses.TYPE;
	}
}

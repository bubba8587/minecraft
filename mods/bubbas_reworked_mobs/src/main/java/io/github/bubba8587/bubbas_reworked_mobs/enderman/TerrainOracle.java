// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

/**
 * Natural terrain as first generated, worked out again from the seed (specs/endermen.md, "The target world"). Never
 * stored in the world: a chunk's terrain is regenerated on demand off the main thread by running the world's own
 * noise generator (fill, surface rules, carvers: {@link NoiseBasedChunkGenerator#buildTerrain}) into a scratch chunk,
 * and kept in a small per-level cache. Features (trees, ores, flowers, lakes) and structures are not reproduced, which
 * is why the enderman rules only revert terrain materials and leave every structure's bounding box alone.
 *
 * <p>Superflat worlds use their layer column; other generators have no target (endermen leave terrain alone there).
 * Main thread only, except the work handed to {@link #WORKER}.
 */
public final class TerrainOracle {
	/** One regenerated chunk: what was there at generation, its top solid block per column, and structure boxes. */
	public static final class TerrainChunk {
		private final @Nullable ChunkAccess chunk;
		private final @Nullable NoiseColumn column;
		private final int[] surface;
		private final List<BoundingBox> structures;

		TerrainChunk(@Nullable ChunkAccess chunk, @Nullable NoiseColumn column, int[] surface, List<BoundingBox> structures) {
			this.chunk = chunk;
			this.column = column;
			this.surface = surface;
			this.structures = structures;
		}

		/** The generated block here (the position must be in this chunk). */
		public BlockState state(BlockPos pos) {
			if (column != null) {
				return column.getBlock(pos.getY());
			}
			return chunk.getBlockState(pos);
		}

		/** Y of the top solid (non-air, non-fluid) generated block in this column. */
		public int surface(int x, int z) {
			return surface[(x & 15) | (z & 15) << 4];
		}

		/** Whether a structure piece (village house, mineshaft, ruin...) covers this spot: endermen don't judge those. */
		public boolean inStructure(BlockPos pos) {
			for (BoundingBox box : structures) {
				if (box.isInside(pos)) {
					return true;
				}
			}
			return false;
		}
	}

	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "brm-target-world");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});
	private static final Map<ServerLevel, LinkedHashMap<Long, CompletableFuture<TerrainChunk>>> CACHE = new WeakHashMap<>();
	/** For the report and tests: chunks regenerated, and the total time spent on the worker. */
	public static final AtomicLong CHUNKS_BUILT = new AtomicLong();
	public static final AtomicLong NANOS_BUILDING = new AtomicLong();

	private TerrainOracle() {
	}

	/** Whether this level's generator can be reproduced at all. */
	public static boolean supports(ServerLevel level) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		return generator instanceof NoiseBasedChunkGenerator || generator instanceof FlatLevelSource;
	}

	/**
	 * The regenerated chunk if it is ready; otherwise starts regenerating it (when its real chunk is loaded) and
	 * returns null. Never blocks.
	 */
	public static @Nullable TerrainChunk get(ServerLevel level, int chunkX, int chunkZ) {
		LinkedHashMap<Long, CompletableFuture<TerrainChunk>> cache = CACHE.computeIfAbsent(level,
				l -> new LinkedHashMap<>(64, 0.75F, true) {
					@Override
					protected boolean removeEldestEntry(Map.Entry<Long, CompletableFuture<TerrainChunk>> eldest) {
						return size() > EndermanTuning.TERRAIN_CACHE_CHUNKS;
					}
				});
		long key = ChunkPos.pack(chunkX, chunkZ);
		CompletableFuture<TerrainChunk> future = cache.get(key);
		if (future == null) {
			if (!supports(level) || !level.hasChunk(chunkX, chunkZ)) {
				return null;
			}
			future = request(level, chunkX, chunkZ);
			cache.put(key, future);
		}
		return future.getNow(null);
	}

	/** The regenerated block at {@code pos}, or null while its chunk isn't ready. */
	public static @Nullable BlockState state(ServerLevel level, BlockPos pos) {
		TerrainChunk chunk = get(level, pos.getX() >> 4, pos.getZ() >> 4);
		return chunk == null ? null : chunk.state(pos);
	}

	/** Blocks until the chunk is regenerated (tests and commands only). */
	public static TerrainChunk getNow(ServerLevel level, int chunkX, int chunkZ) {
		get(level, chunkX, chunkZ);
		CompletableFuture<TerrainChunk> future = CACHE.get(level).get(ChunkPos.pack(chunkX, chunkZ));
		if (future == null) {
			future = request(level, chunkX, chunkZ);
		}
		return future.join();
	}

	public static void clear(ServerLevel level) {
		CACHE.remove(level);
	}

	private static CompletableFuture<TerrainChunk> request(ServerLevel level, int chunkX, int chunkZ) {
		// Structure pieces are read here, on the main thread, from the loaded chunk's references.
		List<StructureStart> starts = level.hasChunk(chunkX, chunkZ)
				? level.structureManager().startsForStructure(chunkX, chunkZ, s -> true) : List.of();
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		List<BoundingBox> boxes = new ArrayList<>();
		BoundingBox chunkBox = new BoundingBox(pos.getMinBlockX(), level.getMinY(), pos.getMinBlockZ(), pos.getMaxBlockX(), level.getMaxY(), pos.getMaxBlockZ());
		for (StructureStart start : starts) {
			for (StructurePiece piece : start.getPieces()) {
				BoundingBox box = piece.getBoundingBox().inflatedBy(EndermanTuning.STRUCTURE_MARGIN);
				if (box.intersects(chunkBox)) {
					boxes.add(box);
				}
			}
		}
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		long seed = level.getSeed();
		return CompletableFuture.supplyAsync(() -> {
			long started = System.nanoTime();
			try {
				TerrainChunk result;
				if (generator instanceof FlatLevelSource) {
					NoiseColumn column = generator.getBaseColumn(pos.getMinBlockX(), pos.getMinBlockZ(), level, randomState);
					int top = level.getMinY() - 1;
					for (int y = level.getMaxY(); y >= level.getMinY(); y--) {
						BlockState state = column.getBlock(y);
						if (!state.isAir() && state.getFluidState().isEmpty()) {
							top = y;
							break;
						}
					}
					int[] surface = new int[256];
					java.util.Arrays.fill(surface, top);
					result = new TerrainChunk(null, column, surface, boxes);
				} else {
					result = regenerate(level, generator, randomState, seed, pos, starts, boxes);
				}
				return result;
			} catch (RuntimeException e) {
				BubbasReworkedMobs.LOGGER.warn("Could not regenerate terrain for chunk {}", pos, e);
				throw e;
			} finally {
				CHUNKS_BUILT.incrementAndGet();
				NANOS_BUILDING.addAndGet(System.nanoTime() - started);
			}
		}, WORKER);
	}

	private static TerrainChunk regenerate(ServerLevel level, ChunkGenerator generator, RandomState randomState, long seed, ChunkPos pos,
			List<StructureStart> starts, List<BoundingBox> boxes) {
		ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
		// Only the beardifier reads structures here (terrain raised or cut around villages and the like).
		StructureManager structures = new KnownStarts(level, starts);
		generator.createBiomes(randomState, Blender.empty(), structures, chunk).join();
		BiomeManager biomes = new BiomeManager(generator.getBiomeSource().createUncachedResolver(randomState), BiomeManager.obfuscateSeed(seed));
		generator.buildTerrain(chunk, Blender.empty(), randomState, structures, biomes, null, null).join();
		int[] surface = new int[256];
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int top = level.getMinY() - 1;
				for (int y = level.getMaxY(); y >= level.getMinY(); y--) {
					BlockState state = chunk.getBlockState(at.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
					if (!state.isAir() && state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK)) {
						top = y;
						break;
					}
				}
				surface[x | z << 4] = top;
			}
		}
		return new TerrainChunk(chunk, null, surface, boxes);
	}

	/** A structure manager that answers only from structure starts gathered beforehand on the main thread. */
	private static final class KnownStarts extends StructureManager {
		private final List<StructureStart> starts;

		@SuppressWarnings("DataFlowIssue")
		KnownStarts(ServerLevel level, List<StructureStart> starts) {
			super(level, null, null);
			this.starts = starts;
		}

		@Override
		public List<StructureStart> startsForStructure(int sectionX, int sectionZ, Predicate<Structure> matcher) {
			return starts.stream().filter(s -> matcher.test(s.getStructure())).toList();
		}
	}
}

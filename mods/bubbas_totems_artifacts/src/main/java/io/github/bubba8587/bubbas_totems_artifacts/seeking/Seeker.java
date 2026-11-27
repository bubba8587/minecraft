// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Stepped searches for the nearest structure or biome of a kind, skipping spots already found (Totem of Seeking; specs/totems-artifacts.md). Main server thread only. */
public final class Seeker {
	private record Key(ResourceKey<Level> dimension, SeekKinds.Kind kind, BlockPos origin, List<BlockPos> skip) {
	}

	private interface Search {
		/** Works until {@code deadline} (System.nanoTime); true when finished. */
		boolean step(long deadline);

		@Nullable BlockPos best();
	}

	private static final Map<Key, Optional<BlockPos>> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Optional<BlockPos>> eldest) {
			return this.size() > TotemsTuning.SEEK_CACHE_SIZE;
		}
	};
	private static final Map<Key, Search> RUNNING = new LinkedHashMap<>();

	private Seeker() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> run(System.nanoTime() + TotemsTuning.SEEK_BUDGET_NANOS));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	/** Forgets every result and running search (server stopping, tests). */
	public static void clear() {
		CACHE.clear();
		RUNNING.clear();
		SeekKinds.clear();
	}

	/**
	 * The nearest {@code kind} to {@code origin} that is not near a {@code skip} spot: present = found, empty = none in
	 * reach, null = still searching (a search is started if none is running).
	 */
	public static @Nullable Optional<BlockPos> lookup(ServerLevel level, SeekKinds.Kind kind, BlockPos origin, List<BlockPos> skip) {
		Key key = new Key(level.dimension(), kind, origin.immutable(), List.copyOf(skip));
		Optional<BlockPos> cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		if (!RUNNING.containsKey(key)) {
			Search search = kind.biome()
					? SeekKinds.biome(level, kind).<Search>map(b -> new BiomeSearch(level, b, key.origin(), key.skip())).orElse(null)
					: SeekKinds.structure(level, kind).<Search>map(s -> new StructureSearch(level, s, key.origin(), key.skip())).orElse(null);
			if (search == null) {
				CACHE.put(key, Optional.empty());
				return Optional.empty();
			}
			RUNNING.put(key, search);
		}
		return null;
	}

	/** Runs searches until {@code deadline} (System.nanoTime). */
	public static void run(long deadline) {
		for (Iterator<Map.Entry<Key, Search>> it = RUNNING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Key, Search> entry = it.next();
			if (entry.getValue().step(deadline)) {
				CACHE.put(entry.getKey(), Optional.ofNullable(entry.getValue().best()));
				it.remove();
			}
			if (System.nanoTime() >= deadline) {
				return;
			}
		}
	}

	/** Searches to the end in one go (tests). */
	public static Optional<BlockPos> lookupNow(ServerLevel level, SeekKinds.Kind kind, BlockPos origin, List<BlockPos> skip) {
		Optional<BlockPos> found;
		while ((found = lookup(level, kind, origin, skip)) == null) {
			run(Long.MAX_VALUE);
		}
		return found;
	}

	/** Horizontal distance squared. */
	static double flatDistSqr(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/** Far enough from the origin, in reach, and not near a skip spot. */
	private static boolean allowed(BlockPos at, BlockPos origin, List<BlockPos> skip, int skipRadius) {
		double d = flatDistSqr(at, origin);
		if (d < (double) TotemsTuning.SEEK_MIN_DISTANCE * TotemsTuning.SEEK_MIN_DISTANCE || d > (double) TotemsTuning.SEEK_REACH * TotemsTuning.SEEK_REACH) {
			return false;
		}
		for (BlockPos s : skip) {
			if (flatDistSqr(at, s) < (double) skipRadius * skipRadius) {
				return false;
			}
		}
		return true;
	}

	/** Vanilla's /locate order for one structure (concentric rings, then the random-spread grid ring by ring), resumable, never loading chunks. */
	private static final class StructureSearch implements Search {
		private final ServerLevel level;
		private final BlockPos origin;
		private final List<BlockPos> skip;
		private final Holder<Structure> structure;
		private final long seed;
		private final List<ConcentricRingsStructurePlacement> rings = new ArrayList<>();
		private final List<RandomSpreadStructurePlacement> spreads = new ArrayList<>();
		private final boolean enabled;
		private boolean ringsDone;
		private int radius;
		private int placement;
		private int cell;
		private boolean hitThisRadius;
		private @Nullable BlockPos best;
		private double bestDistSqr = Double.MAX_VALUE;

		StructureSearch(ServerLevel level, Holder<Structure> structure, BlockPos origin, List<BlockPos> skip) {
			this.level = level;
			this.origin = origin;
			this.skip = skip;
			this.structure = structure;
			ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
			this.seed = state.getLevelSeed();
			for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
				if (placement instanceof ConcentricRingsStructurePlacement concentric) {
					this.rings.add(concentric);
				} else if (placement instanceof RandomSpreadStructurePlacement spread) {
					this.spreads.add(spread);
				}
			}
			this.enabled = level.getServer().getWorldGenSettings().options().generateStructures();
		}

		@Override
		public @Nullable BlockPos best() {
			return this.best;
		}

		@Override
		public boolean step(long deadline) {
			if (!this.enabled) {
				return true;
			}
			if (!this.ringsDone) {
				this.ringsDone = true;
				for (ConcentricRingsStructurePlacement placement : this.rings) {
					List<ChunkPos> positions = this.level.getChunkSource().getGeneratorState().getRingPositionsFor(placement);
					if (positions != null) {
						for (ChunkPos chunk : positions) {
							check(placement, chunk);
						}
					}
				}
				return this.spreads.isEmpty();
			}
			int originX = SectionPos.blockToSectionCoord(this.origin.getX());
			int originZ = SectionPos.blockToSectionCoord(this.origin.getZ());
			while (System.nanoTime() < deadline) {
				RandomSpreadStructurePlacement spread = this.spreads.get(this.placement);
				int cells = this.radius == 0 ? 1 : 8 * this.radius;
				if (this.cell < cells) {
					int[] d = ringCell(this.radius, this.cell++);
					ChunkPos target = spread.getPotentialStructureChunk(this.seed, originX + spread.spacing() * d[0], originZ + spread.spacing() * d[1]);
					if (check(spread, target)) {
						this.hitThisRadius = true;
					}
					continue;
				}
				this.cell = 0;
				if (++this.placement >= this.spreads.size()) {
					this.placement = 0;
					if (this.hitThisRadius) {
						return true;
					}
					this.radius++;
					if (beyondReach()) {
						return true;
					}
				}
			}
			return false;
		}

		/** Every placement's next ring starts past the search reach. */
		private boolean beyondReach() {
			for (RandomSpreadStructurePlacement spread : this.spreads) {
				if ((this.radius - 1) * spread.spacing() * 16 <= TotemsTuning.SEEK_REACH) {
					return false;
				}
			}
			return true;
		}

		/** Vanilla's quick presence check (no chunk loading); keeps the nearest allowed hit. */
		private boolean check(StructurePlacement placement, ChunkPos chunk) {
			BlockPos at = placement.getLocatePos(chunk);
			if (!allowed(at, this.origin, this.skip, TotemsTuning.SEEK_SKIP_STRUCTURE)
					|| this.level.structureManager().checkStructurePresence(chunk, this.structure.value(), placement, false) == StructureCheckResult.START_NOT_PRESENT) {
				return false;
			}
			double d = flatDistSqr(at, this.origin);
			if (d < this.bestDistSqr) {
				this.bestDistSqr = d;
				this.best = at;
			}
			return true;
		}
	}

	/**
	 * Vanilla's /locate biome order (a square spiral of columns, heights outward from the origin's), resumable, sampling
	 * the biome noise; a hit counts only at or below the ground (the generator's height estimate), never in the air. In
	 * land already generated the saved biomes win: the hit's column must hold the biome in the loaded chunk, or in the
	 * saved chunk read off-thread (the search waits for it).
	 */
	private static final class BiomeSearch implements Search {
		private final ServerLevel level;
		private final BlockPos origin;
		private final List<BlockPos> skip;
		private final Holder<Biome> biome;
		private final BiomeResolver resolver;
		private final Iterator<BlockPos.MutableBlockPos> spiral;
		private final int[] ys;
		private @Nullable BlockPos best;
		private @Nullable BlockPos candidate;
		private @Nullable CollectFields scanned;
		private @Nullable CompletableFuture<Void> scan;

		BiomeSearch(ServerLevel level, Holder<Biome> biome, BlockPos origin, List<BlockPos> skip) {
			this.level = level;
			this.origin = origin;
			this.skip = skip;
			this.biome = biome;
			this.resolver = level.getChunkSource().getGenerator().getBiomeSource().createCachingResolver(level.getChunkSource().randomState());
			this.spiral = BlockPos.spiralAround(BlockPos.ZERO, TotemsTuning.SEEK_REACH / TotemsTuning.SEEK_BIOME_STEP, Direction.EAST, Direction.SOUTH).iterator();
			this.ys = Mth.outFromOrigin(origin.getY(), level.getMinY() + 1, level.getMaxY() + 1, TotemsTuning.SEEK_BIOME_STEP_Y).toArray();
		}

		@Override
		public @Nullable BlockPos best() {
			return this.best;
		}

		@Override
		public boolean step(long deadline) {
			while (System.nanoTime() < deadline) {
				if (this.candidate != null) {
					Boolean saved = savedHas(this.candidate);
					if (saved == null) {
						return false;
					}
					if (saved) {
						this.best = this.candidate;
						return true;
					}
					this.candidate = null;
					continue;
				}
				if (!this.spiral.hasNext()) {
					return true;
				}
				BlockPos.MutableBlockPos column = this.spiral.next();
				int x = this.origin.getX() + column.getX() * TotemsTuning.SEEK_BIOME_STEP;
				int z = this.origin.getZ() + column.getZ() * TotemsTuning.SEEK_BIOME_STEP;
				if (!allowed(new BlockPos(x, this.origin.getY(), z), this.origin, this.skip, TotemsTuning.SEEK_SKIP_BIOME)) {
					continue;
				}
				for (int y : this.ys) {
					if (is(x, y, z)) {
						this.candidate = grounded(x, z);
						break;
					}
				}
			}
			return false;
		}

		/**
		 * Whether generated land at this spot holds the biome somewhere in its column: true/false, or null while the
		 * saved chunk is being read. Land not generated yet: true (the generator will make what the noise says).
		 */
		private @Nullable Boolean savedHas(BlockPos at) {
			int qx = QuartPos.fromBlock(at.getX());
			int qz = QuartPos.fromBlock(at.getZ());
			ChunkAccess loaded = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(at.getX()), SectionPos.blockToSectionCoord(at.getZ()));
			if (loaded != null) {
				for (int qy = QuartPos.fromBlock(this.level.getMinY()); qy <= QuartPos.fromBlock(this.level.getMaxY()); qy++) {
					if (loaded.getNoiseBiome(qx, qy, qz).is(this.biome)) {
						return true;
					}
				}
				return false;
			}
			if (this.scan == null) {
				this.scanned = new CollectFields(new FieldSelector(StringTag.TYPE, "Status"), new FieldSelector(ListTag.TYPE, "sections"));
				this.scan = this.level.getChunkSource().chunkScanner().scanChunk(new ChunkPos(at.getX() >> 4, at.getZ() >> 4), this.scanned);
				return null;
			}
			if (!this.scan.isDone()) {
				return null;
			}
			CollectFields fields = this.scanned;
			boolean failed = this.scan.isCompletedExceptionally();
			this.scan = null;
			this.scanned = null;
			if (failed || !(fields.getResult() instanceof CompoundTag chunk)) {
				return true;
			}
			ListTag sections = chunk.getListOrEmpty("sections");
			var codec = this.level.palettedContainerFactory().biomeContainerCodec();
			boolean any = false;
			for (int i = 0; i < sections.size(); i++) {
				var biomes = sections.getCompoundOrEmpty(i).getCompound("biomes").flatMap(tag -> codec.parse(NbtOps.INSTANCE, tag).result());
				if (biomes.isEmpty()) {
					continue;
				}
				any = true;
				for (int qy = 0; qy < 4; qy++) {
					if (biomes.get().get(qx & 3, qy, qz & 3).is(this.biome)) {
						return true;
					}
				}
			}
			return !any;
		}

		/** The biome sample at a block. */
		private boolean is(int x, int y, int z) {
			return this.resolver.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).is(this.biome);
		}

		/** The highest spot of the biome in this column at or below the ground, if any. */
		private @Nullable BlockPos grounded(int x, int z) {
			int ground = this.level.getChunkSource().getGenerator().getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
					this.level, this.level.getChunkSource().randomState());
			for (int y = ground; y > this.level.getMinY(); y -= TotemsTuning.SEEK_BIOME_STEP_GROUND) {
				if (is(x, y, z)) {
					return new BlockPos(x, y, z);
				}
			}
			return null;
		}
	}

	/** The {@code index}-th of the 8r cells on the edge of the square ring of radius {@code r} (r = 0: the centre). */
	public static int[] ringCell(int r, int index) {
		if (r == 0) {
			return new int[] {0, 0};
		}
		int side = 2 * r;
		int edge = index / side;
		int along = index % side;
		return switch (edge) {
			case 0 -> new int[] {-r + along, -r};
			case 1 -> new int[] {r, -r + along};
			case 2 -> new int[] {r - along, r};
			default -> new int[] {-r, r - along};
		};
	}
}

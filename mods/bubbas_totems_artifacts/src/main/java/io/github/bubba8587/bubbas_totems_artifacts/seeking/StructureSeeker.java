// dte:C6,B17
package io.github.bubba8587.bubbas_totems_artifacts.seeking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Finds the nearest structure of a type without freezing the server (Totem of Seeking; specs/totems-artifacts.md).
 *
 * <p>The search is vanilla's own (ChunkGenerator.findNearestMapStructure, what /locate and explorer maps use: the
 * nearest of a concentric-rings placement such as strongholds, then the random-spread grid ring by ring out to
 * {@link TotemsTuning#SEEK_MAX_RINGS}, stopping at the first ring with a hit), with two changes so it can't stall a
 * tick:
 * <ul>
 * <li>it is cut into steps: every server tick, all running searches together get {@link TotemsTuning#SEEK_BUDGET_NANOS}
 * and pick up where they stopped;</li>
 * <li>it never loads or generates a chunk: a candidate counts when vanilla's quick structure check says a start is there
 * or would be generated there (biome and placement checks, which vanilla then confirms by generating the chunk's
 * structure starts). In rare cases a structure that fails to fit still counts.</li>
 * </ul>
 * Results are cached per level, type and {@link TotemsTuning#SEEK_CELL_BLOCKS}-block cell of the asker's position, so
 * every holder in a cell shares one search. Main server thread only.
 */
public final class StructureSeeker {
	private record Key(ResourceKey<Level> dimension, Identifier type, int cellX, int cellZ) {
	}

	private static final Map<Key, Optional<BlockPos>> CACHE = new LinkedHashMap<>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Optional<BlockPos>> eldest) {
			return this.size() > TotemsTuning.SEEK_CACHE_SIZE;
		}
	};
	private static final Map<Key, Search> RUNNING = new LinkedHashMap<>();

	private StructureSeeker() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> run(System.nanoTime() + TotemsTuning.SEEK_BUDGET_NANOS));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	/** Forgets every result and running search (server stopping, tests). */
	public static void clear() {
		CACHE.clear();
		RUNNING.clear();
	}

	/**
	 * The nearest structure of {@code type} to {@code origin}: present = found (its locate position, like /locate's),
	 * empty = none within reach in this level, null = still searching (a search is started if none is running).
	 */
	public static @Nullable Optional<BlockPos> lookup(ServerLevel level, SeekingTypes.SeekType type, BlockPos origin) {
		int shift = Integer.numberOfTrailingZeros(TotemsTuning.SEEK_CELL_BLOCKS);
		Key key = new Key(level.dimension(), type.id(), origin.getX() >> shift, origin.getZ() >> shift);
		Optional<BlockPos> cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		if (!RUNNING.containsKey(key)) {
			RUNNING.put(key, new Search(level, type.resolve(level.registryAccess()), origin));
		}
		return null;
	}

	/** Runs searches until {@code deadline} (System.nanoTime). */
	public static void run(long deadline) {
		for (Iterator<Map.Entry<Key, Search>> it = RUNNING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Key, Search> entry = it.next();
			if (entry.getValue().step(deadline)) {
				CACHE.put(entry.getKey(), Optional.ofNullable(entry.getValue().best));
				it.remove();
			}
			if (System.nanoTime() >= deadline) {
				return;
			}
		}
	}

	/** Searches to the end in one go (tests). */
	public static Optional<BlockPos> lookupNow(ServerLevel level, SeekingTypes.SeekType type, BlockPos origin) {
		Optional<BlockPos> found;
		while ((found = lookup(level, type, origin)) == null) {
			run(Long.MAX_VALUE);
		}
		return found;
	}

	/** One search, vanilla's order, resumable. */
	private static final class Search {
		private final ServerLevel level;
		private final BlockPos origin;
		private final StructureManager structures;
		private final long seed;
		private final List<Map.Entry<ConcentricRingsStructurePlacement, Set<Holder<Structure>>>> rings = new ArrayList<>();
		private final List<Map.Entry<RandomSpreadStructurePlacement, Set<Holder<Structure>>>> spreads = new ArrayList<>();
		private final boolean enabled;
		private boolean ringsDone;
		private int radius;
		private int placement;
		private int cell;
		private boolean hitThisRadius;
		private @Nullable BlockPos best;
		private double bestDistSqr = Double.MAX_VALUE;

		Search(ServerLevel level, HolderSet<Structure> wanted, BlockPos origin) {
			this.level = level;
			this.origin = origin.immutable();
			this.structures = level.structureManager();
			ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
			this.seed = state.getLevelSeed();
			Map<StructurePlacement, Set<Holder<Structure>>> byPlacement = new Object2ObjectArrayMap<>();
			for (Holder<Structure> structure : wanted) {
				for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
					byPlacement.computeIfAbsent(placement, p -> new ObjectArraySet<>()).add(structure);
				}
			}
			for (Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : byPlacement.entrySet()) {
				if (entry.getKey() instanceof ConcentricRingsStructurePlacement concentric) {
					this.rings.add(Map.entry(concentric, entry.getValue()));
				} else if (entry.getKey() instanceof RandomSpreadStructurePlacement spread) {
					this.spreads.add(Map.entry(spread, entry.getValue()));
				}
			}
			this.enabled = level.getServer().getWorldGenSettings().options().generateStructures();
		}

		/** Works until {@code deadline}; true when finished ({@link #best} is the answer). */
		boolean step(long deadline) {
			if (!this.enabled) {
				return true;
			}
			if (!this.ringsDone) {
				this.ringsDone = true;
				for (Map.Entry<ConcentricRingsStructurePlacement, Set<Holder<Structure>>> entry : this.rings) {
					List<ChunkPos> positions = this.level.getChunkSource().getGeneratorState().getRingPositionsFor(entry.getKey());
					if (positions == null) {
						continue;
					}
					for (ChunkPos chunk : positions) {
						BlockPos at = new BlockPos(SectionPos.sectionToBlockCoord(chunk.x(), 8), 32, SectionPos.sectionToBlockCoord(chunk.z(), 8));
						if (at.distSqr(this.origin) < this.bestDistSqr) {
							check(entry.getKey(), entry.getValue(), chunk);
						}
					}
				}
				return this.spreads.isEmpty();
			}
			int originX = SectionPos.blockToSectionCoord(this.origin.getX());
			int originZ = SectionPos.blockToSectionCoord(this.origin.getZ());
			while (System.nanoTime() < deadline) {
				if (this.radius > TotemsTuning.SEEK_MAX_RINGS) {
					return true;
				}
				Map.Entry<RandomSpreadStructurePlacement, Set<Holder<Structure>>> entry = this.spreads.get(this.placement);
				int cells = this.radius == 0 ? 1 : 8 * this.radius;
				if (this.cell < cells) {
					int[] d = ringCell(this.radius, this.cell++);
					RandomSpreadStructurePlacement spread = entry.getKey();
					ChunkPos target = spread.getPotentialStructureChunk(this.seed, originX + spread.spacing() * d[0], originZ + spread.spacing() * d[1]);
					if (check(spread, entry.getValue(), target)) {
						this.hitThisRadius = true;
					}
					continue;
				}
				// This placement's ring is done; next placement, or the next ring (vanilla stops at the first ring with a hit).
				this.cell = 0;
				if (++this.placement >= this.spreads.size()) {
					this.placement = 0;
					if (this.hitThisRadius) {
						return true;
					}
					this.radius++;
				}
			}
			return false;
		}

		/** Vanilla's quick presence check (no chunk loading); keeps the nearest hit. */
		private boolean check(StructurePlacement placement, Set<Holder<Structure>> wanted, ChunkPos chunk) {
			for (Holder<Structure> structure : wanted) {
				if (this.structures.checkStructurePresence(chunk, structure.value(), placement, false) != StructureCheckResult.START_NOT_PRESENT) {
					BlockPos at = placement.getLocatePos(chunk);
					double d = at.distSqr(this.origin);
					if (d < this.bestDistSqr) {
						this.bestDistSqr = d;
						this.best = at;
					}
					return true;
				}
			}
			return false;
		}
	}

	/** The {@code index}-th of the 8r cells on the edge of the square ring of radius {@code r} (r = 0: the centre). */
	static int[] ringCell(int r, int index) {
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

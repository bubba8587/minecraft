// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.jspecify.annotations.Nullable;

/**
 * Where cairns are (specs/endermen.md, E2): fixed from the seed like structure placement. The overworld is cut into
 * square regions of {@link EndermanTuning#SITE_REGION_CHUNKS} chunks; each region has one candidate spot (kept
 * {@link EndermanTuning#SITE_SEPARATION_CHUNKS} chunks from its edges, so sites are never closer than twice that), and
 * the candidate is a site when the ground there is dry and fairly flat. Never stored: worked out on demand, off the
 * main thread, and cached per region. Works for any generator that answers base heights (noise and superflat).
 */
public final class CairnSites {
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "brm-cairn-sites");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});
	private static final Map<ServerLevel, Map<Long, CompletableFuture<Optional<CairnSite>>>> REGIONS = new WeakHashMap<>();
	/** Sites placed by hand (tests, the build command in a world without natural sites), by level. */
	private static final Map<ServerLevel, List<CairnSite>> EXTRA = new WeakHashMap<>();

	/** A site placed with the {@code cairn here} command: saved with the level so it survives a reload (test world). */
	private record SavedSite(BlockPos origin, long seed) {
		static final Codec<SavedSite> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("origin").forGetter(SavedSite::origin),
				Codec.LONG.fieldOf("seed").forGetter(SavedSite::seed)).apply(i, SavedSite::new));
	}

	/** Hand-placed sites of a level, saved on the level itself (Fabric data attachment). */
	public static final AttachmentType<List<SavedSite>> SAVED_SITES = AttachmentRegistry.createPersistent(
			BubbasReworkedMobs.id("cairn_sites"), SavedSite.CODEC.listOf());

	private CairnSites() {
	}

	/** Whether this level has natural cairn sites: the overworld, outside the game test server. */
	public static boolean naturalSites(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && !(level.getServer() instanceof GameTestServer);
	}

	/** The site in the region holding this block column, if worked out already; null while pending or when none. */
	public static @Nullable CairnSite regionSite(ServerLevel level, int blockX, int blockZ) {
		if (!naturalSites(level)) {
			return null;
		}
		int size = EndermanTuning.SITE_REGION_CHUNKS;
		int rx = Math.floorDiv(blockX >> 4, size);
		int rz = Math.floorDiv(blockZ >> 4, size);
		CompletableFuture<Optional<CairnSite>> future = REGIONS.computeIfAbsent(level, l -> new HashMap<>())
				.computeIfAbsent(ChunkPos.pack(rx, rz), k -> CompletableFuture.supplyAsync(() -> compute(level, rx, rz), WORKER));
		Optional<CairnSite> site = future.getNow(null);
		return site == null ? null : site.orElse(null);
	}

	/**
	 * The site whose design covers {@code pos}, or null. Where boxes overlap (hand-placed cairns close together), the
	 * one with a design block at {@code pos} wins, so the right cairn says what belongs there.
	 */
	public static @Nullable CairnSite covering(ServerLevel level, BlockPos pos) {
		CairnSite boxOnly = null;
		List<CairnSite> extra = extra(level);
		if (extra != null) {
			for (CairnSite site : extra) {
				if (site.box().isInside(pos)) {
					if (site.at(pos) != null) {
						return site;
					}
					if (boxOnly == null) {
						boxOnly = site;
					}
				}
			}
		}
		CairnSite site = regionSite(level, pos.getX(), pos.getZ());
		if (site != null && site.box().isInside(pos) && (site.at(pos) != null || boxOnly == null)) {
			return site;
		}
		return boxOnly;
	}

	/** Sites (worked out already) whose origin is within {@code radius} blocks (horizontally) of {@code pos}. */
	/** Sites within {@code radius} of any player (spectators only when {@code spectators}). */
	public static java.util.Set<CairnSite> nearPlayers(ServerLevel level, int radius, boolean spectators) {
		java.util.Set<CairnSite> out = new java.util.LinkedHashSet<>();
		for (net.minecraft.server.level.ServerPlayer player : level.players()) {
			if (spectators || !player.isSpectator()) {
				out.addAll(near(level, player.blockPosition(), radius));
			}
		}
		return out;
	}

	public static List<CairnSite> near(ServerLevel level, BlockPos pos, int radius) {
		List<CairnSite> found = new ArrayList<>();
		List<CairnSite> extra = extra(level);
		if (extra != null) {
			for (CairnSite site : extra) {
				if (horizontalDistSqr(site.origin(), pos) <= (double) radius * radius) {
					found.add(site);
				}
			}
		}
		if (!naturalSites(level)) {
			return found;
		}
		int step = EndermanTuning.SITE_REGION_CHUNKS * 16;
		for (int x = Math.floorDiv(pos.getX() - radius, step); x <= Math.floorDiv(pos.getX() + radius, step); x++) {
			for (int z = Math.floorDiv(pos.getZ() - radius, step); z <= Math.floorDiv(pos.getZ() + radius, step); z++) {
				CairnSite site = regionSite(level, x * step, z * step);
				if (site != null && horizontalDistSqr(site.origin(), pos) <= (double) radius * radius) {
					found.add(site);
				}
			}
		}
		return found;
	}

	/** The nearest worked-out site within {@code radius}, or null. */
	public static @Nullable CairnSite nearest(ServerLevel level, BlockPos pos, int radius) {
		CairnSite best = null;
		for (CairnSite site : near(level, pos, radius)) {
			if (best == null || horizontalDistSqr(site.origin(), pos) < horizontalDistSqr(best.origin(), pos)) {
				best = site;
			}
		}
		return best;
	}

	/** Blocks until every region within {@code radius} is worked out, then {@link #nearest} (commands and tests). */
	public static @Nullable CairnSite nearestNow(ServerLevel level, BlockPos pos, int radius) {
		if (naturalSites(level)) {
			int step = EndermanTuning.SITE_REGION_CHUNKS * 16;
			for (int x = Math.floorDiv(pos.getX() - radius, step); x <= Math.floorDiv(pos.getX() + radius, step); x++) {
				for (int z = Math.floorDiv(pos.getZ() - radius, step); z <= Math.floorDiv(pos.getZ() + radius, step); z++) {
					regionSite(level, x * step, z * step);
					REGIONS.get(level).get(ChunkPos.pack(x, z)).join();
				}
			}
		}
		return nearest(level, pos, radius);
	}

	/** Adds a site by hand at {@code origin} (on flat ground), for tests; forgotten when the server stops. */
	public static CairnSite addSite(ServerLevel level, BlockPos origin, long seed) {
		CairnSite site = design(level, origin, seed);
		extra(level).add(site);
		return site;
	}

	/** Adds a site by hand and saves it with the level (the {@code cairn here} command, the test world). */
	public static CairnSite addSavedSite(ServerLevel level, BlockPos origin, long seed) {
		CairnSite site = addSite(level, origin, seed);
		List<SavedSite> saved = new ArrayList<>(level.getAttachedOrElse(SAVED_SITES, List.of()));
		saved.add(new SavedSite(origin.immutable(), seed));
		level.setAttached(SAVED_SITES, List.copyOf(saved));
		return site;
	}

	/** This level's hand-placed sites, loading the saved ones the first time it's asked. */
	private static List<CairnSite> extra(ServerLevel level) {
		synchronized (EXTRA) {
			return EXTRA.computeIfAbsent(level, l -> {
				List<CairnSite> sites = new java.util.concurrent.CopyOnWriteArrayList<>();
				for (SavedSite saved : l.getAttachedOrElse(SAVED_SITES, List.of())) {
					sites.add(design(l, saved.origin(), saved.seed()));
				}
				return sites;
			});
		}
	}

	private static CairnSite design(ServerLevel level, BlockPos origin, long seed) {
		return design(level, origin, seed, level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), (x, z) -> -1);
	}

	public static void removeSite(ServerLevel level, CairnSite site) {
		extra(level).remove(site);
	}

	static double horizontalDistSqr(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/**
	 * The site of region (rx, rz), worked out from the seed alone: the same answer on any thread, on any run. Used
	 * by the worldgen footing feature too.
	 */
	public static Optional<CairnSite> compute(ServerLevel level, int rx, int rz) {
		try {
			long[] candidate = candidate(level.getSeed(), rx, rz);
			int x = (int) candidate[0];
			int z = (int) candidate[1];
			long seed = candidate[2];
			ChunkGenerator generator = level.getChunkSource().getGenerator();
			RandomState randomState = level.getChunkSource().randomState();
			int ground = groundY(level, generator, randomState, x, z);
			if (ground == Integer.MIN_VALUE || ground + 1 <= generator.getSeaLevel()) {
				return Optional.empty();
			}
			for (int i = 0; i < 8; i++) {
				double a = Math.PI / 4.0 * i;
				int gx = x + (int) Math.round(Math.cos(a) * EndermanTuning.SITE_FLAT_RADIUS);
				int gz = z + (int) Math.round(Math.sin(a) * EndermanTuning.SITE_FLAT_RADIUS);
				int g = groundY(level, generator, randomState, gx, gz);
				if (g == Integer.MIN_VALUE || Math.abs(g - ground) > EndermanTuning.SITE_FLAT_TOLERANCE) {
					return Optional.empty();
				}
			}
			BlockPos origin = new BlockPos(x, ground + 1, z);
			Int2IntOpenHashMap cache = new Int2IntOpenHashMap();
			return Optional.of(design(level, origin, seed, generator, randomState, (dx, dz) -> cache.computeIfAbsent((dx & 0xFFFF) << 16 | (dz & 0xFFFF), k -> {
				int g = groundY(level, generator, randomState, x + dx, z + dz);
				return g == Integer.MIN_VALUE ? -100 : g - ground - 1;
			})));
		} catch (RuntimeException e) {
			BubbasReworkedMobs.LOGGER.warn("Could not work out the cairn site of region {}, {}", rx, rz, e);
			return Optional.empty();
		}
	}

	/**
	 * Region (rx, rz)'s candidate spot, from the world seed alone and cheap (no terrain): block x, block z, and the
	 * site's own seed. Whether it becomes a site depends on the ground ({@link #compute}).
	 */
	public static long[] candidate(long worldSeed, int rx, int rz) {
		int size = EndermanTuning.SITE_REGION_CHUNKS;
		int sep = EndermanTuning.SITE_SEPARATION_CHUNKS;
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(worldSeed, rx, rz, EndermanTuning.SITE_SALT);
		int cx = rx * size + sep + random.nextInt(size - 2 * sep);
		int cz = rz * size + sep + random.nextInt(size - 2 * sep);
		int x = cx * 16 + 4 + random.nextInt(8);
		int z = cz * 16 + 4 + random.nextInt(8);
		return new long[] { x, z, random.nextLong() };
	}

	/** Y of the top ground block from the noise column (no features), or MIN_VALUE when water lies on it. */
	private static int groundY(ServerLevel level, ChunkGenerator generator, RandomState randomState, int x, int z) {
		int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
		int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
		return floor == surface ? floor - 1 : Integer.MIN_VALUE;
	}

	private static CairnSite design(ServerLevel level, BlockPos origin, long seed, ChunkGenerator generator, RandomState randomState,
			java.util.function.IntBinaryOperator groundAt) {
		Holder<Biome> biome = generator.getBiomeSource().createUncachedResolver(randomState)
				.getNoiseBiome(QuartPos.fromBlock(origin.getX()), QuartPos.fromBlock(origin.getY()), QuartPos.fromBlock(origin.getZ()));
		Direction pointer = strongholdDirection(level, origin, seed);
		return new CairnSite(origin, seed, pointer, CairnDesign.design(seed, offerings(biome), pointer, groundAt));
	}

	static CairnDesign.Offerings offerings(Holder<Biome> biome) {
		if (biome.is(Biomes.MUSHROOM_FIELDS)) {
			return CairnDesign.Offerings.MUSHROOM;
		}
		if (biome.is(BiomeTags.IS_BADLANDS)) {
			return CairnDesign.Offerings.BADLANDS;
		}
		if (biome.is(Biomes.DESERT) || biome.is(Biomes.BEACH)) {
			return CairnDesign.Offerings.SANDY;
		}
		if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
			return CairnDesign.Offerings.SWAMP;
		}
		if (biome.is(BiomeTags.IS_JUNGLE)) {
			return CairnDesign.Offerings.JUNGLE;
		}
		if (biome.value().getBaseTemperature() < 0.15F) {
			return CairnDesign.Offerings.SNOWY;
		}
		if (biome.is(Biomes.DARK_FOREST) || biome.is(Biomes.PALE_GARDEN) || biome.is(BiomeTags.IS_TAIGA)) {
			return CairnDesign.Offerings.DARK;
		}
		if (biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.MEADOW) || biome.is(Biomes.CHERRY_GROVE) || biome.is(Biomes.SUNFLOWER_PLAINS)) {
			return CairnDesign.Offerings.FLOWERY;
		}
		return CairnDesign.Offerings.TEMPERATE;
	}

	/** Toward the nearest stronghold (from the seed's ring positions); a seed-picked direction when there are none. */
	private static Direction strongholdDirection(ServerLevel level, BlockPos origin, long seed) {
		Optional<Holder.Reference<StructureSet>> set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).get(BuiltinStructureSets.STRONGHOLDS);
		if (set.isPresent() && set.get().value().placement() instanceof ConcentricRingsStructurePlacement rings) {
			List<ChunkPos> positions = level.getChunkSource().getGeneratorState().getRingPositionsFor(rings);
			if (positions != null && !positions.isEmpty()) {
				ChunkPos best = null;
				double bestDist = Double.MAX_VALUE;
				for (ChunkPos p : positions) {
					double dx = p.getMiddleBlockX() - origin.getX();
					double dz = p.getMiddleBlockZ() - origin.getZ();
					if (dx * dx + dz * dz < bestDist) {
						bestDist = dx * dx + dz * dz;
						best = p;
					}
				}
				int dx = best.getMiddleBlockX() - origin.getX();
				int dz = best.getMiddleBlockZ() - origin.getZ();
				return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
			}
		}
		return Direction.from2DDataValue((int) Math.floorMod(seed, 4L));
	}
}

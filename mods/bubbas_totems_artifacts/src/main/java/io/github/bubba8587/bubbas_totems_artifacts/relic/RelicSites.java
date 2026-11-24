// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.jspecify.annotations.Nullable;

/**
 * Where each named relic vault is: one site per (name, size), worked out from the world seed and its overworld biomes,
 * the same every time (specs/totems-artifacts.md, "Relic maps").
 */
public final class RelicSites {
	/** One named vault's chunk. */
	public record Site(String name, int size, ChunkPos chunk) {
		/** The middle of the site's chunk (where the vault's door is), at y 0. */
		public BlockPos centre() {
			return new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
		}
	}

	/** Which overworld spots a vault may sit under. */
	public interface Land {
		/** Every spot (seed-only layouts, for tests). */
		Land ANY = (x, z) -> true;

		boolean allows(int blockX, int blockZ);

		/** Not under an ocean: the biome at sea level there isn't an ocean. */
		static Land notOcean(BiomeSource source, RandomState randomState) {
			BiomeResolver resolver = source.createUncachedResolver(randomState);
			int qy = QuartPos.fromBlock(63);
			return (x, z) -> !resolver.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z)).is(BiomeTags.IS_OCEAN);
		}

		/** The land rule of this server's overworld. */
		static Land of(ServerLevel overworld) {
			return notOcean(overworld.getChunkSource().getGenerator().getBiomeSource(), overworld.getChunkSource().randomState());
		}
	}

	private static final long SALT = 0x5E1C_4A9B_2026_1124L;
	private static final int ATTEMPTS = 48;

	private record Layout(long seed, boolean landAware, List<Site> sites, Map<Long, Site> byChunk, Map<String, Site> byKey) {
	}

	private static volatile @Nullable Layout cached;

	private RelicSites() {
	}

	public static List<Site> sites(long seed, Land land) {
		return layout(seed, land).sites();
	}

	/** The site in this chunk, or null. */
	public static @Nullable Site at(long seed, Land land, int chunkX, int chunkZ) {
		return layout(seed, land).byChunk().get(ChunkPos.pack(chunkX, chunkZ));
	}

	public static @Nullable Site site(long seed, Land land, String name, int size) {
		return layout(seed, land).byKey().get(key(name, size));
	}

	private static String key(String name, int size) {
		return name + "/" + size;
	}

	private static Layout layout(long seed, Land land) {
		boolean landAware = land != Land.ANY;
		Layout layout = cached;
		if (layout == null || layout.seed() != seed || layout.landAware() != landAware) {
			layout = compute(seed, land);
			cached = layout;
		}
		return layout;
	}

	private static Layout compute(long seed, Land land) {
		RandomSource random = new XoroshiroRandomSource(seed ^ SALT);
		List<Site> sites = new ArrayList<>();
		Map<Long, Site> byChunk = new HashMap<>();
		Map<String, Site> byKey = new HashMap<>();
		int minSqr = RelicTuning.SITE_SEPARATION_CHUNKS * RelicTuning.SITE_SEPARATION_CHUNKS;
		for (int size : RelicNames.SIZES) {
			int min = size == 9 ? RelicTuning.GRAND_MIN_DISTANCE : RelicTuning.SMALL_MIN_DISTANCE;
			int max = size == 9 ? RelicTuning.GRAND_MAX_DISTANCE : RelicTuning.SMALL_MAX_DISTANCE;
			for (String name : RelicNames.ALL) {
				ChunkPos fallback = null;
				ChunkPos onLand = null;
				for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
					double angle = random.nextDouble() * Math.PI * 2.0;
					double distance = min + random.nextDouble() * (max - min);
					ChunkPos candidate = new ChunkPos(Math.floorDiv((int) Math.round(Math.cos(angle) * distance), 16),
							Math.floorDiv((int) Math.round(Math.sin(angle) * distance), 16));
					if (byChunk.containsKey(candidate.pack())) {
						continue;
					}
					fallback = candidate;
					if (!land.allows(candidate.getMiddleBlockX(), candidate.getMiddleBlockZ())) {
						continue;
					}
					onLand = candidate;
					boolean clear = true;
					for (Site other : sites) {
						int dx = other.chunk().x() - candidate.x();
						int dz = other.chunk().z() - candidate.z();
						if (dx * dx + dz * dz < minSqr) {
							clear = false;
							break;
						}
					}
					if (clear) {
						break;
					}
				}
				ChunkPos chunk = onLand != null ? onLand : fallback;
				Site site = new Site(name, size, chunk);
				sites.add(site);
				byChunk.put(chunk.pack(), site);
				byKey.put(key(name, size), site);
			}
		}
		return new Layout(seed, land != Land.ANY, List.copyOf(sites), Map.copyOf(byChunk), Map.copyOf(byKey));
	}
}

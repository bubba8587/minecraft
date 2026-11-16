// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.jspecify.annotations.Nullable;

/**
 * Where Ancient Fabricators are (owner, 2026-11-16: one beside every ancient city). Worked out from the seed alone,
 * never stored: each ancient-city grid cell (vanilla's own placement, read from the {@code ancient_cities} structure
 * set, so datapacks that move cities move Fabricators too) has a candidate city chunk; where the city's biome test
 * passes there (the biome at the chunk's corner at y -27, the city's start height: the same test vanilla makes, give
 * or take the centre piece's offset), the Fabricator stands {@link #CITY_DISTANCE} blocks from it in a seeded
 * direction, deck at y {@link #DECK_Y}, turned so its tunnel faces the city. Hand-placed ones (the op command) are
 * saved with the level, and so is each site's state (prepared, Warden spawned, heart placed, broken for good).
 */
public final class FabricatorSites {
	/** Horizontal distance from the city's centre: clear of its pieces (they reach 116) with the cavern's radius. */
	public static final int CITY_DISTANCE = 150;
	/** First air above the deck (the city floors are at about -51). */
	public static final int DECK_Y = -50;
	/** The city's floor height, for the tunnel's end. */
	public static final int CITY_FLOOR_Y = -51;
	private static final long SALT = 0x4642_5249_4341_544FL;

	/** A site placed by the op command: saved with the level. */
	private record Saved(BlockPos origin, int turns, long seed, String mob) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("origin").forGetter(Saved::origin),
				Codec.INT.fieldOf("turns").forGetter(Saved::turns),
				Codec.LONG.fieldOf("seed").forGetter(Saved::seed),
				Codec.STRING.fieldOf("mob").forGetter(Saved::mob)).apply(i, Saved::new));
	}

	/** A site's state: bit flags (see {@link #PREPARED} ...). */
	private record State(BlockPos origin, int flags) {
		static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("origin").forGetter(State::origin),
				Codec.INT.fieldOf("flags").forGetter(State::flags)).apply(i, State::new));
	}

	public static final AttachmentType<List<Saved>> SAVED = AttachmentRegistry.createPersistent(
			BubbasReworkedMobs.id("fabricator_sites"), Saved.CODEC.listOf());
	public static final AttachmentType<List<State>> STATES = AttachmentRegistry.createPersistent(
			BubbasReworkedMobs.id("fabricator_states"), State.CODEC.listOf());

	/** The first time a player came near: shriekers fixed, pad cleared, jukebox loaded. */
	public static final int PREPARED = 1;
	/** Its Warden has been spawned (never again). */
	public static final int WARDEN_SPAWNED = 2;
	/** The Fabricator Heart went into its floor beacon (Reworked Beacons). */
	public static final int HEART_PLACED = 4;
	/** Its hidden spawner was broken: stopped for good. */
	public static final int BROKEN = 8;

	private static final Map<ServerLevel, Map<Long, Optional<FabricatorSite>>> REGIONS = new WeakHashMap<>();
	private static final Map<ServerLevel, List<FabricatorSite>> EXTRA = new WeakHashMap<>();

	private FabricatorSites() {
	}

	/** Natural sites: the overworld with structures on, outside the game test server. */
	public static boolean naturalSites(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && !(level.getServer() instanceof GameTestServer)
				&& level.getServer().getWorldGenSettings().options().generateStructures();
	}

	private static @Nullable RandomSpreadStructurePlacement cityPlacement(ServerLevel level) {
		Optional<Holder.Reference<StructureSet>> set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).get(BuiltinStructureSets.ANCIENT_CITIES);
		return set.isPresent() && set.get().value().placement() instanceof RandomSpreadStructurePlacement p ? p : null;
	}

	/** The natural site of ancient-city grid cell (gx, gz), or null. Cheap (one biome lookup); cached. */
	public static @Nullable FabricatorSite cellSite(ServerLevel level, int gx, int gz) {
		Map<Long, Optional<FabricatorSite>> map;
		synchronized (REGIONS) {
			map = REGIONS.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
		}
		return map.computeIfAbsent(ChunkPos.pack(gx, gz), k -> Optional.ofNullable(compute(level, gx, gz))).orElse(null);
	}

	private static @Nullable FabricatorSite compute(ServerLevel level, int gx, int gz) {
		RandomSpreadStructurePlacement placement = cityPlacement(level);
		if (placement == null) {
			return null;
		}
		try {
			ChunkPos city = placement.getPotentialStructureChunk(level.getSeed(), gx * placement.spacing(), gz * placement.spacing());
			int cx = city.getMinBlockX();
			int cz = city.getMinBlockZ();
			Optional<Holder.Reference<Structure>> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(BuiltinStructures.ANCIENT_CITY);
			if (structure.isEmpty()) {
				return null;
			}
			Holder<Biome> biome = level.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(level.getChunkSource().randomState())
					.getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(-27), QuartPos.fromBlock(cz));
			if (!structure.get().value().biomes().contains(biome)) {
				return null;
			}
			RandomSource random = RandomSource.create(level.getSeed() ^ SALT ^ city.pack() * 0x9E3779B97F4A7C15L);
			long seed = random.nextLong();
			double angle = random.nextDouble() * Math.PI * 2.0;
			BlockPos origin = new BlockPos(cx + (int) Math.round(Math.cos(angle) * CITY_DISTANCE), DECK_Y, cz + (int) Math.round(Math.sin(angle) * CITY_DISTANCE));
			FactoryDesign.Mob mob = FactoryDesign.Mob.values()[random.nextInt(FactoryDesign.Mob.values().length)];
			return forCity(origin, seed, mob, new BlockPos(cx, CITY_FLOOR_Y, cz));
		} catch (RuntimeException e) {
			BubbasReworkedMobs.LOGGER.warn("Could not work out the Ancient Fabricator of cell {}, {}", gx, gz, e);
			return null;
		}
	}

	/** A site at {@code origin} facing a city centred at {@code city} (world): turned so its tunnel heads there. */
	public static FabricatorSite forCity(BlockPos origin, long seed, FactoryDesign.Mob mob, BlockPos city) {
		int vx = city.getX() - origin.getX();
		int vz = city.getZ() - origin.getZ();
		Rotation best = Rotation.NONE;
		double bestDot = -Double.MAX_VALUE;
		for (Rotation r : Rotation.values()) {
			BlockPos front = new BlockPos(0, 0, 1).rotate(r);
			double dot = front.getX() * (double) vx + front.getZ() * (double) vz;
			if (dot > bestDot) {
				bestDot = dot;
				best = r;
			}
		}
		Rotation inverse = switch (best) {
			case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
			case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
			default -> best;
		};
		BlockPos local = new BlockPos(vx, city.getY() - origin.getY(), vz).rotate(inverse);
		return new FabricatorSite(origin, best, seed, mob, local);
	}

	/** The ancient-city grid cell holding this block column. */
	public static int[] cellOf(ServerLevel level, int blockX, int blockZ) {
		RandomSpreadStructurePlacement placement = cityPlacement(level);
		int spacing = placement == null ? 24 : placement.spacing();
		return new int[] { Math.floorDiv(blockX >> 4, spacing), Math.floorDiv(blockZ >> 4, spacing) };
	}

	/** Every known site (natural ones in the cells round it, and hand-placed) whose pad is within {@code radius}. */
	public static List<FabricatorSite> near(ServerLevel level, BlockPos pos, int radius) {
		List<FabricatorSite> found = new ArrayList<>();
		for (FabricatorSite site : extra(level)) {
			if (site.origin().distSqr(pos) <= (double) radius * radius) {
				found.add(site);
			}
		}
		if (naturalSites(level)) {
			int spacing = 24 * 16;
			RandomSpreadStructurePlacement placement = cityPlacement(level);
			if (placement != null) {
				spacing = placement.spacing() * 16;
			}
			int reach = radius + CITY_DISTANCE + 16;
			for (int gx = Math.floorDiv(pos.getX() - reach, spacing); gx <= Math.floorDiv(pos.getX() + reach, spacing); gx++) {
				for (int gz = Math.floorDiv(pos.getZ() - reach, spacing); gz <= Math.floorDiv(pos.getZ() + reach, spacing); gz++) {
					FabricatorSite site = cellSite(level, gx, gz);
					if (site != null && horizontalDistSqr(site.origin(), pos) <= (double) radius * radius) {
						found.add(site);
					}
				}
			}
		}
		return found;
	}

	public static @Nullable FabricatorSite nearest(ServerLevel level, BlockPos pos, int radius) {
		FabricatorSite best = null;
		for (FabricatorSite site : near(level, pos, radius)) {
			if (best == null || horizontalDistSqr(site.origin(), pos) < horizontalDistSqr(best.origin(), pos)) {
				best = site;
			}
		}
		return best;
	}

	/** Adds a site by hand (tests): forgotten when the server stops. */
	public static FabricatorSite addSite(ServerLevel level, FabricatorSite site) {
		extra(level).add(site);
		return site;
	}

	/** Adds a site by hand and saves it with the level (the op command). */
	public static FabricatorSite addSavedSite(ServerLevel level, FabricatorSite site) {
		addSite(level, site);
		List<Saved> saved = new ArrayList<>(level.getAttachedOrElse(SAVED, List.of()));
		saved.add(new Saved(site.origin(), site.rotation().ordinal(), site.seed(), site.mob().id()));
		level.setAttached(SAVED, List.copyOf(saved));
		return site;
	}

	public static void removeSite(ServerLevel level, FabricatorSite site) {
		extra(level).remove(site);
	}

	private static List<FabricatorSite> extra(ServerLevel level) {
		synchronized (EXTRA) {
			return EXTRA.computeIfAbsent(level, l -> {
				List<FabricatorSite> sites = new CopyOnWriteArrayList<>();
				for (Saved s : l.getAttachedOrElse(SAVED, List.of())) {
					sites.add(new FabricatorSite(s.origin(), Rotation.values()[Math.floorMod(s.turns(), 4)], s.seed(), FactoryDesign.Mob.byId(s.mob()), null));
				}
				return sites;
			});
		}
	}

	// ---- State ----

	public static boolean has(ServerLevel level, FabricatorSite site, int flag) {
		for (State s : level.getAttachedOrElse(STATES, List.of())) {
			if (s.origin().equals(site.origin())) {
				return (s.flags() & flag) != 0;
			}
		}
		return false;
	}

	public static void set(ServerLevel level, FabricatorSite site, int flag) {
		List<State> states = new ArrayList<>(level.getAttachedOrElse(STATES, List.of()));
		int flags = flag;
		for (int i = 0; i < states.size(); i++) {
			if (states.get(i).origin().equals(site.origin())) {
				flags |= states.get(i).flags();
				states.remove(i);
				break;
			}
		}
		states.add(new State(site.origin(), flags));
		level.setAttached(STATES, List.copyOf(states));
	}

	private static double horizontalDistSqr(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX();
		double dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}
}

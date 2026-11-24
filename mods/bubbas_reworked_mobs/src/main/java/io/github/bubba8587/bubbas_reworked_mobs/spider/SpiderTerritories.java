// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Giant spider territories (specs/spiders.md "Territories"): a 2x2 chunk square per giant, keyed by the giant's UUID,
 * saved with the level (a Fabric data attachment). Lookups are by chunk.
 */
public final class SpiderTerritories {
	/** One of a territory's webs: the entity and its hub (for the chunk it sits in). */
	public record WebSpot(UUID id, BlockPos hub) {
		static final Codec<WebSpot> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(WebSpot::id),
				BlockPos.CODEC.fieldOf("hub").forGetter(WebSpot::hub)).apply(i, WebSpot::new));
	}

	/** A claimed 2x2 chunk square; {@link #webs} holds the home web first, then the subsidiary webs. */
	public static final class Territory {
		static final Codec<Territory> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("owner").forGetter(t -> t.owner),
				Codec.INT.fieldOf("min_x").forGetter(t -> t.minX),
				Codec.INT.fieldOf("min_z").forGetter(t -> t.minZ),
				WebSpot.CODEC.listOf().fieldOf("webs").forGetter(t -> List.copyOf(t.webs))).apply(i, Territory::new));

		public final UUID owner;
		/** Chunk coordinates of the square's lowest corner chunk. */
		public final int minX;
		public final int minZ;
		final List<WebSpot> webs;
		int missingTicks;

		Territory(UUID owner, int minX, int minZ, List<WebSpot> webs) {
			this.owner = owner;
			this.minX = minX;
			this.minZ = minZ;
			this.webs = new ArrayList<>(webs);
		}

		public List<WebSpot> webs() {
			return List.copyOf(webs);
		}

		public boolean contains(BlockPos pos) {
			int cx = pos.getX() >> 4;
			int cz = pos.getZ() >> 4;
			return cx >= minX && cx <= minX + 1 && cz >= minZ && cz <= minZ + 1;
		}

		/** The square's block box from {@code minY} to {@code maxY}. */
		public AABB box(double minY, double maxY) {
			return new AABB(minX << 4, minY, minZ << 4, (minX + 2) << 4, maxY, (minZ + 2) << 4);
		}

		long[] chunks() {
			return new long[] {ChunkPos.pack(minX, minZ), ChunkPos.pack(minX + 1, minZ), ChunkPos.pack(minX, minZ + 1), ChunkPos.pack(minX + 1, minZ + 1)};
		}
	}

	public static final AttachmentType<List<Territory>> SAVED = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("spider_territories"),
			Territory.CODEC.listOf());

	private static final class Claims {
		final Map<UUID, Territory> byOwner = new LinkedHashMap<>();
		final Long2ObjectMap<Territory> byChunk = new Long2ObjectOpenHashMap<>();

		void put(Territory t) {
			byOwner.put(t.owner, t);
			for (long c : t.chunks()) {
				byChunk.put(c, t);
			}
		}

		void remove(Territory t) {
			byOwner.remove(t.owner);
			for (long c : t.chunks()) {
				if (byChunk.get(c) == t) {
					byChunk.remove(c);
				}
			}
		}
	}

	private static final Map<ServerLevel, Claims> LEVELS = new WeakHashMap<>();

	private SpiderTerritories() {
	}

	private static Claims claims(ServerLevel level) {
		return LEVELS.computeIfAbsent(level, l -> {
			Claims claims = new Claims();
			List<Territory> saved = l.getAttached(SAVED);
			if (saved != null) {
				for (Territory t : saved) {
					claims.put(new Territory(t.owner, t.minX, t.minZ, t.webs));
				}
			}
			return claims;
		});
	}

	/** Drops the in-memory claims so the next lookup reads them back from the saved attachment (tests: a reload). */
	public static void reloadFromSaved(ServerLevel level) {
		LEVELS.remove(level);
	}

	private static void save(ServerLevel level) {
		level.setAttached(SAVED, List.copyOf(claims(level).byOwner.values()));
	}

	public static List<Territory> all(ServerLevel level) {
		return List.copyOf(claims(level).byOwner.values());
	}

	public static @Nullable Territory of(ServerLevel level, UUID owner) {
		return claims(level).byOwner.get(owner);
	}

	public static @Nullable Territory at(ServerLevel level, BlockPos pos) {
		return claims(level).byChunk.get(ChunkPos.pack(pos));
	}

	/** Whether the giant {@code owner} may spin a web with its hub at {@code hub}: inside its own territory, or unclaimed land if it has none. */
	public static boolean mayWeb(ServerLevel level, UUID owner, Vec3 hub) {
		Territory here = at(level, BlockPos.containing(hub));
		Territory mine = of(level, owner);
		return mine != null ? here == mine : here == null;
	}

	/**
	 * Claims a 2x2 square holding {@code home} for {@code owner}: first the square that puts the web nearest its middle
	 * (by which quarter of its chunk the hub is in), else any other free square holding the web's chunk. Null if all four
	 * overlap someone's claim.
	 */
	public static @Nullable Territory claim(ServerLevel level, UUID owner, SpunWeb home) {
		Claims claims = claims(level);
		Territory had = claims.byOwner.get(owner);
		if (had != null) {
			return had;
		}
		BlockPos hub = BlockPos.containing(home.position());
		int cx = hub.getX() >> 4;
		int cz = hub.getZ() >> 4;
		int px = (hub.getX() & 15) < 8 ? cx - 1 : cx;
		int pz = (hub.getZ() & 15) < 8 ? cz - 1 : cz;
		int ox = px == cx ? cx - 1 : cx;
		int oz = pz == cz ? cz - 1 : cz;
		int[][] squares = {{px, pz}, {ox, pz}, {px, oz}, {ox, oz}};
		for (int[] sq : squares) {
			Territory t = new Territory(owner, sq[0], sq[1], List.of(new WebSpot(home.getUUID(), hub)));
			boolean free = true;
			for (long c : t.chunks()) {
				Territory other = claims.byChunk.get(c);
				if (other != null && !other.owner.equals(owner)) {
					free = false;
					break;
				}
			}
			if (free) {
				claims.put(t);
				save(level);
				return t;
			}
		}
		return null;
	}

	public static void addWeb(ServerLevel level, Territory t, SpunWeb web) {
		t.webs.add(new WebSpot(web.getUUID(), BlockPos.containing(web.position())));
		save(level);
	}

	/**
	 * Forgets webs that are gone (only where their chunk's entities are loaded: an unloaded web still counts) and
	 * returns the loaded live ones, home first.
	 */
	public static List<SpunWeb> liveWebs(ServerLevel level, Territory t) {
		List<SpunWeb> out = new ArrayList<>();
		boolean changed = t.webs.removeIf(spot -> {
			if (!level.areEntitiesLoaded(ChunkPos.pack(spot.hub()))) {
				return false;
			}
			if (level.getEntity(spot.id()) instanceof SpunWeb web && !web.isRemoved()) {
				out.add(web);
				return false;
			}
			return true;
		});
		if (changed) {
			save(level);
		}
		return out;
	}

	/** Its home web if loaded and standing, else null. */
	public static @Nullable SpunWeb home(ServerLevel level, Territory t) {
		List<SpunWeb> live = liveWebs(level, t);
		return !live.isEmpty() && !t.webs.isEmpty() && live.getFirst().getUUID().equals(t.webs.getFirst().id()) ? live.getFirst() : null;
	}

	/** Its subsidiary webs that are loaded and standing (all but the home web). */
	public static List<SpunWeb> subsidiaryWebs(ServerLevel level, Territory t) {
		List<SpunWeb> live = liveWebs(level, t);
		if (t.webs.isEmpty()) {
			return live;
		}
		UUID home = t.webs.getFirst().id();
		return live.stream().filter(w -> !w.getUUID().equals(home)).toList();
	}

	public static void release(ServerLevel level, UUID owner) {
		Claims claims = claims(level);
		Territory t = claims.byOwner.get(owner);
		if (t != null) {
			claims.remove(t);
			save(level);
		}
	}

	/** Releases territories whose giant has been missing while all four chunks' entities were loaded. */
	public static void tick(ServerLevel level) {
		if (level.getGameTime() % SpiderTuning.TERRITORY_CHECK_TICKS != 0) {
			return;
		}
		Claims claims = claims(level);
		if (claims.byOwner.isEmpty()) {
			return;
		}
		List<Territory> stale = new ArrayList<>();
		for (Territory t : claims.byOwner.values()) {
			boolean loaded = true;
			for (long c : t.chunks()) {
				loaded &= level.areEntitiesLoaded(c);
			}
			if (!loaded) {
				continue;
			}
			if (level.getEntity(t.owner) instanceof GiantSpider giant && giant.isAlive()) {
				t.missingTicks = 0;
			} else if ((t.missingTicks += SpiderTuning.TERRITORY_CHECK_TICKS) >= SpiderTuning.TERRITORY_STALE_TICKS) {
				stale.add(t);
			}
		}
		if (!stale.isEmpty()) {
			stale.forEach(claims::remove);
			save(level);
		}
	}
}

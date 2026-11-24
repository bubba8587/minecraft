// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

/**
 * The camps and pillager outposts of a level (specs/monster-camps.md), saved with the level (a Fabric data attachment).
 * Camps are found from the structure references of the chunks round each player, registered the first time, then:
 * <ul>
 * <li><b>armed</b> while a banner of the camp's kind stands in its box (read from the box's block entities while a
 * player is near, never by scanning blocks);</li>
 * <li>an armed camp's <b>roster appears</b> when a player comes within {@link CampTuning#TRIGGER_RANGE}
 * ({@link CampRoster});</li>
 * <li>once a day, a camp whose roster is dead and whose banner stands <b>resets</b> with chance
 * {@link CampTuning#RESET_CHANCE}, only while nobody is within {@link CampTuning#AWAY_RANGE}; a camp re-armed by
 * putting a banner back resets at the next check for sure;</li>
 * <li>a lit campfire anywhere in a camp's box <b>quiets</b> it while it burns: no roster, no reset;</li>
 * <li>a pillager outpost with no banner standing spawns no pillagers ({@link #disarmedOutpostAt}).</li>
 * </ul>
 */
public final class CampSites {
	public static final AttachmentType<List<CampSite>> SAVED = AttachmentRegistry.createPersistent(BubbasReworkedMobs.id("camp_sites"),
			CampSite.CODEC.listOf());

	private static final class Sites {
		final Map<Long, CampSite> byId = new LinkedHashMap<>();
		/** Outposts with no banner standing (checked on every monster spawn attempt there, so kept apart). */
		List<BoundingBox> disarmedOutposts = List.of();
		long lastDay = Long.MIN_VALUE;
	}

	private static final Map<ServerLevel, Sites> LEVELS = new WeakHashMap<>();

	private CampSites() {
	}

	private static Sites sites(ServerLevel level) {
		return LEVELS.computeIfAbsent(level, l -> {
			Sites sites = new Sites();
			List<CampSite> saved = l.getAttached(SAVED);
			if (saved != null) {
				for (CampSite site : saved) {
					sites.byId.put(site.id, site);
				}
			}
			refreshOutposts(sites);
			return sites;
		});
	}

	/** Marks the level's camps changed (saved with it). */
	static void save(ServerLevel level) {
		Sites sites = sites(level);
		refreshOutposts(sites);
		level.setAttached(SAVED, List.copyOf(sites.byId.values()));
	}

	private static void refreshOutposts(Sites sites) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (CampSite site : sites.byId.values()) {
			if (site.isOutpost() && !site.armed) {
				boxes.add(site.box);
			}
		}
		sites.disarmedOutposts = List.copyOf(boxes);
	}

	/** Every registered camp and outpost of the level. */
	public static List<CampSite> all(ServerLevel level) {
		return List.copyOf(sites(level).byId.values());
	}

	public static @Nullable CampSite get(ServerLevel level, long id) {
		return sites(level).byId.get(id);
	}

	/** Registers a camp built by hand (the command, tests) from its piece. */
	public static CampSite register(ServerLevel level, CampPiece piece) {
		long id = ChunkPos.containing(piece.centre()).pack();
		CampSite site = CampSite.camp(id, piece, piece.towerSpots(), piece.groundSpots());
		sites(level).byId.put(id, site);
		save(level);
		return site;
	}

	/** Registers a pillager outpost's box by hand (tests); normally outposts are found from their structure. */
	public static CampSite registerOutpost(ServerLevel level, BoundingBox box) {
		long id = ChunkPos.containing(box.getCenter()).pack() ^ 0x7905L;
		CampSite site = CampSite.outpost(id, box);
		sites(level).byId.put(id, site);
		save(level);
		return site;
	}

	/** Whether {@code pos} is inside a pillager outpost whose banners are all gone (its pillagers stop spawning). */
	public static boolean disarmedOutpostAt(ServerLevel level, BlockPos pos) {
		List<BoundingBox> boxes = sites(level).disarmedOutposts;
		for (int i = 0; i < boxes.size(); i++) {
			if (boxes.get(i).isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	// --- ticking ----------------------------------------------------------------------------------

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % CampTuning.CHECK_TICKS != 0) {
			return;
		}
		List<ServerPlayer> players = level.players().stream().filter(EntitySelector.NO_SPECTATORS).toList();
		List<ServerPlayer> fighters = players.stream().filter(EntitySelector.NO_CREATIVE_OR_SPECTATOR).toList();
		boolean changed = false;
		for (ServerPlayer player : players) {
			changed |= discover(level, player.chunkPosition());
		}
		Sites sites = sites(level);
		for (CampSite site : sites.byId.values()) {
			if (nearest(players, site.centre) <= CampTuning.AWAY_RANGE) {
				changed |= refresh(level, site);
			}
			if (!site.isOutpost() && site.armed && !site.quiet && !site.spawned && nearest(fighters, site.centre) <= CampTuning.TRIGGER_RANGE
					&& level.getChunkSource().getChunkNow(site.centre.getX() >> 4, site.centre.getZ() >> 4) != null) {
				CampRoster.spawn(level, site);
				changed = true;
			}
		}
		// Once a day: cleared camps with a banner standing may reset while nobody is near.
		long day = Math.floorDiv(level.getOverworldClockTime(), 24000L);
		if (sites.lastDay == Long.MIN_VALUE) {
			sites.lastDay = day;
		} else if (day != sites.lastDay) {
			sites.lastDay = day;
			for (CampSite site : sites.byId.values()) {
				if (nearest(players, site.centre) > CampTuning.AWAY_RANGE && tryReset(level, site, false)) {
					changed = true;
				}
			}
		}
		if (changed) {
			save(level);
		}
	}

	/** Runs the daily reset check now, whoever is near; {@code sure} skips the chance. Returns how many camps reset. */
	public static int resetCheck(ServerLevel level, boolean sure) {
		int reset = 0;
		for (CampSite site : sites(level).byId.values()) {
			if (tryReset(level, site, sure)) {
				reset++;
			}
		}
		save(level);
		return reset;
	}

	/** A cleared, armed, unquiet camp resets (by chance unless {@code sure}, or always when re-armed); true if it did. */
	private static boolean tryReset(ServerLevel level, CampSite site, boolean sure) {
		if (site.isOutpost() || !site.spawned || !site.members.isEmpty() || !site.armed || site.quiet
				|| !(sure || site.rearmed || level.getRandom().nextFloat() < CampTuning.RESET_CHANCE)) {
			return false;
		}
		site.spawned = false;
		site.rearmed = false;
		return true;
	}

	/** Re-reads a camp's banners and fire now; returns whether its banners stand, null when its box isn't loaded. */
	public static @Nullable Boolean recheck(ServerLevel level, CampSite site) {
		refresh(level, site);
		save(level);
		return CampBanners.standsIn(level, site.box, site.kind);
	}

	/** Re-reads whether a camp's banners stand and a fire burns in it; true if either changed. */
	private static boolean refresh(ServerLevel level, CampSite site) {
		boolean changed = false;
		Boolean stands = CampBanners.standsIn(level, site.box, site.kind);
		if (stands != null && stands != site.armed) {
			if (stands && site.spawned && site.members.isEmpty()) {
				site.rearmed = true; // a banner put back in a cleared camp
			}
			site.armed = stands;
			changed = true;
		}
		if (!site.isOutpost()) {
			Boolean fire = CampBanners.litFireIn(level, site.box);
			if (fire != null && fire != site.quiet) {
				site.quiet = fire;
				changed = true;
			}
		}
		return changed;
	}

	private static double nearest(List<ServerPlayer> players, BlockPos centre) {
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : players) {
			double dx = player.getX() - (centre.getX() + 0.5);
			double dz = player.getZ() - (centre.getZ() + 0.5);
			best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
		}
		return best;
	}

	/** Registers camps and outposts referenced by the loaded chunks round {@code around}; returns whether any were new. */
	private static boolean discover(ServerLevel level, ChunkPos around) {
		var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Structure camp = structures.getValue(Camps.CAMP_KEY);
		Structure nether = structures.getValue(Camps.OUTPOST_KEY);
		Structure outpost = structures.getValue(BuiltinStructures.PILLAGER_OUTPOST);
		Sites sites = sites(level);
		boolean found = false;
		int r = CampTuning.DISCOVER_CHUNKS;
		for (int cx = around.x() - r; cx <= around.x() + r; cx++) {
			for (int cz = around.z() - r; cz <= around.z() + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				if (nether != null) {
					found |= register(level, sites, nether, chunk.getReferencesForStructure(nether), false);
				}
				if (camp != null) {
					found |= register(level, sites, camp, chunk.getReferencesForStructure(camp), false);
				}
				if (outpost != null) {
					found |= register(level, sites, outpost, chunk.getReferencesForStructure(outpost), true);
				}
			}
		}
		return found;
	}

	private static boolean register(ServerLevel level, Sites sites, Structure structure, LongSet references, boolean isOutpost) {
		boolean found = false;
		for (long ref : references) {
			if (sites.byId.containsKey(ref)) {
				continue;
			}
			ChunkAccess startChunk = level.getChunk(ChunkPos.getX(ref), ChunkPos.getZ(ref), ChunkStatus.STRUCTURE_STARTS, false);
			if (startChunk == null) {
				continue;
			}
			StructureStart start = startChunk.getStartForStructure(structure);
			if (start == null || !start.isValid()) {
				continue;
			}
			if (isOutpost) {
				sites.byId.put(ref, CampSite.outpost(ref, start.getBoundingBox()));
				found = true;
			} else if (!start.getPieces().isEmpty() && start.getPieces().getFirst() instanceof CampPiece piece) {
				sites.byId.put(ref, CampSite.camp(ref, piece, piece.towerSpots(), piece.groundSpots()));
				found = true;
			}
		}
		return found;
	}

	// --- the roster's deaths ----------------------------------------------------------------------

	/** A camp mob left the world for good: off its camp's roster. */
	public static void removed(ServerLevel level, Entity entity) {
		CampSite site = siteOf(level, entity);
		if (site != null && site.members.remove(entity.getUUID())) {
			save(level);
		}
	}

	/** A camp mob converted (zombie to drowned, skeleton to stray): the new mob takes its place on the roster. */
	public static void converted(ServerLevel level, Entity from, Entity to) {
		CampSite site = siteOf(level, from);
		if (site != null) {
			site.members.remove(from.getUUID());
			if (!site.members.contains(to.getUUID())) {
				site.members.add(to.getUUID());
			}
			save(level);
		}
	}

	private static @Nullable CampSite siteOf(ServerLevel level, Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(CampTuning.CAMP_TAG + ":")) {
				try {
					return sites(level).byId.get(Long.parseLong(tag.substring(CampTuning.CAMP_TAG.length() + 1)));
				} catch (NumberFormatException ignored) {
					// not ours
				}
			}
		}
		return null;
	}
}

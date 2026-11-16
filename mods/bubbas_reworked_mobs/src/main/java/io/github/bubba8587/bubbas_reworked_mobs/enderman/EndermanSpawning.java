// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Where overworld endermen come from (D2, E2, specs/endermen.md): mostly cairn sites (the nexus), plus vanilla's
 * random spawns at the monster cap's low rate. A site spawns while a player is within
 * SITE_ACTIVE_RANGE: slowly while its cairn is a stub, faster as it fills in (chance and cap rise with the share
 * standing), much faster once it matches its design, and faster the more of its endermen are already about (left alone it snowballs; killing them slows it, tearing the cairn down drops it
 * back to the unfinished rate). A ward over the site's centre stops it. New endermen arrive carrying the lowest of the
 * cairn's missing blocks that no enderman there carries yet (builders; one that can't go in yet waits by the cairn), carrying the original block for a hole nearby, or empty-handed (repairers).
 *
 * <p>Vanilla's spawn rules still apply to each spot: the biome must list endermen, the spot must pass the enderman's
 * spawn placement and darkness rules, and spawn_mobs / spawn_monsters and peaceful turn it off. Everything follows
 * the game rule {@code bubbas_reworked_mobs:cairn_endermen} (off: cairns spawn nothing). Vanilla's random overworld
 * enderman spawns stay, at the monster cap cut every monster gets (owner, 2026-11-16: a small natural rate); those
 * arrive empty-handed and tend the land around where they show up.
 *
 * <p>Cost: every SPAWN_INTERVAL ticks, per active site (not per player or block): one entity box query, one pass over
 * its design (about 40-120 block reads) and, when it spawns, up to SPAWN_TRIES column probes.
 */
public final class EndermanSpawning {
	private EndermanSpawning() {
	}

	static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD || level.getServer() instanceof GameTestServer || level.getGameTime() % EndermanTuning.SPAWN_INTERVAL != 0) {
			return;
		}
		if (!spawningAllowed(level)) {
			return;
		}
		Set<CairnSite> active = new LinkedHashSet<>();
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				active.addAll(CairnSites.near(level, player.blockPosition(), EndermanTuning.SITE_ACTIVE_RANGE));
			}
		}
		for (CairnSite site : active) {
			trySpawnAt(level, site, false);
		}
	}

	static boolean spawningAllowed(ServerLevel level) {
		return level.getGameRules().get(ModGameRules.CAIRN_ENDERMEN) && level.getGameRules().get(GameRules.SPAWN_MOBS)
				&& level.getGameRules().get(GameRules.SPAWN_MONSTERS) && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	/** Endermen near a site (its own). */
	public static List<Enderman> endermenAt(ServerLevel level, CairnSite site) {
		return level.getEntitiesOfClass(Enderman.class, new AABB(site.origin()).inflate(EndermanTuning.SITE_RADIUS), Enderman::isAlive);
	}

	/** This check's spawn chance for a site whose spire has grown {@code growth} (0-1), with {@code present} of its endermen about. */
	public static float chance(boolean complete, float growth, int present) {
		float base = complete ? EndermanTuning.SPAWN_CHANCE_COMPLETE : EndermanTuning.SPAWN_CHANCE_EMPTY
				+ (EndermanTuning.SPAWN_CHANCE_ALMOST - EndermanTuning.SPAWN_CHANCE_EMPTY) * (float) Math.pow(growth, EndermanTuning.SPAWN_CURVE);
		return Math.min(1.0F, base * (1.0F + EndermanTuning.SNOWBALL_PER_ENDERMAN * present));
	}

	/** How many of its own endermen a site spawns at most. */
	public static int cap(boolean complete, float growth) {
		return complete ? EndermanTuning.CAP_COMPLETE
				: EndermanTuning.CAP_EMPTY + Math.round((EndermanTuning.CAP_ALMOST - EndermanTuning.CAP_EMPTY) * growth * growth);
	}

	/** The share of newcomers at an unfinished site that arrive carrying one of its missing blocks. */
	public static float builderShare(float growth) {
		return EndermanTuning.BUILDER_SHARE_EMPTY + (EndermanTuning.BUILDER_SHARE_ALMOST - EndermanTuning.BUILDER_SHARE_EMPTY) * growth;
	}

	/**
	 * One spawn check for a site; {@code force} skips the chance roll (tests). Returns the new enderman, or null.
	 */
	public static @Nullable Enderman trySpawnAt(ServerLevel level, CairnSite site, boolean force) {
		return trySpawnAt(level, site, force, EndermanTuning.SPAWN_MIN_DISTANCE, EndermanTuning.SPAWN_MAX_DISTANCE);
	}

	/** As above, spawning between {@code minDistance} and {@code maxDistance} blocks from the site's centre. */
	public static @Nullable Enderman trySpawnAt(ServerLevel level, CairnSite site, boolean force, double minDistance, double maxDistance) {
		if (!level.isLoaded(site.origin()) || EndermanWards.isWarded(level, site.origin())) {
			return null;
		}
		boolean complete = site.complete(level);
		float growth = complete ? 1.0F : site.growth(level);
		List<Enderman> own = endermenAt(level, site);
		int present = own.size();
		boolean bottleneck = false;
		if (present >= cap(complete, growth)) {
			// At the cap, an unfinished cairn still calls one builder when all its endermen are builders whose blocks wait
			// (the blocks under them are missing) and the block that could go in next is carried by nobody (its holder
			// died or despawned): otherwise the build stalls for good. Once that builder is there it can work, so at
			// most one extra.
			if (complete || own.stream().anyMatch(e -> e.getCarriedBlock() == null || site.spotFor(level, e.getCarriedBlock()) != null)) {
				return null;
			}
			BlockPos next = site.nextUnclaimed(level, own);
			if (next == null || !CairnSite.canPlace(level, next, site.at(next))) {
				return null;
			}
			bottleneck = true;
		}
		RandomSource random = level.getRandom();
		if (!force && random.nextFloat() >= chance(complete, growth, present)) {
			return null;
		}
		for (int i = 0; i < EndermanTuning.SPAWN_TRIES; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = Mth.lerp(random.nextDouble(), minDistance, maxDistance);
			int x = Mth.floor(site.origin().getX() + Math.cos(angle) * distance);
			int z = Mth.floor(site.origin().getZ() + Math.sin(angle) * distance);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos cell = spawnCell(level, x, site.origin().getY(), z);
			if (cell == null || EndermanWards.isWarded(level, cell) || !allowedHere(level, cell, random) || level.getNearestPlayer(x + 0.5, cell.getY(), z + 0.5, 24.0, false) != null) {
				continue;
			}
			Enderman enderman = EntityTypes.ENDERMAN.create(level, EntitySpawnReason.NATURAL);
			if (enderman == null) {
				return null;
			}
			enderman.snapTo(x + 0.5, cell.getY(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (!level.noCollision(enderman)) {
				continue;
			}
			((EndermanAccess) enderman).brm$markCairnSpawn();
			enderman.finalizeSpawn(level, level.getCurrentDifficultyAt(cell), EntitySpawnReason.NATURAL, null);
			giveJob(level, enderman, site, complete, bottleneck, random);
			return level.tryAddFreshEntityWithPassengers(enderman) ? enderman : null;
		}
		return null;
	}

	/** The highest spot in this column within SPAWN_HEIGHT of the site's ground with a floor and room for an enderman. */
	private static @Nullable BlockPos spawnCell(ServerLevel level, int x, int y, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dy = EndermanTuning.SPAWN_HEIGHT; dy >= -EndermanTuning.SPAWN_HEIGHT; dy--) {
			pos.set(x, y + dy, z);
			BlockPos below = pos.below();
			if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP) && level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(2)).isAir()
					&& SpawnPlacements.isSpawnPositionOk(EntityTypes.ENDERMAN, level, pos)) {
				return pos.immutable();
			}
		}
		return null;
	}

	/**
	 * Builder (carries a missing cairn block), filler (carries the block a nearby hole needs) or repairer (empty). At a
	 * complete cairn newcomers come empty-handed: with nothing to do they stand vigil around it.
	 */
	public static void giveJob(ServerLevel level, Enderman enderman, CairnSite site, boolean complete, RandomSource random) {
		giveJob(level, enderman, site, complete, false, random);
	}

	/** As above; {@code builder} makes it a builder for sure (the cap's bottleneck spawn). */
	public static void giveJob(ServerLevel level, Enderman enderman, CairnSite site, boolean complete, boolean builder, RandomSource random) {
		EndermanMind mind = EndermanMind.of(enderman);
		if (mind == null || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		if (complete) {
			return;
		}
		if (builder || random.nextFloat() < builderShare(site.growth(level))) {
			// The lowest missing block nobody here carries yet; it goes in now if it can, else it waits by the cairn.
			BlockPos spot = site.nextUnclaimed(level, endermenAt(level, site));
			if (spot != null) {
				BlockState want = site.at(spot);
				enderman.setCarriedBlock(want);
				if (CairnSite.canPlace(level, spot, want)) {
					mind.set(EndermanMind.Job.DELIVER, spot, level.getGameTime());
				}
				return;
			}
		}
		// Empty-handed at an unfinished cairn: it knows one thing in the cairn's way and comes to clear it (owner: "if the
		// cairn site calls for removal of dirt/trees/etc, then a block-less enderman can remove one").
		BlockPos obstacle = site.nextObstacle(level);
		if (obstacle != null) {
			mind.set(EndermanMind.Job.TAKE, obstacle, level.getGameTime());
			return;
		}
		if (random.nextBoolean()) {
			// A filler: find a hole near the site and bring its original block.
			BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
			for (int i = 0; i < EndermanTuning.FILL_COLUMNS; i++) {
				int x = site.origin().getX() + random.nextInt(2 * EndermanTuning.FILL_RADIUS + 1) - EndermanTuning.FILL_RADIUS;
				int z = site.origin().getZ() + random.nextInt(2 * EndermanTuning.FILL_RADIUS + 1) - EndermanTuning.FILL_RADIUS;
				int surface = TargetWorld.surface(level, x, z);
				if (surface == Integer.MIN_VALUE || !level.hasChunk(x >> 4, z >> 4)) {
					continue;
				}
				for (int y = surface; y > surface - EndermanTuning.FILL_DEPTH; y--) {
					at.set(x, y, z);
					BlockState target = TargetWorld.target(level, at);
					if (target != null && TerrainKinds.family(target) != null && EndermanWork.needsFill(level, at, target)) {
						enderman.setCarriedBlock(target);
						mind.set(EndermanMind.Job.DELIVER, at.immutable(), level.getGameTime());
						return;
					}
				}
			}
		}
	}

	/** Vanilla's rules for a natural enderman here: the biome lists endermen, and its spawn placement and light rules pass. */
	private static boolean allowedHere(ServerLevel level, BlockPos cell, RandomSource random) {
		boolean listed = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, cell)
				.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream().anyMatch(w -> w.value().type() == EntityTypes.ENDERMAN);
		return listed && SpawnPlacements.isSpawnPositionOk(EntityTypes.ENDERMAN, level, cell)
				&& SpawnPlacements.checkSpawnRules(EntityTypes.ENDERMAN, level, EntitySpawnReason.NATURAL, cell, random);
	}
}

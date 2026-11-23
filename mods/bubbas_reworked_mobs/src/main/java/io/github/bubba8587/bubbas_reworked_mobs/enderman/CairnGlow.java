// dte:C1,D2
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Particles on complete cairns (owner, 2026-11-16; specs/endermen.md, Vigil): a slow purple portal shimmer around its
 * blocks, and a thin stream of end rod sparkles rising from its highest block, sent long-distance so a finished cairn
 * can be spotted from afar at night. Server side, vanilla particles, no new assets.
 *
 * <p>Cost: every GLOW_CHECK_TICKS, one completeness check (one pass over the design, ~50 reads) per site within
 * GLOW_RANGE of a player; every GLOW_TICKS, a few particle packets per complete one.
 */
public final class CairnGlow {
	private static final Map<ServerLevel, Map<CairnSite, Boolean>> COMPLETE = new WeakHashMap<>();

	private CairnGlow() {
	}

	static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}
		long time = level.getGameTime();
		if (time % EndermanTuning.GLOW_TICKS != 0) {
			return;
		}
		boolean recheck = time % EndermanTuning.GLOW_CHECK_TICKS == 0;
		Map<CairnSite, Boolean> known = COMPLETE.computeIfAbsent(level, l -> new HashMap<>());
		java.util.Set<CairnSite> near = CairnSites.nearPlayers(level, EndermanTuning.GLOW_RANGE, true); // spectators see it too
		known.keySet().retainAll(near);
		RandomSource random = level.getRandom();
		for (CairnSite site : near) {
			if (!level.isLoaded(site.origin())) {
				continue;
			}
			Boolean complete = known.get(site);
			if (complete == null || recheck) {
				complete = site.complete(level);
				known.put(site, complete);
			}
			if (complete) {
				glow(level, site, random);
			}
		}
	}

	private static void glow(ServerLevel level, CairnSite site, RandomSource random) {
		List<BlockPos> order = site.order();
		for (int i = 0; i < 2; i++) {
			BlockPos pos = order.get(random.nextInt(order.size()));
			level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.6, 0.6, 0.6, 0.4);
		}
		BlockPos top = order.getLast();
		for (BlockPos pos : order) {
			if (pos.getY() > top.getY()) {
				top = pos;
			}
		}
		level.sendParticles(ParticleTypes.END_ROD, false, true, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 1, 0.08, 0.2, 0.08, 0.02);
	}
}

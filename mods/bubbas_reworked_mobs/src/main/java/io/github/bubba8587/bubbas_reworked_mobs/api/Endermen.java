// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.api;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanWards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The small public hook other mods use to keep Reworked Mobs' caretaker endermen out of an area (specs/endermen.md,
 * E2), like {@link Sleepers}. Bubba's Totems & Artifacts calls it for every Totem of Warding ward, only when this mod
 * is installed. Inside a kept-out area endermen repair and build nothing, and a cairn site whose centre is inside
 * spawns no endermen. Nothing here depends on that mod.
 *
 * <p>Stable: keep these signatures; add, don't change. Server thread only.
 */
public final class Endermen {
	private Endermen() {
	}

	/**
	 * Keeps endermen's work out of {@code area} for {@code lifetimeTicks}. {@code source} is any key of the caller's;
	 * calling again replaces and refreshes that source's area, so a caller that stops refreshing needs no cleanup.
	 */
	public static void keepOut(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		EndermanWards.put(level, source, area, lifetimeTicks);
	}

	/** Ends {@code source}'s area now. */
	public static void release(ServerLevel level, Object source) {
		EndermanWards.remove(level, source);
	}

	/** Whether some caller's area keeps endermen out of {@code pos}. */
	public static boolean isKeptOut(Level level, BlockPos pos) {
		return EndermanWards.isWarded(level, pos);
	}

	/** Whether {@code pos} is part of a (worked-out) cairn site's design. */
	public static boolean isCairnBlock(ServerLevel level, BlockPos pos) {
		CairnSite site = CairnSites.covering(level, pos);
		return site != null && site.at(pos) != null;
	}
}

// dte:C1,C6
package io.github.bubba8587.bubbas_reworked_mobs.api;

import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnGifts;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSite;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.CairnSites;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanNexus;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanWards;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.Ripples;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

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

	// --- added 2026-11-22 for the Totems of the Beginning and the End ------------------------------------

	/**
	 * Starts a ripple at {@code centre}: the ground within {@code radius} (a cylinder, as far up and down) goes back,
	 * ring by ring outward ({@code ticksPerBlock} ticks a block), to how it generated, as endermen see it. Only terrain
	 * blocks and air change; built things and the columns round them are left alone. False when this level has no
	 * generated terrain to go back to (not the overworld, or a generator that can't be reproduced): nothing happens.
	 */
	public static boolean restoreAround(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock) {
		return Ripples.start(level, centre, radius, ticksPerBlock);
	}

	/**
	 * Starts a pulse at {@code centre}: like {@link #restoreAround}, but each block only goes back with {@code chance}
	 * ({@code oreChance} where the generated block is an ore, and ore only into air), never one a pulse reverted within
	 * {@code recentTicks}, never an unbreakable block or one {@code player} may not touch (null: no player check). Each
	 * reverted block puffs and sounds ({@link #revertEffect}). False when this level has no generated terrain.
	 */
	public static boolean pulseAround(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock, float chance, float oreChance,
			int recentTicks, @Nullable Player player) {
		return Ripples.pulse(level, centre, radius, ticksPerBlock, new Ripples.Odds(chance, oreChance, recentTicks, player));
	}

	/** The revert effect on one block: a particle puff and a soft, low sound (throttled per level). */
	public static void revertEffect(ServerLevel level, BlockPos pos) {
		Ripples.revertEffect(level, pos);
	}

	/**
	 * {@code area} draws caretaker endermen like a cairn nexus for {@code lifetimeTicks} (they spawn there and tidy the
	 * ground). Like {@link #keepOut}: calling again refreshes {@code source}'s area, which lapses unless refreshed.
	 */
	public static void drawIn(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		EndermanNexus.put(level, source, area, lifetimeTicks);
	}

	/** Ends {@code source}'s drawing area now. */
	public static void stopDrawing(ServerLevel level, Object source) {
		EndermanNexus.remove(level, source);
	}

	/**
	 * Players endermen treat as their own: breaking or hitting near a cairn doesn't turn the others on them. (Eye
	 * contact is the caller's: a vanilla rule, so a mixin of its own covers every enderman.) Replaces any earlier one.
	 */
	public static void setKin(Predicate<Player> kin) {
		io.github.bubba8587.bubbas_reworked_mobs.enderman.Endermen.setKin(kin);
	}

	/**
	 * What cairns give, once ever per site and saved with the world: {@code offering} is laid at the foot of some
	 * complete cairns (picked from each site's seed) by their endermen. May be null (none); nothing is marked while it
	 * is. No reward for completing a cairn (owner, 2026-11-22).
	 */
	public static void setCairnGifts(@Nullable Supplier<ItemStack> offering) {
		CairnGifts.set(offering);
	}
}

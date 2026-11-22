// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Sleepers (dormant undead) and caretaker endermen belong to Bubba's Reworked Mobs, an optional mod (specs/totems-artifacts.md,
 * "Independence"). Everything here that touches them goes through this hook: with Reworked Mobs installed it calls that
 * mod's api/Sleepers ({@link ReworkedMobsSleepers}, the only class that names it, loaded only then); without it there
 * are no sleepers and every call does nothing.
 */
public interface SleeperHook {
	/** No sleepers: Reworked Mobs isn't installed. */
	SleeperHook NONE = new SleeperHook() {
	};

	/** The hook for this game, picked once. */
	static SleeperHook get() {
		return Holder.HOOK;
	}

	/** Takes {@code points} off the disturbance meter of sleepers within {@code radius} (a held totem's pulse). */
	default void calm(ServerLevel level, Vec3 centre, double radius, float points) {
	}

	/** Sleepers inside {@code area} stay asleep for {@code lifetimeTicks} (a full-pyramid ward); refresh to keep it. */
	default void keepAsleep(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
	}

	/** Ends {@code source}'s keep-asleep area. */
	default void release(ServerLevel level, Object source) {
	}

	/** Reworked Mobs' endermen do no work inside {@code area} (any ward) for {@code lifetimeTicks}; refresh to keep it. */
	default void keepEndermenOut(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
	}

	/** Ends {@code source}'s keep-endermen-out area. */
	default void releaseEndermen(ServerLevel level, Object source) {
	}

	/** A ward leaves this mob alone: a Reworked Mobs Fabricator mob still deactivated (limp, waiting to be teleported). */
	default boolean wardIgnores(Entity entity) {
		return false;
	}

	/**
	 * Starts the Totem of the Beginning's ripple: the ground round {@code centre} goes back to how it generated (Reworked
	 * Mobs' endermen's target world). False when it can't (no Reworked Mobs, or no generated terrain here).
	 */
	default boolean restoreAround(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock) {
		return false;
	}

	/** {@code area} draws Reworked Mobs' caretaker endermen like a cairn for {@code lifetimeTicks}; refresh to keep it. */
	default void drawEndermen(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
	}

	/**
	 * Tells Reworked Mobs what its cairns give ({@code offering}: laid at some complete cairns) and which players its
	 * endermen treat as kin.
	 */
	default void setUpEndermen(Supplier<ItemStack> offering, Predicate<Player> kin) {
	}

	/** Squared distance to the nearest sleeper within {@code range} (client or server), or {@code Double.MAX_VALUE}. */
	default double nearestSleeperSqr(Level level, Vec3 centre, double range) {
		return Double.MAX_VALUE;
	}

	final class Holder {
		private static final SleeperHook HOOK = FabricLoader.getInstance().isModLoaded("bubbas_reworked_mobs")
				? new ReworkedMobsSleepers() : NONE;

		private Holder() {
		}
	}
}

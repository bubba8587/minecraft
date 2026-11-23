// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.compat;

import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.bubba8587.bubbas_reworked_mobs.api.Endermen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import io.github.bubba8587.bubbas_reworked_mobs.api.Fabricators;
import net.minecraft.world.entity.Entity;
import io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@link SleeperHook} through Bubba's Reworked Mobs' api/Sleepers. The only class that touches that mod; created only
 * when it is installed (SleeperHook.Holder checks), so it is never loaded otherwise.
 */
final class ReworkedMobsSleepers implements SleeperHook {
	@Override
	public void calm(ServerLevel level, Vec3 centre, double radius, float points) {
		Sleepers.calm(level, centre, radius, points);
	}

	@Override
	public void keepAsleep(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		Sleepers.keepAsleep(level, source, area, lifetimeTicks);
	}

	@Override
	public void release(ServerLevel level, Object source) {
		Sleepers.release(level, source);
	}

	@Override
	public void keepEndermenOut(ServerLevel level, Object source, AABB area, int lifetimeTicks) {
		try {
			Endermen.keepOut(level, source, area, lifetimeTicks);
		} catch (LinkageError e) {
			// An older Reworked Mobs without api/Endermen: nothing to keep out.
		}
	}

	@Override
	public void releaseEndermen(ServerLevel level, Object source) {
		try {
			Endermen.release(level, source);
		} catch (LinkageError e) {
			// As above.
		}
	}

	@Override
	public boolean wardIgnores(Entity entity) {
		try {
			return Fabricators.isDeactivated(entity);
		} catch (LinkageError e) {
			// An older Reworked Mobs without api/Fabricators: no Fabricator mobs.
			return false;
		}
	}

	@Override
	public boolean pulseAround(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock, float chance, float oreChance,
			@org.jspecify.annotations.Nullable Player player) {
		try {
			return Endermen.pulseAround(level, centre, radius, ticksPerBlock, chance, oreChance, player);
		} catch (LinkageError e) {
			// An older Reworked Mobs without the pulse: nothing happens.
			return false;
		}
	}

	@Override
	public boolean restoreAround(ServerLevel level, BlockPos centre, int radius, int ticksPerBlock) {
		try {
			return Endermen.restoreAround(level, centre, radius, ticksPerBlock);
		} catch (LinkageError e) {
			// An older Reworked Mobs without the ripple: nothing happens (the totem isn't used up).
			return false;
		}
	}

	@Override
	public void setUpEndermen(Supplier<ItemStack> offering, Predicate<Player> kin) {
		try {
			Endermen.setCairnGifts(offering);
			Endermen.setKin(kin);
		} catch (LinkageError e) {
			// As above: an older Reworked Mobs has no cairn gifts.
		}
	}

	@Override
	public double nearestSleeperSqr(Level level, Vec3 centre, double range) {
		return Sleepers.nearestSqr(level, centre, range);
	}
}

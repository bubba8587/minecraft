// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ordinary bells are extra loud to sleepers (owner, specs/undead.md "Warding"). Vanilla's vibration for a ring is a plain
 * block change (16 blocks, like breaking a block), but the bell's sound plays at volume 2, which players hear to 32
 * blocks; sleepers hear it as far as players do. Called from the bell mixin after every ring (players, projectiles,
 * redstone). Walls and wool count as for any other noise.
 */
public final class SleeperBells {
	private SleeperBells() {
	}

	/** Points a bell's ring is worth {@code distance} blocks away: {@link UndeadTuning#BELL_POINTS}, falling off linearly. */
	public static float loudness(double distance) {
		return (float) (UndeadTuning.BELL_POINTS * Math.max(0.0, 1.0 - distance / UndeadTuning.BELL_RANGE));
	}

	public static void rang(ServerLevel level, BlockPos bell, @Nullable Entity ringer) {
		if (ringer != null && (ringer.isSpectator() || ringer.dampensVibrations())) {
			return;
		}
		Vec3 from = Vec3.atCenterOf(bell);
		for (SleeperBlockEntity sleeper : SleeperFinder.within(level, from, UndeadTuning.BELL_RANGE)) {
			if (!sleeper.isHearing()) {
				continue;
			}
			double distance = from.distanceTo(Vec3.atCenterOf(sleeper.getBlockPos()));
			float points = loudness(distance);
			if (points > 0.0F) {
				sleeper.getListener().hear(level, from, points * sleeper.effectiveSensitivity(level), ringer, distance, false);
			}
		}
	}
}

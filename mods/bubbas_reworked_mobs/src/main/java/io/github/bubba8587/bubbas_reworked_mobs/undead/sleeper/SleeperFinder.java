// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.world.LoadedBlockEntities;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Sleepers near a point, in loaded chunks. */
public final class SleeperFinder {
	private SleeperFinder() {
	}

	/** Every sleeper whose block centre is within {@code range} of {@code centre}. */
	public static List<SleeperBlockEntity> within(Level level, Vec3 centre, double range) {
		return LoadedBlockEntities.near(level, SleeperBlockEntity.class, centre, range);
	}
}

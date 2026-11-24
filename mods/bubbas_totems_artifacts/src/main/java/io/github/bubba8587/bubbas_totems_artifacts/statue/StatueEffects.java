// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import io.github.bubba8587.bubbas_totems_artifacts.gathering.GatheringRing;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemAreas;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import io.github.bubba8587.bubbas_totems_artifacts.warding.Wards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/** Statues give their totem's area effect in a tiny radius. */
public final class StatueEffects {
	/** Area source key of a statue. */
	private record Source(BlockPos pos) {
	}

	private StatueEffects() {
	}

	/** Whether this statue type has an area effect. */
	public static boolean pulses(TotemType type) {
		return switch (type) {
			case WARDING, GATHERING, FORTIFICATION, GROWTH, HERDING, CLEAR_SKIES, NORTH_WIND, BEGINNING -> true;
			default -> false;
		};
	}

	/** The statue's area. */
	public static AABB area(BlockPos pos) {
		return new AABB(pos).inflate(TotemsTuning.STATUE_RADIUS);
	}

	/** The statue's area for its type. */
	public static AABB area(BlockPos pos, TotemType type) {
		return area(pos);
	}

	/** One pulse of the statue at pos. */
	public static void pulse(ServerLevel level, BlockPos pos, TotemType type) {
		AABB area = area(pos, type);
		Source source = new Source(pos.immutable());
		int life = TotemsTuning.STATUE_PULSE_TICKS + 20;
		switch (type) {
			case WARDING -> Wards.put(level, source, area, true, life);
			case GATHERING -> GatheringRing.put(level, pos, area, life,
					TotemsTuning.STATUE_RING_RADIUS, TotemsTuning.STATUE_RING_HEIGHT);
			case FORTIFICATION -> TotemAreas.put(level, TotemAreas.Kind.FORTIFICATION, source, area, 1, life);
			case GROWTH -> TotemAreas.put(level, TotemAreas.Kind.GROWTH, source, area, TotemsTuning.GROWTH_STATUE_EXTRA, life);
			case HERDING -> TotemAreas.put(level, TotemAreas.Kind.HERDING, source, area, 1, life);
			case CLEAR_SKIES -> TotemAreas.put(level, TotemAreas.Kind.CLEAR_SKIES, source, area, 1, life);
			case NORTH_WIND -> TotemAreas.put(level, TotemAreas.Kind.NORTH_WIND, source, area, 1, life);
			case BEGINNING -> {
				if (io.github.bubba8587.bubbas_totems_artifacts.beginning.BeginningAreas.due(level, TotemsTuning.STATUE_PULSE_TICKS)) {
					io.github.bubba8587.bubbas_totems_artifacts.beginning.BeginningAreas.pulse(level, pos, TotemsTuning.STATUE_RADIUS);
				}
			}
			default -> {
			}
		}
	}
}

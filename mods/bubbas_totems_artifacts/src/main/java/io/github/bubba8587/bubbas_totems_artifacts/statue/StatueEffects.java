// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import io.github.bubba8587.bubbas_totems_artifacts.gathering.Gathering;
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
			case WARDING, GATHERING, FORTIFICATION, GROWTH, HERDING, CLEAR_SKIES -> true;
			default -> false;
		};
	}

	/** The statue's area. */
	public static AABB area(BlockPos pos) {
		return new AABB(pos).inflate(TotemsTuning.STATUE_RADIUS);
	}

	/** The statue's area for its type. */
	public static AABB area(BlockPos pos, TotemType type) {
		return type == TotemType.WARDING ? new AABB(pos).inflate(TotemsTuning.WARD_STATUE_RADIUS) : area(pos);
	}

	/** One pulse of the statue at pos. */
	public static void pulse(ServerLevel level, BlockPos pos, TotemType type) {
		AABB area = area(pos, type);
		Source source = new Source(pos.immutable());
		int life = TotemsTuning.STATUE_PULSE_TICKS + 20;
		switch (type) {
			case WARDING -> Wards.put(level, source, area, true, life);
			case GATHERING -> Gathering.intoChest(level, pos);
			case FORTIFICATION -> TotemAreas.put(level, TotemAreas.Kind.FORTIFICATION, source, area, 1, life);
			case GROWTH -> TotemAreas.put(level, TotemAreas.Kind.GROWTH, source, area, TotemsTuning.GROWTH_STATUE_EXTRA, life);
			case HERDING -> TotemAreas.put(level, TotemAreas.Kind.HERDING, source, area, 1, life);
			case CLEAR_SKIES -> TotemAreas.put(level, TotemAreas.Kind.CLEAR_SKIES, source, area, 1, life);
			default -> {
			}
		}
	}
}

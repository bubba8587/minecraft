// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

/**
 * How loud a noise is to a sleeper (specs/undead.md "Hearing and waking"): points by event, built on vanilla's
 * vibration frequency table (the one sculk sensors and the warden use), times a steep distance falloff and a
 * penalty per solid block in between. Pure functions; the meter itself lives on the block entity.
 */
public final class SleeperHearing {
	private SleeperHearing() {
	}

	static boolean isStep(Holder<GameEvent> event) {
		return event.is(GameEvent.STEP) || event.is(GameEvent.SWIM) || event.is(GameEvent.FLAP);
	}

	/** Points one event adds at full range, before falloff and sensitivity. */
	public static float points(Holder<GameEvent> event, boolean sprinting) {
		if (event.is(ModGameEvents.AWAKEN)) {
			return UndeadTuning.AWAKEN_POINTS;
		}
		if (event.is(GameEvent.EXPLODE) || event.is(GameEvent.LIGHTNING_STRIKE)) {
			return UndeadTuning.DEAFENING_POINTS;
		}
		if (isStep(event)) {
			return UndeadTuning.STEP_POINTS * (sprinting ? UndeadTuning.SPRINT_STEP_FACTOR : 1.0F);
		}
		// Anything in #vibrations without a vanilla frequency (other mods' events) counts as the quietest non-step.
		int frequency = Math.max(2, VibrationSystem.getGameEventFrequency(event));
		return UndeadTuning.EVENT_BASE_POINTS + UndeadTuning.EVENT_POINTS_PER_FREQUENCY * frequency;
	}

	/** 1 within {@link UndeadTuning#NOISE_FULL_RANGE}, then the square of the distance left, 0 at {@link UndeadTuning#NOISE_SILENT_RANGE}. */
	public static float falloff(double distance) {
		double left = (UndeadTuning.NOISE_SILENT_RANGE - distance) / (UndeadTuning.NOISE_SILENT_RANGE - UndeadTuning.NOISE_FULL_RANGE);
		double clamped = Math.clamp(left, 0.0, 1.0);
		return (float) (clamped * clamped);
	}

	/** Points at this distance (no wall penalty). */
	public static float loudness(Holder<GameEvent> event, double distance, boolean sprinting) {
		return points(event, sprinting) * falloff(distance);
	}

	/**
	 * {@link UndeadTuning#SOLID_BLOCK_FACTOR} per opaque full block the straight line passes through, not counting
	 * the blocks at either end (a placed block, the sleeper itself). Vanilla vibrations ignore walls except wool;
	 * this keeps noise in the next hallway from carrying. At most 3 are counted (the noise is gone by then).
	 */
	public static float wallFactor(BlockGetter level, Vec3 from, Vec3 to) {
		// Block centre to block centre, like vanilla's occlusion rays: a footstep's position is the top face of the
		// floor, and a line from there would run through the floor itself.
		BlockPos start = BlockPos.containing(from);
		BlockPos end = BlockPos.containing(to);
		int[] solid = {0};
		BlockGetter.traverseBlocks(Vec3.atCenterOf(start), Vec3.atCenterOf(end), level, (getter, pos) -> {
			if (!pos.equals(start) && !pos.equals(end) && getter.getBlockState(pos).isSolidRender()) {
				solid[0]++;
			}
			return solid[0] >= 3 ? Boolean.TRUE : null;
		}, getter -> Boolean.FALSE);
		return (float) Math.pow(UndeadTuning.SOLID_BLOCK_FACTOR, solid[0]);
	}
}

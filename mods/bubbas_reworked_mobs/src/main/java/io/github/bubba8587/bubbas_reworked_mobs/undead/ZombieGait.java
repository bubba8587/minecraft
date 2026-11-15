// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead;

import net.minecraft.util.Mth;

/**
 * The woken zombie's two-phase shamble (specs/undead.md "Tuning", walk animations), shared by the server (the real
 * movement pulses) and the client (the animation), which compute the same phase from the game time and the entity id
 * without any extra packets.
 * <ul>
 * <li>DRAG (the first {@link UndeadTuning#GAIT_DRAG_FRACTION} of the cycle): the body creeps on a little slower
 * than average ({@link UndeadTuning#GAIT_DRAG_SPEED}) while the bad leg is dragged up.</li>
 * <li>STEP, a fall onto the good leg (the rest, called the lurch in code): a short, modest burst, {@code DRAG_SPEED + k sin^2}, peaking in its middle, with {@code k}
 * chosen so the average over the whole cycle is exactly the normal speed (escape difficulty unchanged).</li>
 * </ul>
 */
public final class ZombieGait {
	private static final float K = 2.0F * (1.0F - UndeadTuning.GAIT_DRAG_SPEED) / (1.0F - UndeadTuning.GAIT_DRAG_FRACTION);

	private ZombieGait() {
	}

	/** Where in its cycle (0 to 1) the zombie with this network id is at this game time (plus a partial tick). */
	public static float cycle(int entityId, long gameTime, float partialTick) {
		int period = UndeadTuning.GAIT_PERIOD_TICKS;
		// Each zombie on its own offset, so a horde doesn't lurch in step. Same id on both sides.
		int offset = Math.floorMod(Mth.murmurHash3Mixer(entityId), period);
		long tick = Math.floorMod(gameTime + offset, (long) period);
		return (tick + partialTick) / period;
	}

	/** 0 to 1 through the lurch, or a negative number during the drag. */
	public static float lurch(float cycle) {
		float drag = UndeadTuning.GAIT_DRAG_FRACTION;
		return cycle < drag ? -1.0F : (cycle - drag) / (1.0F - drag);
	}

	/** The body's speed at this point of the cycle, as a fraction of the average speed (averages 1 over a cycle). */
	public static float velocityFactor(float cycle) {
		float lurch = lurch(cycle);
		if (lurch < 0.0F) {
			return UndeadTuning.GAIT_DRAG_SPEED;
		}
		float s = Mth.sin(lurch * Mth.PI);
		return UndeadTuning.GAIT_DRAG_SPEED + K * s * s;
	}

	/**
	 * How far through its cycle's distance the body is (0 to 1): the integral of {@link #velocityFactor}. The client
	 * moves planted feet by this, so they stay put while the body goes over them.
	 */
	public static float distance(float cycle) {
		float drag = UndeadTuning.GAIT_DRAG_FRACTION;
		float d = UndeadTuning.GAIT_DRAG_SPEED;
		float lurch = lurch(cycle);
		if (lurch < 0.0F) {
			return d * cycle;
		}
		// integral of sin^2(pi l) dl = l/2 - sin(2 pi l) / (4 pi)
		float bump = K * (1.0F - drag) * (lurch / 2.0F - Mth.sin(2.0F * Mth.PI * lurch) / (4.0F * Mth.PI));
		return d * cycle + bump;
	}

	/**
	 * Multiplier for the speed a mob's move control sets. Mobs accelerate by speed squared (the speed and the forward
	 * input both carry it), so the square root gives {@link #velocityFactor} in actual motion.
	 */
	public static float moveSpeedFactor(int entityId, long gameTime) {
		return (float) Math.sqrt(velocityFactor(cycle(entityId, gameTime, 0.0F)));
	}
}

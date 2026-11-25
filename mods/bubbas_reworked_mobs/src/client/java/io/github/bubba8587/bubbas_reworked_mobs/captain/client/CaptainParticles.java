// dte:C1,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Wither Captain summoning particles: soul fire flames spiralling up round the captain, the odd soul. */
public final class CaptainParticles {
	private static final int FLAMES_PER_TICK = 2;
	private static final float SOUL_CHANCE = 0.15F;
	private static final double RADIUS = 0.75;
	private static final double SPIN = 0.35;
	private static final double RISE = 0.045;

	private CaptainParticles() {
	}

	public static void summoningTick(ClientLevel level, double x, double y, double z, float height, RandomSource random) {
		long time = level.getGameTime();
		// flames: two spiral arms rising round the body
		for (int i = 0; i < FLAMES_PER_TICK; i++) {
			double angle = time * SPIN + i * Math.PI + random.nextDouble() * 0.6;
			double r = RADIUS * (0.8 + random.nextDouble() * 0.4);
			double h = random.nextDouble() * height * 0.9;
			double px = x + Math.cos(angle) * r;
			double pz = z + Math.sin(angle) * r;
			// tangential drift, so the flames swirl
			double vx = -Math.sin(angle) * 0.03;
			double vz = Math.cos(angle) * 0.03;
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, y + h, pz, vx, RISE + random.nextDouble() * 0.02, vz);
		}
		// souls: rarely, drifting up from the feet
		if (random.nextFloat() < SOUL_CHANCE) {
			double angle = random.nextDouble() * Mth.TWO_PI;
			level.addParticle(ParticleTypes.SOUL,
				x + Math.cos(angle) * RADIUS * 0.6, y + 0.1, z + Math.sin(angle) * RADIUS * 0.6,
				0.0, 0.05 + random.nextDouble() * 0.03, 0.0);
		}
	}
}

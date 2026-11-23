// dte:C8,B17
package io.github.bubba8587.bubbas_world_tweaks.lava;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** What an erupting lava geyser looks and sounds like (client side, from tick). */
public final class LavaGeyserEffects {
	private LavaGeyserEffects() {
	}

	public static void tick(LavaGeyser geyser) {
		Level level = geyser.level();
		double x = geyser.getX();
		double y = geyser.getY();
		double z = geyser.getZ();
		int age = geyser.tickCount;
		if (age % 2 == 0) {
			for (int i = 0; i < 3; i++) {
				level.addParticle(ModParticles.LAVA_GEYSER_PLUME, x, y, z, 0.0, 0.0, 0.0);
			}
		}
		if (age % 3 == 0) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, x + (level.getRandom().nextDouble() - 0.5) * 0.6, y + 0.3,
					z + (level.getRandom().nextDouble() - 0.5) * 0.6, 0.0, 0.25, 0.0);
		}
		if (age % 7 == 0) {
			level.addParticle(ParticleTypes.LAVA, x, y + 0.2, z, 0.0, 0.0, 0.0);
		}
		if (age % 40 == 1) {
			level.playLocalSound(x, y, z, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.6F, false);
		}
	}
}

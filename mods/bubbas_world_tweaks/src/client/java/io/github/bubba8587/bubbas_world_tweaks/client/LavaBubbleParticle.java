// dte:C8,B17
package io.github.bubba8587.bubbas_world_tweaks.client;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * A lava bubble (specs/world-tweaks.md): a half-dome of lava swelling on the surface for about a second, then bursting
 * with a goopy plop (vanilla's lava pop, pitched down). Pixel-art frames in vanilla lava's palette
 * (textures/particle/lava_bubble_0..6: 0-4 swell, 5-6 burst), lit like lava (full bright). A round blister facing the
 * camera, centred on the surface: the lava hides its lower half from the side (a half-dome), and from above it's whole.
 */
public class LavaBubbleParticle extends SingleQuadParticle {
	private final SpriteSet sprites;
	private final int swell;
	private boolean popped;

	LavaBubbleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, RandomSource random) {
		super(level, x, y, z, sprites.get(0, 6));
		this.sprites = sprites;
		this.swell = WorldTweaksTuning.BUBBLE_SWELL_TICKS + random.nextInt(WorldTweaksTuning.BUBBLE_LIFE_SPREAD + 1);
		this.lifetime = swell + WorldTweaksTuning.BUBBLE_POP_TICKS;
		this.quadSize = 0.22F + random.nextFloat() * 0.12F;
		this.gravity = 0.0F;
		this.hasPhysics = false;
		this.xd = 0.0;
		this.yd = 0.0;
		this.zd = 0.0;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			this.remove();
			return;
		}
		if (age < swell) {
			setSprite(sprites.get(Math.min(4, age * 5 / swell), 6));
		} else {
			if (!popped) {
				popped = true;
				level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.AMBIENT, WorldTweaksTuning.PLOP_VOLUME,
						WorldTweaksTuning.PLOP_PITCH + level.getRandom().nextFloat() * 0.1F, false);
			}
			setSprite(sprites.get(age - swell < 2 ? 5 : 6, 6));
		}
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return 0xF000F0;
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.OPAQUE;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public @Nullable Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xa, double ya,
				double za, RandomSource random) {
			return new LavaBubbleParticle(level, x, y, z, sprites, random);
		}
	}
}

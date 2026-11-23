// dte:C8,B17
package io.github.bubba8587.bubbas_world_tweaks.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/** A lava geyser's black smoke. */
public class LavaGeyserPlumeParticle extends SingleQuadParticle {
	private static final int PLUME_HEIGHT = 10;
	private final SpriteSet sprites;
	private final double startY;
	private final double maxY;
	private final float initialPropulsion;
	private final float sprayX;
	private final float sprayZ;
	private final float minSize;
	private final float maxSize;
	private boolean done;

	LavaGeyserPlumeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, RandomSource random) {
		super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
		this.sprites = sprites;
		this.hasPhysics = true;
		this.speedUpWhenYMotionIsBlocked = true;
		this.lifetime = PLUME_HEIGHT * 5;
		this.yd = 0.0;
		this.startY = y;
		this.maxY = y + PLUME_HEIGHT - 1.0;
		this.sprayX = (random.nextFloat() - 0.5F) * 0.3F;
		this.sprayZ = (random.nextFloat() - 0.5F) * 0.3F;
		this.friction = 1.0F;
		this.initialPropulsion = PLUME_HEIGHT * 1.45F;
		this.gravity = -initialPropulsion;
		float base = this.quadSize * 0.75F;
		this.minSize = base * (2.0F + PLUME_HEIGHT / 8.0F);
		this.maxSize = base * (3.5F + PLUME_HEIGHT / 8.0F);
		this.quadSize = minSize;
		float shade = 0.85F + random.nextFloat() * 0.15F;
		this.rCol = shade;
		this.gCol = shade;
		this.bCol = shade;
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (!done && (yd < 0.0 || y > maxY || y == yo)) {
			lifetime = Math.min(lifetime, age + 5);
			friction = 0.0F;
			done = true;
		}
		double linear = Math.clamp((y - startY) / (maxY - startY), 0.0, 1.0);
		gravity = initialPropulsion * (float) Math.pow(linear, 3.0) * 0.12F;
		xd = linear * sprayX;
		zd = linear * sprayZ;
		setSpriteFromAge(sprites);
		quadSize = minSize + (float) (linear * (maxSize - minSize));
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
			return new LavaGeyserPlumeParticle(level, x, y, z, sprites, random);
		}
	}
}

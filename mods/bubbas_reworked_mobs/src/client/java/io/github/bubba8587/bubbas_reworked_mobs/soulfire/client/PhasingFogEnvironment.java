// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.soulfire.client;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Phasing blindness: a soul-dark fog from the Phasing effect itself, ahead of every other fog. */
public class PhasingFogEnvironment extends FogEnvironment {
	/** Fog colour: soul-dark teal, no red (so Night Vision can't brighten it). */
	private static final Vector3fc COLOR = new Vector3f(0.0F, 0.045F, 0.05F);
	/** Fog end (blocks): Blindness's. */
	public static final float DISTANCE = 5.0F;

	@Override
	public boolean isApplicable(@Nullable FogType fogType, Entity entity) {
		return entity instanceof LivingEntity living && living.hasEffect(Phasing.PHASING);
	}

	@Override
	public Vector3fc getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks) {
		return COLOR;
	}

	@Override
	public void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
		if (!(camera.entity() instanceof LivingEntity living)) {
			return;
		}
		MobEffectInstance phasing = living.getEffect(Phasing.PHASING);
		if (phasing == null) {
			return;
		}
		float distance = phasing.isInfiniteDuration() ? DISTANCE : Mth.lerp(Math.min(1.0F, phasing.getDuration() / 20.0F), renderDistance, DISTANCE);
		fog.environmentalStart = distance * 0.25F;
		fog.environmentalEnd = distance;
		fog.skyEnd = distance * 0.8F;
		fog.cloudEnd = distance * 0.8F;
	}
}

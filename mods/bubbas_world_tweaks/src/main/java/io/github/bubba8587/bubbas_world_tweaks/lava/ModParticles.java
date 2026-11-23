// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.lava;

import io.github.bubba8587.bubbas_world_tweaks.BubbasWorldTweaks;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

/** The mod's particle types (drawn by client/LavaBubbleParticle and client/LavaGeyserPlumeParticle). */
public final class ModParticles {
	/** A lava bubble swelling on the surface over magma, then popping. */
	public static final SimpleParticleType LAVA_BUBBLE = register("lava_bubble");
	/** A lava geyser's black smoke plume (vanilla's sulfur geyser plume, darkened). */
	public static final SimpleParticleType LAVA_GEYSER_PLUME = register("lava_geyser_plume");

	private ModParticles() {
	}

	private static SimpleParticleType register(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, BubbasWorldTweaks.id(name), FabricParticleTypes.simple());
	}

	public static void initialize() {
	}
}

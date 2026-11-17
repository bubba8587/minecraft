// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.registry;

import com.mojang.serialization.Codec;
import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/** Item data components (saved with the stack and sent to clients). */
public final class ModComponents {
	/** Totem of Seeking: which structure type it seeks (an id from seeking/SeekingTypes). */
	public static final DataComponentType<Identifier> SEEKING_TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("seeking_type"),
			DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

	/**
	 * On a vanilla compass pointed by a Seeking beacon: the game time it stops pointing (then the compass is plain again;
	 * mixin/CompassItemMixin).
	 */
	public static final DataComponentType<Long> SEEKING_BEACON_UNTIL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("seeking_beacon_until"),
			DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

	private ModComponents() {
	}

	public static void initialize() {
	}
}

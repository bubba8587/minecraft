// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.registry;

import java.util.List;

import com.mojang.serialization.Codec;
import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.TotemOfSeekingItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/** Item data components (saved with the stack and sent to clients). */
public final class ModComponents {
	/** Totem of Seeking: its current target. */
	public static final DataComponentType<TotemOfSeekingItem.Target> SEEKING_TARGET = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("seeking_target"),
			DataComponentType.<TotemOfSeekingItem.Target>builder().persistent(TotemOfSeekingItem.Target.CODEC).networkSynchronized(TotemOfSeekingItem.Target.STREAM_CODEC).build());

	/** Totem of Seeking: its recorded finds, oldest first. */
	public static final DataComponentType<List<TotemOfSeekingItem.Found>> SEEKING_FOUND = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("seeking_found"),
			DataComponentType.<List<TotemOfSeekingItem.Found>>builder().persistent(TotemOfSeekingItem.Found.LIST_CODEC)
					.networkSynchronized(TotemOfSeekingItem.Found.LIST_STREAM_CODEC).build());

	/** Old saves: the structure type a Totem of Seeking once chose (unused). */
	public static final DataComponentType<Identifier> SEEKING_TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("seeking_type"),
			DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

	/** Old saves: on a compass once pointed by a Seeking beacon (unused). */
	public static final DataComponentType<Long> SEEKING_BEACON_UNTIL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			BubbasTotemsArtifacts.id("seeking_beacon_until"),
			DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

	private ModComponents() {
	}

	public static void initialize() {
	}
}

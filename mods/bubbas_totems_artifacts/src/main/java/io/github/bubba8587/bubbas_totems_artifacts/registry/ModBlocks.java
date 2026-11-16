// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.registry;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemStatueBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Registered blocks. One only: the totem statue, an owner-approved B15 exception (2026-11-16: one shared block for
 * every totem, set down as decoration). It has no item of its own; the totems place it.
 */
public final class ModBlocks {
	private static final ResourceKey<Block> STATUE_KEY = ResourceKey.create(Registries.BLOCK, BubbasTotemsArtifacts.id("totem_statue"));
	public static final TotemStatueBlock TOTEM_STATUE = Registry.register(BuiltInRegistries.BLOCK, STATUE_KEY,
			new TotemStatueBlock(BlockBehaviour.Properties.of().setId(STATUE_KEY).mapColor(MapColor.STONE).sound(SoundType.STONE)
					.strength(1.0F).noOcclusion().pushReaction(PushReaction.POPPED)));

	private ModBlocks() {
	}

	public static void initialize() {
	}
}

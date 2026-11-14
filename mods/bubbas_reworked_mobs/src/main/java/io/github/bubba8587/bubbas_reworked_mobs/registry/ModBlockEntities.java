// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class ModBlockEntities {
	/** One block entity type shared by every sleeper block. */
	public static final BlockEntityType<SleeperBlockEntity> SLEEPER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			BubbasReworkedMobs.id("sleeper"),
			FabricBlockEntityTypeBuilder.create(SleeperBlockEntity::new, ModBlocks.SLEEPERS.values().toArray(Block[]::new)).build());

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}

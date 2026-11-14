// dte:A2,A4
package io.github.bubba8587.refmod.registry;

import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.block.CounterBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class ModBlockEntities {
	public static final BlockEntityType<CounterBlockEntity> COUNTER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			RefMod.id("counter_block"),
			FabricBlockEntityTypeBuilder.create(CounterBlockEntity::new, ModBlocks.COUNTER_BLOCK).build());

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}

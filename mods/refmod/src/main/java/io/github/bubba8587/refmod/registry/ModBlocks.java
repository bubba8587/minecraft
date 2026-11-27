// dte:A2,A4
package io.github.bubba8587.refmod.registry;

import java.util.function.Function;

import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.block.CounterBlock;
import io.github.bubba8587.refmod.block.SignalLampBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.sounds.BlockSoundSets;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	public static final Block SIGNAL_LAMP = register("signal_lamp", SignalLampBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3F).sound(BlockSoundSets.GLASS)
					.lightLevel(state -> state.getValue(SignalLampBlock.LIT) ? 15 : 0));
	public static final Block COUNTER_BLOCK = register("counter_block", CounterBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(BlockSoundSets.STONE));

	private ModBlocks() {
	}

	/** Registers a block and its block item; both get {@code .setId(key)} before construction. */
	private static Block register(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, RefMod.id(path));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, RefMod.id(path));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(props.setId(blockKey)));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
		return block;
	}

	public static void initialize() {
	}
}

// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The block id that {@code Blocks.register} sets with {@code setId} before the block is constructed. */
@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor {
	@Accessor("id")
	@Nullable ResourceKey<Block> bubbas_reworked_mining$id();
}

// dte:C3,B15
package io.github.bubba8587.bubbas_extra_recipes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_extra_recipes.blockstate.SyntheticOre;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Adds SYNTHETIC to chosen vanilla blocks. */
@Mixin(Block.class)
public abstract class BlockExtraPropertyMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"))
	private void bubbas_extra_recipes$addSyntheticProperty(Block block, StateDefinition.Builder<Block, BlockState> builder, Operation<Void> original,
			@Local(argsOnly = true) BlockBehaviour.Properties properties) {
		original.call(block, builder);
		ResourceKey<Block> id = ((BlockPropertiesAccessor) properties).bubbas_extra_recipes$id();
		if (id != null && SyntheticOre.BLOCK_IDS.contains(id.identifier().toString())) {
			builder.add(SyntheticOre.SYNTHETIC);
		}
	}

	/** Keeps synthetic=false as the default state. */
	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState bubbas_extra_recipes$naturalByDefault(BlockState state) {
		return state.hasProperty(SyntheticOre.SYNTHETIC) ? state.setValue(SyntheticOre.SYNTHETIC, false) : state;
	}
}

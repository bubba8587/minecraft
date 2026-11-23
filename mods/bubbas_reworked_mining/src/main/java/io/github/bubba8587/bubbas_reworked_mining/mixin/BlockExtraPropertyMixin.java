// dte:C7,B15
package io.github.bubba8587.bubbas_reworked_mining.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mining.natural.NaturalStone;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Adds {@link NaturalStone#PLACED} to stone and deepslate (same technique as refmod's synthetic ore). */
@Mixin(Block.class)
public abstract class BlockExtraPropertyMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"))
	private void bubbas_reworked_mining$addPlacedProperty(Block block, StateDefinition.Builder<Block, BlockState> builder, Operation<Void> original,
			@Local(argsOnly = true) BlockBehaviour.Properties properties) {
		original.call(block, builder);
		ResourceKey<Block> id = ((BlockPropertiesAccessor) properties).bubbas_reworked_mining$id();
		if (id != null && NaturalStone.marks(id.identifier().toString())) {
			builder.add(NaturalStone.PLACED);
		}
	}

	/** BooleanProperty lists {@code true} first, so without this the default state would count as placed. */
	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState bubbas_reworked_mining$naturalByDefault(BlockState state) {
		return state.hasProperty(NaturalStone.PLACED) ? state.setValue(NaturalStone.PLACED, false) : state;
	}
}

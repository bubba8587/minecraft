// dte:C5,B15
package io.github.bubba8587.bubbas_reworked_beacons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.HangingBeacon;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds UPSIDE_DOWN to the vanilla beacon while it is constructed. */
@Mixin(Block.class)
public abstract class BlockExtraPropertyMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"))
	private void bubbas_reworked_beacons$addUpsideDown(Block block, StateDefinition.Builder<Block, BlockState> builder, Operation<Void> original,
			@Local(argsOnly = true) BlockBehaviour.Properties properties) {
		original.call(block, builder);
		ResourceKey<Block> id = ((BeaconBlockPropertiesAccessor) properties).bubbas_reworked_beacons$id();
		if (id != null && HangingBeacon.BLOCK_ID.equals(id.identifier().toString())) {
			builder.add(HangingBeacon.UPSIDE_DOWN);
		}
	}

	/** Keeps upside_down=false as the default state. */
	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState bubbas_reworked_beacons$uprightByDefault(BlockState state) {
		return state.hasProperty(HangingBeacon.UPSIDE_DOWN) ? state.setValue(HangingBeacon.UPSIDE_DOWN, false) : state;
	}

	/** Hangs a beacon placed against the underside of a block. */
	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void bubbas_reworked_beacons$hangFromCeiling(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		BlockState state = cir.getReturnValue();
		if (state != null && state.hasProperty(HangingBeacon.UPSIDE_DOWN) && context.getClickedFace() == Direction.DOWN) {
			cir.setReturnValue(state.setValue(HangingBeacon.UPSIDE_DOWN, true));
		}
	}
}

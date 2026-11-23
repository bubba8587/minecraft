// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_reworked_mining.natural.NaturalStone;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Stone or deepslate placed from an item is marked placed: it never gives rare finds. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@ModifyReturnValue(method = "getPlacementState", at = @At("RETURN"))
	private @Nullable BlockState bubbas_reworked_mining$markPlaced(@Nullable BlockState state) {
		return state == null ? null : NaturalStone.placed(state);
	}
}

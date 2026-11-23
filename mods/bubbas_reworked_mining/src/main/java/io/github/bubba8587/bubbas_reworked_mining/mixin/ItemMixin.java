// dte:C7
package io.github.bubba8587.bubbas_reworked_mining.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mining.curve.MiningCurve;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The mining curve's wear. */
@Mixin(Item.class)
public abstract class ItemMixin {
	@ModifyExpressionValue(method = "mineBlock", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/component/Tool;damagePerBlock()I", ordinal = 1))
	private int bubbas_reworked_mining$wear(int vanilla, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) BlockState state) {
		return MiningCurve.wear(stack, state, vanilla);
	}
}

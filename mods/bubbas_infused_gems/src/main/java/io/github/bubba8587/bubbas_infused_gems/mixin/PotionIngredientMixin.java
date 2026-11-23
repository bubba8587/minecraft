// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.registry.ModItems;
import io.github.bubba8587.bubbas_infused_gems.station.GemInversion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A finished gem goes in a brewing stand's bottle slot only if it can be inverted. */
@Mixin(PotionIngredient.class)
public abstract class PotionIngredientMixin {
	@ModifyReturnValue(method = "isPotionInput", at = @At("RETURN"))
	private static boolean bubbas$onlyInvertibleGems(boolean input, ItemStack stack, RecipeAccess access) {
		return input && (!stack.is(ModItems.GEM) || GemInversion.canInvert(stack));
	}
}

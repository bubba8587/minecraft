// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Grindstone: gear with gems in it gives the same gear without them. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@ModifyReturnValue(method = "computeResult", at = @At("RETURN"))
	private ItemStack bubbas$removeGems(ItemStack result, ItemStack input, ItemStack additional) {
		if (!Sockets.hasGems(input) && !Sockets.hasGems(additional)) {
			return result;
		}
		if (!result.isEmpty()) {
			return Sockets.withoutGems(result);
		}
		boolean one = input.isEmpty() != additional.isEmpty();
		ItemStack item = input.isEmpty() ? additional : input;
		return one && item.getCount() == 1 ? Sockets.withoutGems(item) : result;
	}
}

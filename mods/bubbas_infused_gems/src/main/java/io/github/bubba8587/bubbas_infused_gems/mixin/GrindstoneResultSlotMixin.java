// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.station.Sockets;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The grindstone result slot: taking it gives back every gem that was in the inputs. */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
	@Shadow
	@Final
	GrindstoneMenu this$0;

	@Inject(method = "onTake", at = @At("HEAD"))
	private void bubbas$returnGems(Player player, ItemStack carried, CallbackInfo ci) {
		if (player.level().isClientSide()) {
			return;
		}
		Container inputs = ((GrindstoneMenuAccessor) this.this$0).bubbas$repairSlots();
		List<ItemStack> gems = new ArrayList<>();
		gems.addAll(Sockets.gemItems(inputs.getItem(0)));
		gems.addAll(Sockets.gemItems(inputs.getItem(1)));
		Sockets.give(player, gems);
	}
}

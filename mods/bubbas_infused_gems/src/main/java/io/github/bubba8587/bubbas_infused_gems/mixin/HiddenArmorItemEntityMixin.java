// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.effect.HiddenArmor;
import net.minecraft.world.entity.item.ItemEntity;

/** Perfect Invisibility hidden armor: a dropped piece (death, thrown) shows again on the ground. */
@Mixin(ItemEntity.class)
abstract class HiddenArmorItemEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void bubbas$restore(CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (!self.level().isClientSide() && HiddenArmor.isHidden(self.getItem())) {
			self.setItem(HiddenArmor.restore(self.getItem().copy()));
		}
	}
}

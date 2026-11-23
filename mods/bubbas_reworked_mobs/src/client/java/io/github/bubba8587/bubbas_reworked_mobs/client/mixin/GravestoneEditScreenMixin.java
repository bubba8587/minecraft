// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.client.grave.GravestoneEditScreen;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GravestoneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gravestones open their own edit screen instead of vanilla's sign screen. */
@Mixin(LocalPlayer.class)
abstract class GravestoneEditScreenMixin {
	@Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
	private void brm$gravestoneEditScreen(SignBlockEntity sign, SignTextSlot slot, CallbackInfo ci) {
		if (sign instanceof GravestoneBlockEntity) {
			Minecraft mc = Minecraft.getInstance();
			mc.gui.setScreen(new GravestoneEditScreen(sign, slot, mc.isTextFilteringEnabled()));
			ci.cancel();
		}
	}
}

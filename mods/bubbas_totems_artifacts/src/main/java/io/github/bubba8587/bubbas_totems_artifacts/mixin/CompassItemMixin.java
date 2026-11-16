// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModComponents;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.SeekingBeacon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A compass pointed by a Seeking beacon (seeking/SeekingBeacon) turns plain again once the beacon stops reaching it,
 * and keeps the name "Compass" meanwhile (vanilla would call any compass with a lodestone target "Lodestone Compass").
 */
@Mixin(CompassItem.class)
abstract class CompassItemMixin {
	@Inject(method = "inventoryTick", at = @At("TAIL"))
	private void bta$lapseSeeking(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot, CallbackInfo ci) {
		SeekingBeacon.lapse(stack, level);
	}

	@Inject(method = "getName", at = @At("HEAD"), cancellable = true)
	private void bta$plainName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
		if (stack.has(ModComponents.SEEKING_BEACON_UNTIL)) {
			cir.setReturnValue(Component.translatable("item.minecraft.compass"));
		}
	}
}

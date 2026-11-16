// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import io.github.bubba8587.bubbas_totems_artifacts.keeping.Keeping;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Totem of Keeping (keeping/Keeping): a kept death drops nothing, as under vanilla's keep_inventory. Both spots are the
 * ones where vanilla reads that rule on death (Player.dropEquipment, Player.getBaseExperienceReward).
 */
@Mixin(Player.class)
abstract class PlayerKeepingMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
	private void bta$keepInventory(ServerLevel level, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && Keeping.tryKeep(level, player)) {
			ci.cancel();
		}
	}

	@Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
	private void bta$keepExperience(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
		if (Keeping.isKept((Player) (Object) this)) {
			cir.setReturnValue(0);
		}
	}
}

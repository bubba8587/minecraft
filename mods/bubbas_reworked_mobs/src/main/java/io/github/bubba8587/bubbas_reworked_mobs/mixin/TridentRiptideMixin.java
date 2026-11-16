// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.drowned.DrownedAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Riptide yanks you free (owner D3): a charged Riptide trident released by someone a drowned holds lets them go first,
 * so vanilla's own Riptide launch (which refuses passengers) then fires. Runs on both sides: the player's client moves
 * the player, so it must see them free too; the server releases the grip (the drowned recoils).
 */
@Mixin(TridentItem.class)
abstract class TridentRiptideMixin {
	@Inject(method = "releaseUsing", at = @At("HEAD"))
	private void brm$riptideFree(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
		if (!(entity instanceof Player player) || !(player.getVehicle() instanceof Drowned drowned)) {
			return;
		}
		int timeHeld = ((TridentItem) (Object) this).getUseDuration(itemStack, entity) - remainingTime;
		if (timeHeld < 10 || !(EnchantmentHelper.getTridentSpinAttackStrength(itemStack, player) > 0.0F) || !player.isInWaterOrRain()) {
			return;
		}
		if (level.isClientSide()) {
			player.stopRiding();
		} else {
			DrownedAccess.mind(drowned).release(drowned, true);
		}
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPosts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Trader posts draw the wandering trader (specs/monster-camps.md "Trader posts"): its place, after vanilla's odds. */
@Mixin(WanderingTraderSpawner.class)
abstract class WanderingTraderSpawnerMixin {
	@Inject(method = "spawn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;find(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;"),
			cancellable = true)
	private void brm$arriveAtTraderPost(ServerLevel level, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) BlockPos playerPos) {
		if (TraderPosts.redirect(level, playerPos) != null) {
			cir.setReturnValue(true);
		}
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.spider.GiantSpider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.stormiespiders.client.ClientEventHandlers;
import win.demistorm.stormiespiders.common.entity.mob.IClimberEntity;

/**
 * Stormie's Spiders (a required dependency) poses every spider it lets climb. A giant spider hauling a catch up a
 * trunk, and waiting or feeding by its cocoon up there, is posed by us instead (rear first, facing down at the catch;
 * upside down under an overhang, sideways on the trunk: owner), so Stormie's render transform is skipped for it then. Everything else, the giant's ordinary climbing included, is left to Stormie's.
 */
@Mixin(value = ClientEventHandlers.class, remap = false)
abstract class StormieClimberRenderMixin {
	@Inject(method = "storeClimberDataForRenderState", at = @At("HEAD"), cancellable = true)
	private static void brm$weposeHauling(LivingEntityRenderState state, LivingEntity entity, IClimberEntity climber, float partialTicks,
			CallbackInfo ci) {
		if (entity instanceof GiantSpider giant && giant.onPerch()) {
			ci.cancel();
		}
	}
}

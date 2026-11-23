// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stormie's Spiders (a required dependency) replaces spider movement (wall and ceiling walking) through an
 * {@code onTravel} hook it merges into vanilla's Spider. An Ancient Fabricator's deactivated spider is limp and must fall
 * onto the pad like any deactivated mob (specs/mob-factory.md), so for it the hook steps aside and vanilla movement
 * runs. Applied after Stormie's (higher priority number), which merged the method.
 */
@Mixin(value = Spider.class, priority = 1500)
abstract class StormieDeactivatedSpiderMixin {
	@Inject(method = "onTravel", at = @At("HEAD"), cancellable = true, remap = false)
	private void brm$limpWhenDeactivated(Vec3 relative, boolean pre, CallbackInfoReturnable<Boolean> cir) {
		if (FabricatorMobs.isDeactivated((Spider) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.bogged.BoggedLurk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the lurking bogged (bogged/BoggedLurk). Neither Bogged nor its parents override Mob.customServerAiStep, so this
 * runs once a server tick for every bogged, after its goals: a lurking bogged's target is cleared before its bow could
 * draw. One instanceof check for every other mob.
 */
@Mixin(Mob.class)
abstract class BoggedLurkMixin {
	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void brm$boggedLurk(ServerLevel level, CallbackInfo ci) {
		if ((Object) this instanceof Bogged bogged) {
			BoggedLurk.tick(level, bogged);
		}
	}
}

// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.phasing.Phasing;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Phasing's Soulfire isn't put out by water, rain or other clears. */
@Mixin(Entity.class)
abstract class PhasingFireMixin {
	@Inject(method = "clearFire", at = @At("HEAD"), cancellable = true)
	private void brm$phasingKeepsFire(CallbackInfo ci) {
		if (Phasing.keepsFire((Entity) (Object) this)) {
			ci.cancel();
		}
	}
}

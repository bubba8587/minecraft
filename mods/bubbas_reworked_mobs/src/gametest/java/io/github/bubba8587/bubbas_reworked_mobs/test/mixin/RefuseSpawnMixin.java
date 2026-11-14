// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-only stand-in for a spawn-control mod: refuses to add any entity named {@link #REFUSE_NAME}. */
@Mixin(ServerLevel.class)
abstract class RefuseSpawnMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void brmTest$refuse(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity.getCustomName() != null && entity.getCustomName().getString().equals(io.github.bubba8587.bubbas_reworked_mobs.test.RefuseSpawnName.VALUE)) {
			cir.setReturnValue(false);
		}
	}
}

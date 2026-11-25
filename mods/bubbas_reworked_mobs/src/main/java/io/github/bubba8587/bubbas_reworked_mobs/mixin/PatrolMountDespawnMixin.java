// dte:B15,B17,B21,C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Camp patrol mounts despawn far away, like their riders. */
@Mixin(Animal.class)
abstract class PatrolMountDespawnMixin {
	@Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
	private void brm$patrolMountsDespawn(double distSqr, CallbackInfoReturnable<Boolean> cir) {
		if (((Animal) (Object) this).entityTags().contains(CampTuning.PATROL_MOUNT_TAG)) {
			cir.setReturnValue(true);
		}
	}
}

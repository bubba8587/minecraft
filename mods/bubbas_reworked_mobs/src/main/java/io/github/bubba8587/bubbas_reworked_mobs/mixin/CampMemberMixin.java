// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Monster camp rosters don't burn by day (camp/CampRoster): a camp holds in daylight. */
@Mixin(Mob.class)
abstract class CampMemberMixin {
	@Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
	private void brm$campMembersDontBurn(CallbackInfo ci) {
		if (((Mob) (Object) this).entityTags().contains(CampTuning.CAMP_TAG)) {
			ci.cancel();
		}
	}
}

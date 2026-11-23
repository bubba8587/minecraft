// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampShade;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camp mobs burn by day like any others, and head for their camp's shade (camp/CampShade). */
@Mixin(Mob.class)
abstract class CampMemberMixin {
	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void brm$campMembersSeekShade(ServerLevel level, CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (mob.entityTags().contains(CampTuning.CAMP_TAG)) {
			CampShade.tick(level, mob);
		}
	}
}

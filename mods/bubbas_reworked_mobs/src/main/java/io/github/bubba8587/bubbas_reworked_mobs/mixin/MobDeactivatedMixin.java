// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An Ancient Fabricator's deactivated mob (specs/mob-factory.md): its whole server AI step (goals, brain, navigation,
 * look, move and jump controls) is skipped while {@code FabricatorMobs.DEACTIVATED} is attached; it still falls,
 * can be pushed and hurt. {@code FabricatorMobs.tick} keeps it limp, looking down, and wakes it when due.
 */
@Mixin(Mob.class)
abstract class MobDeactivatedMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void brm$deactivated(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		FabricatorMobs.State state = self.getAttached(FabricatorMobs.DEACTIVATED);
		if (state != null) {
			FabricatorMobs.tick(self, state);
			ci.cancel();
		}
	}
}

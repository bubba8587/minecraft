// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.undead.FireWard;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the fire ward goal. */
@Mixin(Mob.class)
abstract class FireWardMixin {
	@Shadow
	@Final
	protected GoalSelector goalSelector;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void brm$fearFire(EntityType<? extends Mob> type, Level level, CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (level != null && !level.isClientSide() && FireWard.fearsFire(mob)) {
			goalSelector.addGoal(1, new FireWard((PathfinderMob) mob));
		}
	}
}

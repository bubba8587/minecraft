// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Ender hit (owner): a mob just blinked away takes no new target for a moment. Every target goal (and anything else
 * that aims a mob) goes through Mob.setTarget, so holding a new target off there covers them all; clearing still works.
 * Brain-driven mobs are covered by erasing their attack-target memory around each AI step.
 */
@Mixin(Mob.class)
abstract class EnderConfusionMixin {
	/**
	 * Brain-driven mobs (piglins, hoglins, wardens...) target through the brain's attack-target memory, not setTarget:
	 * while confused, that memory is erased before and after each AI step, so nothing their brain picks survives the tick.
	 */
	@Inject(method = "serverAiStep", at = {@At("HEAD"), @At("RETURN")})
	private void bubbas$confusedBrain(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (GemActions.enderConfused(self) && self.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)) {
			self.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
		}
	}

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void bubbas$confused(@Nullable LivingEntity target, CallbackInfo ci) {
		if (target != null && GemActions.enderConfused((Mob) (Object) this)) {
			ci.cancel();
		}
	}
}

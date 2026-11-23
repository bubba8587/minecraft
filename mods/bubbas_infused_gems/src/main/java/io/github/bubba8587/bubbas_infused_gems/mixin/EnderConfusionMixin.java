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

/** Ender hit: a mob just blinked away takes no new target for a moment. */
@Mixin(Mob.class)
abstract class EnderConfusionMixin {
	/** Brain-driven mobs target through the brain's attack-target memory, not setTarget. */
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

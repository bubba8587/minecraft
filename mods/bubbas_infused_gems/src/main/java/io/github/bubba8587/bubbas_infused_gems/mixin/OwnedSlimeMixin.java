// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Oozing slimes (owner): neutral, they never take a target (their owner included), and they never despawn. */
@Mixin(Mob.class)
public abstract class OwnedSlimeMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void bubbas$neutralSlime(@Nullable LivingEntity target, CallbackInfo ci) {
		if (target != null && GemActions.ownedSlime((Mob) (Object) this)) {
			ci.cancel();
		}
	}

	/** Oozing slimes stay (owner): never despawned, by distance or by peaceful difficulty (they are neutral). */
	@Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
	private void bubbas$keepSlime(CallbackInfo ci) {
		if (GemActions.ownedSlime((Mob) (Object) this)) {
			ci.cancel();
		}
	}
}

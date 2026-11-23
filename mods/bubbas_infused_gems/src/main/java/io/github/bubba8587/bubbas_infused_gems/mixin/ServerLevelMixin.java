// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Silence gems: the wearer's movement vibrations are dropped before any listener hears them. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
			at = @At("HEAD"), cancellable = true)
	private void bubbas$gemSilence(Holder<GameEvent> event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
		if (GemActions.silenced(context.sourceEntity(), event)) {
			ci.cancel();
		}
	}
}

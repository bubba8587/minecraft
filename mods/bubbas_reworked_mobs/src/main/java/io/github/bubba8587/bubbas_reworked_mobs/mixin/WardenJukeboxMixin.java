// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.factory.Fabricators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wardens don't hear a Fabricator office's own jukebox (specs/mob-factory.md: its Warden ignores its own music). A
 * passive Fabricator Warden has no AI and hears nothing anyway (vanilla); this keeps a woken one from wandering back
 * to the office every time the record plays.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.warden.Warden$VibrationUser")
abstract class WardenJukeboxMixin {
	@Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
	private void brm$ignoreOfficeJukebox(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context,
			CallbackInfoReturnable<Boolean> cir) {
		if ((event.is(GameEvent.JUKEBOX_PLAY) || event.is(GameEvent.JUKEBOX_STOP_PLAY)) && Fabricators.isOfficeJukebox(level, pos)) {
			cir.setReturnValue(false);
		}
	}
}

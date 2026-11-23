// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import io.github.bubba8587.bubbas_world_tweaks.lava.GeyserLift;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Geysers lift and speed up players, every tick on both sides (lava/GeyserLift). */
@Mixin(Player.class)
abstract class PlayerGeyserMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void bubbas_world_tweaks$geyserLift(CallbackInfo ci) {
		GeyserLift.apply((Player) (Object) this);
	}
}

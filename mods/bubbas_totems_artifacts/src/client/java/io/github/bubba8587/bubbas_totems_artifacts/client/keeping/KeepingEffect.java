// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.client.keeping;

import io.github.bubba8587.bubbas_totems_artifacts.keeping.Keeping;
import net.minecraft.core.particles.ParticleTypes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * After a death a Totem of Keeping held off (keeping/Keeping): the Totem of Undying's moment on respawn, with the Totem
 * of Keeping in the middle of the screen and its particles around the player (vanilla's entity event 35, minus the
 * sound, which the server plays for everyone nearby).
 */
public final class KeepingEffect {
	private KeepingEffect() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(Keeping.KeptPayload.TYPE, (payload, context) -> {
			var mc = context.client();
			if (mc.player != null) {
				mc.particleEngine.createTrackingEmitter(mc.player, ParticleTypes.TOTEM_OF_UNDYING, 30);
				mc.player.displayItemActivation(Keeping.KeptPayload.shown());
			}
		});
	}
}

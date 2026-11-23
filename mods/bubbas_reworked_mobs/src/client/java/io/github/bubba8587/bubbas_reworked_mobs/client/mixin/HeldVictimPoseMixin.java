// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.client.mixin;

import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Someone a drowned holds rides it (specs/drowned.md "The grab"), but isn't drawn sitting: they stand, held. Players and
 * humanoid mobs. Wrapped upright in a spider's cocoon, they're drawn head down.
 */
@Mixin(HumanoidMobRenderer.class)
abstract class HeldVictimPoseMixin {
	@Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
	private static void brm$standWhenHeld(LivingEntity entity, HumanoidRenderState state, float partialTicks, ItemModelResolver itemModelResolver, CallbackInfo ci) {
		if (entity.getVehicle() instanceof Drowned || entity.getVehicle() instanceof io.github.bubba8587.bubbas_reworked_mobs.grip.Gripper) {
			state.isPassenger = false;
		}
		// Cocooned upright (players, villagers, zombies): they hang head down in the silk (owner, specs/spiders.md "Cocoons").
		if (entity.getVehicle() instanceof io.github.bubba8587.bubbas_reworked_mobs.spider.SilkCocoon
				&& !io.github.bubba8587.bubbas_reworked_mobs.spider.SilkCocoon.horizontal(entity)) {
			state.isUpsideDown = true;
		}
	}
}

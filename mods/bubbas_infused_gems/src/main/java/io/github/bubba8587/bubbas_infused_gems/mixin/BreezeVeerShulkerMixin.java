// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.jspecify.annotations.Nullable;

/** Breeze hit on shulker bullets: they home, so a one-time spread would be steered away. */
@Mixin(ShulkerBullet.class)
abstract class BreezeVeerShulkerMixin {
	@Shadow
	private double targetDeltaX;
	@Shadow
	private double targetDeltaY;
	@Shadow
	private double targetDeltaZ;

	@Inject(method = "selectNextMoveDirection", at = @At("RETURN"))
	private void bubbas$wobble(Direction.@Nullable Axis avoidAxis, @Nullable Entity target, CallbackInfo ci) {
		ShulkerBullet self = (ShulkerBullet) (Object) this;
		double m = GemActions.veer(self.getOwner());
		double len = Math.sqrt(this.targetDeltaX * this.targetDeltaX + this.targetDeltaY * this.targetDeltaY + this.targetDeltaZ * this.targetDeltaZ);
		if (m <= 1.0 || len < 1.0E-7) {
			return;
		}
		var random = self.getRandom();
		double spread = 0.0172275 * m;
		double x = this.targetDeltaX / len + random.triangle(0.0, spread);
		double y = this.targetDeltaY / len + random.triangle(0.0, spread);
		double z = this.targetDeltaZ / len + random.triangle(0.0, spread);
		double n = Math.sqrt(x * x + y * y + z * z);
		this.targetDeltaX = x / n * len;
		this.targetDeltaY = y / n * len;
		this.targetDeltaZ = z / n * len;
	}
}

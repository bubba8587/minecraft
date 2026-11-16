// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.bubba8587.bubbas_infused_gems.effect.GemActions;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Breeze hit (owner, Q22): a shooter carrying the Breeze gem effect fires off course. Vanilla's own spread (the
 * uncertainty every bow, crossbow, trident, snowball and mob shot passes to shoot) is multiplied, at least from 1.
 */
@Mixin(Projectile.class)
abstract class BreezeVeerMixin {
	@ModifyVariable(method = "shoot(DDDFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private float bubbas$veer(float uncertainty) {
		double m = GemActions.veer(((Projectile) (Object) this).getOwner());
		return m > 1.0 ? (float) (Math.max(uncertainty, 1.0F) * m) : uncertainty;
	}
}

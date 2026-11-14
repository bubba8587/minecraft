// dte:A2,A4
package io.github.bubba8587.refmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin (MixinExtras): snowballs deal at least {@link #MIN_SNOWBALL_DAMAGE} to anything, not just blazes.
 * Vanilla computes {@code int damage = entity instanceof Blaze ? 3 : 0;} in {@code Snowball#onHitEntity}.
 */
@Mixin(Snowball.class)
public abstract class SnowballMixin {
	private static final int MIN_SNOWBALL_DAMAGE = 2;

	@ModifyExpressionValue(method = "onHitEntity", at = @At(value = "CONSTANT", args = "intValue=0"))
	private int refmod$snowballsHurt(int original) {
		return Math.max(original, MIN_SNOWBALL_DAMAGE);
	}
}

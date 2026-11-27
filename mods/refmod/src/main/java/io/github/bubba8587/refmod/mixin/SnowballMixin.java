// dte:A2,A4
package io.github.bubba8587.refmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin (MixinExtras): snowballs deal at least {@link #MIN_SNOWBALL_DAMAGE} to anything, not just blazes.
 * Vanilla returns {@code entity instanceof Blaze ? 3 : 0} from {@code Snowball#getDamage}.
 */
@Mixin(Snowball.class)
public abstract class SnowballMixin {
	private static final int MIN_SNOWBALL_DAMAGE = 2;

	@ModifyReturnValue(method = "getDamage", at = @At("RETURN"))
	private int refmod$snowballsHurt(int original) {
		return Math.max(original, MIN_SNOWBALL_DAMAGE);
	}
}

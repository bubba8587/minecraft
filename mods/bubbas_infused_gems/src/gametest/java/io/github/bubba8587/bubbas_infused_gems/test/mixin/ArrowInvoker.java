// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the tests run an arrow hit without aiming one. */
@Mixin(Arrow.class)
public interface ArrowInvoker {
	@Invoker("doPostHurtEffects")
	void bubbas$hit(LivingEntity target);
}

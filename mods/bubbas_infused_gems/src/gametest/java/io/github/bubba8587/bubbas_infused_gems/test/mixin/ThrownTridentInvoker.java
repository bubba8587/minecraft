// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the tests land a thrown trident without throwing one. */
@Mixin(ThrownTrident.class)
public interface ThrownTridentInvoker {
	@Invoker("onHitEntity")
	void bubbas$hit(EntityHitResult hit);
}

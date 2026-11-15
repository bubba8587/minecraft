// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Which entity last deflected a projectile. */
@Mixin(Projectile.class)
public interface ProjectileAccessor {
	@Accessor("lastDeflectedBy")
	Entity bubbas$lastDeflectedBy();
}

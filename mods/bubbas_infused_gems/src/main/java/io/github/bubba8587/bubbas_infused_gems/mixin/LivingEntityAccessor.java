// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The hit cooldown's last damage: a Harming strike leaves it as the hit set it. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("lastHurt")
	float bubbas$lastHurt();

	@Accessor("lastHurt")
	void bubbas$setLastHurt(float value);
}

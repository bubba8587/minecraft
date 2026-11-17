// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Totem of Gathering: tells an item that can never be picked up (pickup delay 32767) from one just thrown. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
	@Accessor("pickupDelay")
	int bta$pickupDelay();
}

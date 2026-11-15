// dte:C4
package io.github.bubba8587.bubbas_infused_gems.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GrindstoneMenu.class)
public interface GrindstoneMenuAccessor {
	@Accessor("repairSlots")
	Container bubbas$repairSlots();
}

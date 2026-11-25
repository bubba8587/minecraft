// dte:B15,B17,B21,C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Map data centred on an exact spot (camp patrol maps). */
@Mixin(MapItemSavedData.class)
public interface MapCentreInvoker {
	@Invoker("<init>")
	static MapItemSavedData brm$create(int centerX, int centerZ, byte scale, boolean trackingPosition, boolean unlimitedTracking, boolean locked,
			ResourceKey<Level> dimension) {
		throw new AssertionError();
	}
}

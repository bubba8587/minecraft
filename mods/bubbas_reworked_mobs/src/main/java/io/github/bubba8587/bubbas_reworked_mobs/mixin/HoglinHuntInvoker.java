// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A hoglin kept as livestock (Nether outposts, camp/CampRoster): not hunted by its piglins, as in a bastion stable. */
@Mixin(Hoglin.class)
public interface HoglinHuntInvoker {
	@Invoker("setCannotBeHunted")
	void bubbas_reworked_mobs$setCannotBeHunted(boolean cannotBeHunted);
}

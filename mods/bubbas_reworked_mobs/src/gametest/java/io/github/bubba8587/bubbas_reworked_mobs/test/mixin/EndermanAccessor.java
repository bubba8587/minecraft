// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only: ask an enderman whether a player is staring at it (vanilla's eye-contact check, with E4's busy rule). */
@Mixin(Enderman.class)
public interface EndermanAccessor {
	@Invoker("isBeingStaredBy")
	boolean brmTest$isBeingStaredBy(Player player);
}

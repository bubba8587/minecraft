// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the tests work out a villager's prices for a player. */
@Mixin(Villager.class)
public interface VillagerInvoker {
	@Invoker("updateSpecialPrices")
	void bubbas$updatePrices(Player player);
}

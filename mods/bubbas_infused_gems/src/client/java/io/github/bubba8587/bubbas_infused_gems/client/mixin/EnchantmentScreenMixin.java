// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The preview hint for an Infused Gem, like vanilla's clue. */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@ModifyReturnValue(method = "getClue", at = @At("RETURN"))
	private @Nullable Component bubbas$gemClue(@Nullable Component clue, int index) {
		ItemStack stack = this.menu.getSlot(0).getItem();
		if (!GemEnchanting.isInfusedGem(stack) || this.menu.costs[index] <= 0) {
			return clue;
		}
		InfusedGem gem = stack.get(ModComponents.INFUSED_GEM);
		int seed = this.menu.getEnchantmentSeed();
		return GemEnchanting.clue(GemEnchanting.roll(seed, index + 1, gem), GemEnchanting.outcome(seed, index + 1, gem));
	}
}

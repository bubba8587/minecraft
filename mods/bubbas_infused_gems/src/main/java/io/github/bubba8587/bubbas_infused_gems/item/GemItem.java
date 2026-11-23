// dte:C4
package io.github.bubba8587.bubbas_infused_gems.item;

import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The one finished gem item. */
public class GemItem extends Item {
	public GemItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		Gem gem = stack.get(ModComponents.GEM);
		if (gem == null) {
			return super.getName(stack);
		}
		return Component.translatable("item.bubbas_infused_gems.gem.named",
				GemText.numeral(gem.infusion()), GemText.effectName(gem.effect()));
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.item;

import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The one Infused Gem item (owner): its name carries the Infusion level, effect and form. */
public class InfusedGemItem extends Item {
	public InfusedGemItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		InfusedGem gem = stack.get(ModComponents.INFUSED_GEM);
		if (gem == null) {
			return super.getName(stack);
		}
		String key = gem.form() == GemForm.NORMAL || gem.form() == GemForm.ITEM ? "item.bubbas_infused_gems.infused_gem.named"
				: "item.bubbas_infused_gems.infused_gem.named_form";
		return Component.translatable(key, GemText.numeral(gem.infusion()), GemText.effectName(gem.effect()),
				Component.translatable(GemText.KEY + "form." + gem.form().getSerializedName()));
	}
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client.mixin;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The preview hint for an Infused Gem, like vanilla's clue. */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
	protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void bubbas$gemClue(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		ItemStack stack = this.menu.getSlot(0).getItem();
		if (!GemEnchanting.isInfusedGem(stack)) {
			return;
		}
		InfusedGem gem = stack.get(ModComponents.INFUSED_GEM);
		for (int i = 0; i < 3; i++) {
			if (this.menu.costs[i] > 0 && this.isHovering(60, 14 + 19 * i, 108, 17, mouseX, mouseY)) {
				List<Component> lines = new ArrayList<>();
				int seed = this.menu.getEnchantmentSeed();
				lines.add(GemEnchanting.clue(GemEnchanting.roll(seed, i + 1, gem), GemEnchanting.outcome(seed, i + 1, gem)));
				if (!this.minecraft.player.hasInfiniteMaterials()) {
					lines.add(CommonComponents.EMPTY);
					int cost = i + 1;
					if (this.minecraft.player.experienceLevel < this.menu.costs[i]) {
						lines.add(Component.translatable("container.enchant.level.requirement", this.menu.costs[i]).withStyle(ChatFormatting.RED));
					} else {
						MutableComponent lapis = cost == 1 ? Component.translatable("container.enchant.lapis.one")
								: Component.translatable("container.enchant.lapis.many", cost);
						lines.add(lapis.withStyle(this.menu.getGoldCount() >= cost ? ChatFormatting.GRAY : ChatFormatting.RED));
						lines.add((cost == 1 ? Component.translatable("container.enchant.level.one")
								: Component.translatable("container.enchant.level.many", cost)).withStyle(ChatFormatting.GRAY));
					}
				}
				graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
				return;
			}
		}
	}
}

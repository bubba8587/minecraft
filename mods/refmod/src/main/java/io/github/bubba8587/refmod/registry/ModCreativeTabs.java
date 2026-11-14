// dte:B5,B6
package io.github.bubba8587.refmod.registry;

import io.github.bubba8587.refmod.RefMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> REFMOD_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, RefMod.id("refmod"));
	public static final CreativeModeTab REFMOD = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, REFMOD_KEY,
			FabricCreativeModeTab.builder()
					.icon(() -> new ItemStack(ModItems.TALLY_STICK))
					.title(Component.translatable("itemGroup.refmod"))
					.displayItems((params, output) -> {
						// Every item in the refmod namespace, in registration order.
						BuiltInRegistries.ITEM.listElements()
								.filter(holder -> holder.key().identifier().getNamespace().equals(RefMod.MOD_ID))
								.forEach(holder -> output.accept(holder.value()));
					})
					.build());

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}

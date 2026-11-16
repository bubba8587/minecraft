// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.registry;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, BubbasTotemsArtifacts.id("main"));
	public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY,
			FabricCreativeModeTab.builder()
					.icon(() -> new ItemStack(ModItems.TOTEM_OF_WARDING))
					.title(Component.translatable("itemGroup." + BubbasTotemsArtifacts.MOD_ID))
					.displayItems((params, output) -> {
						output.accept(ModItems.GRAVE_TALISMAN);
						output.accept(ModItems.TOTEM_OF_WARDING);
					})
					.build());

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}

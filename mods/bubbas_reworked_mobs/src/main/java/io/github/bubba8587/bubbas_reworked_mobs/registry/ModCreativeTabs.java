// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> MAIN_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, BubbasReworkedMobs.id("main"));
	public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN_KEY,
			FabricCreativeModeTab.builder()
					.icon(() -> new ItemStack(ModItems.sleeper(UndeadVariant.SKELETON)))
					.title(Component.translatable("itemGroup." + BubbasReworkedMobs.MOD_ID))
					.displayItems((params, output) -> {
						for (UndeadVariant variant : UndeadVariant.values()) {
							output.accept(ModItems.sleeper(variant));
							// Other poses as their own stacks (the item keeps the pose in minecraft:block_state).
							for (SleeperPose pose : variant.poses().subList(1, variant.poses().size())) {
								output.accept(withPose(new ItemStack(ModItems.sleeper(variant)), pose));
							}
						}
						output.accept(ModItems.GRAVESTONE);
					})
					.build());

	private ModCreativeTabs() {
	}

	public static ItemStack withPose(ItemStack stack, SleeperPose pose) {
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(SleeperBlock.POSE, pose));
		return stack;
	}

	public static void initialize() {
	}
}

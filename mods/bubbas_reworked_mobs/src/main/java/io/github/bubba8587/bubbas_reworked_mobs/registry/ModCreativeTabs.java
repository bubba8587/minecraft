// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperItem;
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
							for (boolean statue : new boolean[] {false, true}) {
								for (SleeperPose pose : variant.poses()) {
									output.accept(SleeperItem.form(ModItems.sleeper(variant), pose, statue));
								}
							}
						}
						output.accept(ModItems.GRAVESTONE);
						output.accept(ModItems.CAPTAINS_HELM);
						output.accept(ModItems.NETHER_STAR_SHARD);
						output.accept(ModItems.WITHER_CAPTAIN_SPAWN_EGG);
						output.accept(ModItems.SOULFIRE_BLAZE_SPAWN_EGG);
						output.accept(ModItems.BOSS_PIGLIN_SPAWN_EGG);
						output.accept(io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire.SOUL_BLAZE_ROD);
						output.accept(io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire.SOUL_BLAZE_POWDER);
						output.accept(io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire.SOUL_FIRE_CHARGE);
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

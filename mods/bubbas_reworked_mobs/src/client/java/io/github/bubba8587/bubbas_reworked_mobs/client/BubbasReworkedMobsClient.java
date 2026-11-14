// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.client;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.client.undead.SleeperRenderer;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;

public class BubbasReworkedMobsClient implements ClientModInitializer {
	@Override
	@SuppressWarnings("unchecked")
	public void onInitializeClient() {
		// 32x textures (Faithful style) as an optional built-in resource pack: specs/textures.md.
		FabricLoader.getInstance().getModContainer(BubbasReworkedMobs.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasReworkedMobs.id(BubbasReworkedMobs.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Reworked Mobs 32x (Faithful style)"), PackActivationType.NORMAL));

		// Undead: sleepers draw through one block entity renderer; woken mobs reuse the vanilla renderers.
		BlockEntityRenderers.register(ModBlockEntities.SLEEPER, SleeperRenderer::new);
		EntityRenderers.register((EntityType<? extends Skeleton>) ModEntities.woken(UndeadVariant.SKELETON), SkeletonRenderer::new);
		EntityRenderers.register((EntityType<? extends Zombie>) ModEntities.woken(UndeadVariant.ZOMBIE), ZombieRenderer::new);
	}
}

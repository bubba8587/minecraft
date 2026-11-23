// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.client;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.client.display.LootDisplayRenderer;
import io.github.bubba8587.bubbas_reworked_mobs.client.undead.SleeperRenderer;
import io.github.bubba8587.bubbas_reworked_mobs.client.undead.WokenSkeletonRenderer;
import io.github.bubba8587.bubbas_reworked_mobs.client.undead.WokenZombieRenderer;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
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
		io.github.bubba8587.bubbas_reworked_mobs.client.meter.MeterBars.initialize();
		// 32x textures (Faithful style) as an optional built-in resource pack: specs/textures.md.
		FabricLoader.getInstance().getModContainer(BubbasReworkedMobs.MOD_ID).ifPresent(mod ->
			ResourceLoader.registerBuiltinPack(BubbasReworkedMobs.id(BubbasReworkedMobs.MOD_ID + "_32x"), mod,
				Component.literal("Bubba's Reworked Mobs 32x (Faithful style)"), PackActivationType.NORMAL));

		// Undead: sleepers draw through one block entity renderer; woken mobs use vanilla renderers with our walk animations (UndeadGaits).
		BlockEntityRenderers.register(ModBlockEntities.SLEEPER, SleeperRenderer::new);
		EntityRenderers.register((EntityType<? extends Skeleton>) ModEntities.woken(UndeadVariant.SKELETON), WokenSkeletonRenderer::new);
		EntityRenderers.register((EntityType<? extends Zombie>) ModEntities.woken(UndeadVariant.ZOMBIE), WokenZombieRenderer::new);
		// Husks and strays: the same renderers with their own textures (and the stray's ragged clothes).
		EntityRenderers.register((EntityType<? extends Zombie>) ModEntities.woken(UndeadVariant.HUSK),
				c -> new WokenZombieRenderer(c, net.minecraft.resources.Identifier.withDefaultNamespace("textures/entity/zombie/husk.png")));
		EntityRenderers.register((EntityType<? extends Skeleton>) ModEntities.woken(UndeadVariant.STRAY), WokenSkeletonRenderer::stray);

		// Loot on display: the item's own model, posed per placement.
		BlockEntityRenderers.register(ModBlockEntities.LOOT_DISPLAY, LootDisplayRenderer::new);
		// Gravestones: vanilla's sign text renderer, placed on the stone's face (the stone is a block model).
		BlockEntityRenderers.register(ModBlockEntities.GRAVESTONE,
				(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider) io.github.bubba8587.bubbas_reworked_mobs.client.grave.GravestoneRenderer::new);
		EntityRenderers.register(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.NEST_SPIDER,
				io.github.bubba8587.bubbas_reworked_mobs.client.spider.NestSpiderRenderer::new);
		EntityRenderers.register(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.GIANT_SPIDER,
				io.github.bubba8587.bubbas_reworked_mobs.client.spider.GiantSpiderRenderer::new);
		EntityRenderers.register(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.SPUN_WEB,
				io.github.bubba8587.bubbas_reworked_mobs.client.spider.SpunWebRenderer::new);
		EntityRenderers.register(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.SILK_COCOON,
				io.github.bubba8587.bubbas_reworked_mobs.client.spider.SilkCocoonRenderer::new);
		EntityRenderers.register(io.github.bubba8587.bubbas_reworked_mobs.spider.Spiders.SPIDER_NEST, net.minecraft.client.renderer.entity.NoopRenderer::new);
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** Spiders (specs/spiders.md): entity types, attributes and worldgen. */
public final class Spiders {
	public static final EntityType<NestSpider> NEST_SPIDER = register("nest_spider",
			EntityType.Builder.of(NestSpider::new, MobCategory.MONSTER).sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F)
					.clientTrackingRange(8).notInPeaceful());
	public static final EntityType<SpiderNest> SPIDER_NEST = register("spider_nest",
			EntityType.Builder.<SpiderNest>of(SpiderNest::new, MobCategory.MISC).sized(0.5F, 0.5F).noSummon().clientTrackingRange(4)
					.updateInterval(20));

	public static final EntityType<GiantSpider> GIANT_SPIDER = register("giant_spider",
			EntityType.Builder.of(GiantSpider::new, MobCategory.MONSTER).sized(1.4F, 0.9F).eyeHeight(0.65F).passengerAttachments(0.765F)
					.clientTrackingRange(10).notInPeaceful());

	public static final EntityType<SpunWeb> SPUN_WEB = register("spun_web",
			EntityType.Builder.<SpunWeb>of(SpunWeb::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(40));

	public static final ResourceKey<PlacedFeature> NEST_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, BubbasReworkedMobs.id("spider_nest"));

	private Spiders() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(NEST_SPIDER, Spider.createAttributes());
		FabricDefaultAttributeRegistry.register(GIANT_SPIDER, GiantSpider.createAttributes());
		net.minecraft.world.entity.SpawnPlacements.register(GIANT_SPIDER, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GiantSpider::checkGiantSpawnRules);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA,
				net.minecraft.world.level.biome.Biomes.OLD_GROWTH_SPRUCE_TAIGA), MobCategory.MONSTER, GIANT_SPIDER, SpiderTuning.GIANT_SPAWN_WEIGHT, 1, 1);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, BubbasReworkedMobs.id("spider_nest"), SpiderNestFeature.CODEC);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION, NEST_FEATURE);
	}
}

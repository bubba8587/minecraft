// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.captain;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** The Wither Captain and Soulfire Blazes (specs/wither-captain.md): entity types, attributes and spawns. */
public final class Captains {
	public static final EntityType<WitherCaptain> WITHER_CAPTAIN = register("wither_captain",
			EntityType.Builder.of(WitherCaptain::new, MobCategory.MONSTER).fireImmune().immuneTo(BlockTags.WITHER_SKELETON_IMMUNE_TO)
					.sized(0.7F, 2.4F).eyeHeight(2.1F).ridingOffset(-0.875F).clientTrackingRange(8).notInPeaceful());
	public static final EntityType<SoulfireBlaze> SOULFIRE_BLAZE = register("soulfire_blaze",
			EntityType.Builder.of(SoulfireBlaze::new, MobCategory.MONSTER).fireImmune().sized(0.6F, 1.8F).clientTrackingRange(8).notInPeaceful());
	public static final EntityType<SoulfireCharge> SOULFIRE_CHARGE = register("soulfire_charge",
			EntityType.Builder.<SoulfireCharge>of(SoulfireCharge::new, MobCategory.MISC).noLootTable().sized(0.3125F, 0.3125F)
					.clientTrackingRange(4).updateInterval(10));

	private Captains() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(WITHER_CAPTAIN, WitherCaptain.createAttributes());
		FabricDefaultAttributeRegistry.register(SOULFIRE_BLAZE, Blaze.createAttributes());
		SpawnPlacements.register(WITHER_CAPTAIN, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(SOULFIRE_BLAZE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.SOUL_SAND_VALLEY), MobCategory.MONSTER, SOULFIRE_BLAZE,
				CaptainTuning.SOULFIRE_BLAZE_VALLEY_WEIGHT, 1, 1);
		CaptainsHelm.initialize();
	}
}

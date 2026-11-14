// dte:B5,B6
package io.github.bubba8587.refmod.registry;

import io.github.bubba8587.refmod.RefMod;
import io.github.bubba8587.refmod.entity.PebbleEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> PEBBLE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, RefMod.id("pebble"));
	public static final EntityType<PebbleEntity> PEBBLE = register(PEBBLE_KEY,
			EntityType.Builder.<PebbleEntity>of(PebbleEntity::new, MobCategory.MISC)
					.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

	private ModEntities() {
	}

	private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void initialize() {
	}
}

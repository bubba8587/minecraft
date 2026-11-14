// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.registry;

import java.util.EnumMap;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public final class ModEntities {
	/** Woken undead, one entity type per {@link UndeadVariant}. */
	public static final Map<UndeadVariant, EntityType<? extends Monster>> WOKEN = new EnumMap<>(UndeadVariant.class);

	static {
		for (UndeadVariant variant : UndeadVariant.values()) {
			ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BubbasReworkedMobs.id(variant.wokenId()));
			EntityType<? extends Monster> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, variant.wokenTypeBuilder().build(key));
			FabricDefaultAttributeRegistry.register(type, variant.wokenAttributes());
			WOKEN.put(variant, type);
		}
	}

	private ModEntities() {
	}

	public static EntityType<? extends Monster> woken(UndeadVariant variant) {
		return WOKEN.get(variant);
	}

	public static void initialize() {
	}
}

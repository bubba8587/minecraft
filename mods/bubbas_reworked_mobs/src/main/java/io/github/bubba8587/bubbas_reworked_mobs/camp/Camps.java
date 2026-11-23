// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Wiring for monster camps (specs/monster-camps.md): the structure and piece types, the loot table, the ticking that
 * registers camps and spawns their rosters, and counting the roster's deaths. Where camps go (biomes, spacing) is data:
 * data/bubbas_reworked_mobs/worldgen/structure/monster_camp.json and structure_set/monster_camps.json.
 */
public final class Camps {
	public static final StructureType<CampStructure> CAMP = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, BubbasReworkedMobs.id("monster_camp"),
			() -> CampStructure.CODEC);
	public static final StructurePieceType CAMP_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, BubbasReworkedMobs.id("monster_camp"),
			(StructurePieceType.ContextlessType) CampPiece::new);
	public static final ResourceKey<Structure> CAMP_KEY = ResourceKey.create(Registries.STRUCTURE, BubbasReworkedMobs.id("monster_camp"));
	public static final ResourceKey<LootTable> CAMP_LOOT = ResourceKey.create(Registries.LOOT_TABLE, BubbasReworkedMobs.id("chests/monster_camp"));

	private Camps() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(CampSites::tick);
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			var reason = entity.getRemovalReason();
			if (reason != null && reason.shouldDestroy() && entity.entityTags().contains(CampTuning.CAMP_TAG)) {
				CampSites.removed(level, entity);
			}
		});
	}
}

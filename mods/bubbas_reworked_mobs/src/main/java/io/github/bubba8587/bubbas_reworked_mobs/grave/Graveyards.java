// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Wiring for graveyards and crypts (specs/graveyards.md): the structure type and piece type. Where yards go (biomes,
 * spacing) is data: data/bubbas_reworked_mobs/worldgen/structure/crypt_yard.json and structure_set/crypt_yards.json.
 */
public final class Graveyards {
	public static final StructureType<CryptYardStructure> CRYPT_YARD = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			BubbasReworkedMobs.id("crypt_yard"), () -> CryptYardStructure.CODEC);
	public static final StructurePieceType CRYPT_YARD_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE,
			BubbasReworkedMobs.id("crypt_yard"), (StructurePieceType.ContextlessType) CryptYardPiece::new);
	public static final ResourceKey<Structure> CRYPT_YARD_KEY = ResourceKey.create(Registries.STRUCTURE, BubbasReworkedMobs.id("crypt_yard"));

	private Graveyards() {
	}

	public static void initialize() {
		Wisps.initialize();
	}
}

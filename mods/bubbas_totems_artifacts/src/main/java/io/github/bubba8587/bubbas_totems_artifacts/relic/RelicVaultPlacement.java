// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Structure placement bubbas_totems_artifacts:relic_sites: exactly the chunks RelicSites gives for the world seed. */
public final class RelicVaultPlacement implements StructurePlacement {
	public static final RelicVaultPlacement INSTANCE = new RelicVaultPlacement();
	public static final MapCodec<RelicVaultPlacement> CODEC = MapCodec.unit(INSTANCE);

	private RelicVaultPlacement() {
	}

	@Override
	public boolean isStructureChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
		return RelicSites.at(state.getLevelSeed(), sourceX, sourceZ) != null;
	}

	@Override
	public MapCodec<? extends StructurePlacement> codec() {
		return CODEC;
	}
}

// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.mixin;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads a structure state's biome source. */
@Mixin(ChunkGeneratorStructureState.class)
public interface StructureStateAccessor {
	@Accessor("biomeSource")
	BiomeSource bta$biomeSource();
}
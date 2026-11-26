// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortressStructure;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Deep-sea fortress placement scope. */
@Mixin(StructureStart.class)
abstract class StructureStartDeepseaMixin {
	@Shadow
	public abstract Structure getStructure();

	@WrapMethod(method = "placeInChunk")
	private void bubbas_world_tweaks$deepseaSwap(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
			RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, Operation<Void> original) {
		if (getStructure() instanceof DeepseaFortressStructure) {
			DeepseaFortresses.placing(() -> original.call(level, structureManager, generator, random, chunkBB, chunkPos));
		} else {
			original.call(level, structureManager, generator, random, chunkBB, chunkPos);
		}
	}
}

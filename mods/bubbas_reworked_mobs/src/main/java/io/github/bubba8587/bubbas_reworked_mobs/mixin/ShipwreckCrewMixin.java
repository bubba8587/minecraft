// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import io.github.bubba8587.bubbas_reworked_mobs.undead.ShipwreckCrew;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shipwreck crew. */
@Mixin(ShipwreckPieces.ShipwreckPiece.class)
abstract class ShipwreckCrewMixin {
	@Inject(method = "postProcess", at = @At("RETURN"))
	private void brm$crew(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos, CallbackInfo ci) {
		ShipwreckPieces.ShipwreckPiece piece = (ShipwreckPieces.ShipwreckPiece) (Object) this;
		ShipwreckCrew.place(level, piece.getBoundingBox(), chunkBB, level.getSeed() ^ piece.getBoundingBox().getCenter().asLong());
	}
}

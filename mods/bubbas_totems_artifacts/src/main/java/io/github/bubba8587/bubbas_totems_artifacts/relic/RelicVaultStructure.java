// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A named relic vault, buried under its site (RelicSites). Built in code by {@link RelicVaultPiece}; the door is at
 * the site's chunk middle, the floor {@link RelicTuning#NAMED_DEPTH} under the ground there.
 */
public class RelicVaultStructure extends Structure {
	public static final MapCodec<RelicVaultStructure> CODEC = simpleCodec(RelicVaultStructure::new);

	public RelicVaultStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		RelicSites.Site site = RelicSites.at(context.seed(), chunk.x(), chunk.z());
		if (site == null) {
			return Optional.empty();
		}
		BlockPos centre = site.centre();
		int ground = context.chunkGenerator().getFirstOccupiedHeight(centre.getX(), centre.getZ(), Heightmap.Types.OCEAN_FLOOR_WG,
				context.heightAccessor(), context.randomState());
		int floor = Math.max(context.heightAccessor().getMinY() + 12, ground - RelicTuning.NAMED_DEPTH);
		BlockPos door = new BlockPos(centre.getX(), floor, centre.getZ());
		RelicVaultPiece.Kind kind = site.size() == 9 ? RelicVaultPiece.Kind.GRAND : RelicVaultPiece.Kind.SMALL;
		long seed = context.random().nextLong();
		return Optional.of(new GenerationStub(door, builder -> builder.addPiece(new RelicVaultPiece(kind, site.name(), door, seed))));
	}

	@Override
	public StructureType<?> type() {
		return Relics.VAULT_TYPE;
	}
}

// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortressStructure;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;

/** Vanilla fortress pieces and whole deep-sea fortresses placed for tests. */
final class DeepseaTestPieces {
	static final String ROOM = "bubbas_world_tweaks-test:fortress_room";
	static final String YARD = "bubbas_world_tweaks-test:fortress_yard";
	static final Block[] NETHER = { Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.LAVA,
			Blocks.NETHER_WART, Blocks.CHEST };

	private DeepseaTestPieces() {
	}

	/** A spawner room facing south at relative (1, 1, 1). */
	static NetherFortressPieces.MonsterThrone throne(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		return new NetherFortressPieces.MonsterThrone(1, new BoundingBox(at.getX(), at.getY(), at.getZ(), at.getX() + 6, at.getY() + 7,
				at.getZ() + 8), Direction.SOUTH);
	}

	/** A corridor turn with a chest, facing south at relative (9, 1, 1). */
	static NetherFortressPieces.CastleSmallCorridorLeftTurnPiece chestTurn(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(new BlockPos(9, 1, 1));
		long seed = 0;
		while (RandomSource.create(seed).nextInt(3) != 0) {
			seed++;
		}
		return new NetherFortressPieces.CastleSmallCorridorLeftTurnPiece(1, RandomSource.create(seed), new BoundingBox(at.getX(), at.getY(),
				at.getZ(), at.getX() + 4, at.getY() + 6, at.getZ() + 4), Direction.SOUTH);
	}

	/** A south-facing box of this size at relative (1, 1, 1). */
	static BoundingBox box(GameTestHelper helper, int w, int h, int d) {
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		return new BoundingBox(at.getX(), at.getY(), at.getZ(), at.getX() + w - 1, at.getY() + h - 1, at.getZ() + d - 1);
	}

	/** Places a lone start piece as a deep-sea fortress, chunk by chunk like worldgen. */
	static void placeStart(ServerLevel level, StructurePiece start) {
		Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(DeepseaFortresses.STRUCTURE);
		StructurePiecesBuilder builder = new StructurePiecesBuilder();
		builder.addPiece(start);
		placeAll(level, new StructureStart(structure, ChunkPos.containing(start.getBoundingBox().getCenter()), 0, builder.build()), 7L);
	}

	/** Places a piece in the test area, with or without the deep-sea swap. */
	static void place(GameTestHelper helper, StructurePiece piece, boolean deepsea) {
		ServerLevel level = helper.getLevel();
		var area = helper.getBounds();
		BoundingBox chunkBB = new BoundingBox((int) area.minX, (int) area.minY, (int) area.minZ, (int) area.maxX - 1, (int) area.maxY - 1,
				(int) area.maxZ - 1);
		Runnable run = () -> piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), RandomSource.create(7L),
				chunkBB, ChunkPos.containing(piece.getBoundingBox().getCenter()), piece.getBoundingBox().getCenter());
		if (deepsea) {
			DeepseaFortresses.placing(run);
		} else {
			run.run();
		}
	}

	/** World position of a south-facing piece's local (x, y, z). */
	static BlockPos local(StructurePiece piece, int x, int y, int z) {
		BoundingBox box = piece.getBoundingBox();
		return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
	}

	/** Lays out and places a whole deep-sea fortress around (x, z) on a floor at floorY, the way worldgen does. */
	static StructureStart fortress(ServerLevel level, long seed, int x, int z, int floorY, int seaLevel) {
		Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(DeepseaFortresses.STRUCTURE);
		StructurePiecesBuilder builder = new StructurePiecesBuilder();
		DeepseaFortressStructure.layOut(builder, RandomSource.create(seed), x, z, floorY, seaLevel);
		StructureStart start = new StructureStart(structure, ChunkPos.containing(new BlockPos(x, floorY, z)), 0, builder.build());
		placeAll(level, start, seed);
		return start;
	}

	private static void placeAll(ServerLevel level, StructureStart start, long seed) {
		BoundingBox box = start.getBoundingBox();
		ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
		ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
		RandomSource random = RandomSource.create(seed);
		ChunkPos.rangeClosed(min, max).forEach(c -> {
			level.getChunk(c.x(), c.z());
			start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), random,
					new BoundingBox(c.getMinBlockX(), level.getMinY(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxY(), c.getMaxBlockZ()), c);
		});
	}
}

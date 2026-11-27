// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.fortress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;

/** A slanted tear of erosion through a deep-sea fortress: its stonework along a ragged tube becomes water. */
public class TearPiece extends StructurePiece {
	/** Extra radius the ragged edge can reach, in blocks. */
	private static final double RAGGED = 1.2;
	private final BlockPos from;
	private final BlockPos to;
	private final int radius;

	public TearPiece(BlockPos from, BlockPos to, int radius) {
		super(DeepseaFortresses.TEAR_PIECE, 0, BoundingBox.fromCorners(from, to).inflatedBy(radius + 2));
		this.from = from;
		this.to = to;
		this.radius = radius;
	}

	public TearPiece(CompoundTag tag) {
		super(DeepseaFortresses.TEAR_PIECE, tag);
		this.from = new BlockPos(tag.getIntOr("FromX", 0), tag.getIntOr("FromY", 0), tag.getIntOr("FromZ", 0));
		this.to = new BlockPos(tag.getIntOr("ToX", 0), tag.getIntOr("ToY", 0), tag.getIntOr("ToZ", 0));
		this.radius = tag.getIntOr("Radius", 2);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("FromX", from.getX());
		tag.putInt("FromY", from.getY());
		tag.putInt("FromZ", from.getZ());
		tag.putInt("ToX", to.getX());
		tag.putInt("ToY", to.getY());
		tag.putInt("ToZ", to.getZ());
		tag.putInt("Radius", radius);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
			ChunkPos chunkPos, BlockPos referencePos) {
		Vec3 a = Vec3.atCenterOf(from);
		Vec3 ab = Vec3.atCenterOf(to).subtract(a);
		double lengthSq = ab.lengthSqr();
		BoundingBox box = this.getBoundingBox();
		BlockState water = Blocks.WATER.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(Math.max(box.minX(), chunkBB.minX()), Math.max(box.minY(), chunkBB.minY()),
				Math.max(box.minZ(), chunkBB.minZ()), Math.min(box.maxX(), chunkBB.maxX()), Math.min(box.maxY(), chunkBB.maxY()),
				Math.min(box.maxZ(), chunkBB.maxZ()))) {
			Vec3 p = Vec3.atCenterOf(pos);
			double t = lengthSq == 0 ? 0 : Mth.clamp(p.subtract(a).dot(ab) / lengthSq, 0.0, 1.0);
			double taper = Math.sin(Math.PI * t);
			double reach = radius * (0.35 + 0.65 * taper) + RAGGED * Math.floorMod(Mth.getSeed(pos), 100L) / 100.0;
			if (p.distanceToSqr(a.add(ab.scale(t))) > reach * reach || !FortressBlocks.erodes(level.getBlockState(pos))) {
				continue;
			}
			level.setBlock(pos, water, 2);
			for (Direction side : Direction.Plane.HORIZONTAL) {
				BlockPos next = pos.relative(side);
				if (chunkBB.isInside(next) && FortressBlocks.reshapes(level.getBlockState(next))) {
					level.getChunk(next).markPosForPostProcessing(next);
				}
			}
		}
	}
}

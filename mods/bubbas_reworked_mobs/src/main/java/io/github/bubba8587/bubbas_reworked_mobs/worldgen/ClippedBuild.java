// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Places a code-built structure piece's blocks only inside the chunk being generated, and reads the ground the same way
 * in every chunk (crypt yards, monster camps). A piece decides everything from its own seed; each chunk it spans builds
 * its own part.
 */
public class ClippedBuild {
	protected final WorldGenLevel level;
	protected final BoundingBox chunk;

	public ClippedBuild(WorldGenLevel level, BoundingBox chunk) {
		this.level = level;
		this.chunk = chunk;
	}

	public WorldGenLevel level() {
		return level;
	}

	public boolean inside(int x, int y, int z) {
		return chunk.isInside(x, y, z);
	}

	/**
	 * The ground of this column: the top block that isn't a tree, a plant or air, or MIN_VALUE when the column isn't
	 * in this chunk or the ground is under water. Read the same way in every chunk (the yard is built after trees,
	 * which it clears).
	 */
	public int ground(int x, int z) {
		if (x < chunk.minX() || x > chunk.maxX() || z < chunk.minZ() || z > chunk.maxZ()) {
			return Integer.MIN_VALUE;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
		while (pos.getY() > level.getMinY() && cover(level.getBlockState(pos))) {
			pos.move(0, -1, 0);
		}
		return level.getFluidState(pos).isEmpty() ? pos.getY() : Integer.MIN_VALUE;
	}

	/** Trees and plants standing on the ground (cleared out of a yard), and air. */
	public static boolean cover(BlockState state) {
		return state.isAir() || state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.LEAVES)
				|| state.is(net.minecraft.tags.BlockTags.REPLACEABLE) || state.is(Blocks.VINE) || state.is(Blocks.BEE_NEST)
				|| state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
				|| state.is(net.minecraft.tags.BlockTags.FLOWERS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.PUMPKIN);
	}

	/** Clears trees and plants from the ground up to {@code height} above it, in this column. */
	public void clearCover(int x, int z, int height) {
		int g = ground(x, z);
		if (g == Integer.MIN_VALUE) {
			return;
		}
		for (int y = g + 1; y <= g + height; y++) {
			if (inside(x, y, z) && !level.getBlockState(new BlockPos(x, y, z)).isAir() && cover(level.getBlockState(new BlockPos(x, y, z)))) {
				put(x, y, z, Blocks.AIR.defaultBlockState());
			}
		}
	}

	private final java.util.Set<BlockPos> fences = new java.util.HashSet<>();

	public void put(int x, int y, int z, BlockState state) {
		if (inside(x, y, z)) {
			BlockPos pos = new BlockPos(x, y, z);
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			if (state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {
				fences.add(pos);
			} else {
				fences.remove(pos);
			}
		}
	}

	/**
	 * Fence pass after a piece is built: orphans (no side joined, nothing on top, no fence below) are taken out, then every
	 * fence's sides are set from its neighbours, and fences just over the chunk edge (built by an earlier chunk) get their
	 * side toward this chunk set too.
	 */
	public void tidyFences(BoundingBox piece) {
		boolean removed = true;
		while (removed) {
			removed = false;
			for (java.util.Iterator<BlockPos> it = fences.iterator(); it.hasNext();) {
				BlockPos pos = it.next();
				BlockState state = level.getBlockState(pos);
				if (!(state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock fence)) {
					it.remove();
					continue;
				}
				boolean joined = false;
				for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
					BlockPos n = pos.relative(d);
					joined |= joins(fence, n, d) || !inside(n.getX(), n.getY(), n.getZ()) && state.getValue(propertyOf(d));
				}
				BlockState above = level.getBlockState(pos.above());
				BlockState below = level.getBlockState(pos.below());
				if (!joined && above.isAir() && !(below.getBlock() instanceof net.minecraft.world.level.block.FenceBlock)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					it.remove();
					removed = true;
				}
			}
		}
		for (BlockPos pos : fences) {
			level.setBlock(pos, joined(pos, level.getBlockState(pos)), Block.UPDATE_CLIENTS);
		}
		fences.clear();
		int y0 = Math.max(piece.minY(), level.getMinY());
		int y1 = Math.min(piece.maxY(), level.getMaxY());
		for (int x = chunk.minX() - 1; x <= chunk.maxX() + 1; x++) {
			for (int z = chunk.minZ() - 1; z <= chunk.maxZ() + 1; z++) {
				boolean ring = x < chunk.minX() || x > chunk.maxX() || z < chunk.minZ() || z > chunk.maxZ();
				boolean corner = (x < chunk.minX() || x > chunk.maxX()) && (z < chunk.minZ() || z > chunk.maxZ());
				if (!ring || corner || x < piece.minX() || x > piece.maxX() || z < piece.minZ() || z > piece.maxZ()) {
					continue;
				}
				for (int y = y0; y <= y1; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {
						BlockState fixed = joined(pos, state);
						if (!anySide(fixed) && level.getBlockState(pos.above()).isAir()
								&& !(level.getBlockState(pos.below()).getBlock() instanceof net.minecraft.world.level.block.FenceBlock)) {
							fixed = Blocks.AIR.defaultBlockState();
							rejoinAround(pos);
						}
						if (fixed != state) {
							level.setBlock(pos, fixed, Block.UPDATE_CLIENTS);
						}
					}
				}
			}
		}
	}

	private static boolean anySide(BlockState fence) {
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			if (fence.getValue(propertyOf(d))) {
				return true;
			}
		}
		return false;
	}

	/** Neighbour fences inside the chunk re-joined after a ring fence went. */
	private void rejoinAround(BlockPos pos) {
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			BlockPos n = pos.relative(d);
			BlockState ns = level.getBlockState(n);
			if (inside(n.getX(), n.getY(), n.getZ()) && ns.getBlock() instanceof net.minecraft.world.level.block.FenceBlock) {
				level.setBlock(n, ns.setValue(propertyOf(d.getOpposite()), false), Block.UPDATE_CLIENTS);
			}
		}
	}

	private BlockState joined(BlockPos pos, BlockState state) {
		net.minecraft.world.level.block.FenceBlock fence = (net.minecraft.world.level.block.FenceBlock) state.getBlock();
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			state = state.setValue(propertyOf(d), joins(fence, pos.relative(d), d));
		}
		return state;
	}

	private boolean joins(net.minecraft.world.level.block.FenceBlock fence, BlockPos n, net.minecraft.core.Direction d) {
		BlockState ns = level.getBlockState(n);
		return fence.connectsTo(ns, ns.isFaceSturdy(level, n, d.getOpposite()), d.getOpposite());
	}

	public static net.minecraft.world.level.block.state.properties.BooleanProperty propertyOf(net.minecraft.core.Direction d) {
		return net.minecraft.world.level.block.CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(d);
	}

	public void putIfAir(int x, int y, int z, BlockState state) {
		if (inside(x, y, z) && level.getBlockState(new BlockPos(x, y, z)).isAir()) {
			put(x, y, z, state);
		}
	}

	public void clearAbove(int x, int y, int z, int height) {
		for (int dy = 1; dy <= height; dy++) {
			if (inside(x, y + dy, z) && !level.getBlockState(new BlockPos(x, y + dy, z)).isAir()) {
				put(x, y + dy, z, Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** Builds a whole piece now, chunk by chunk, as worldgen would (commands, tests). */
	public static void buildNow(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.levelgen.structure.StructurePiece piece, BlockPos pivot) {
		BoundingBox box = piece.getBoundingBox();
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(cx, cz);
				BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(),
						chunk.getMaxBlockZ());
				piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, chunk, pivot);
			}
		}
	}
}

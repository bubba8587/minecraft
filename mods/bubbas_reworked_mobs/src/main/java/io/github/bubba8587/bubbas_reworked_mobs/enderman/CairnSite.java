// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * One cairn site: where it is and its full design (specs/endermen.md). Computed from the seed, never stored; how much of
 * it stands is read from the world each time.
 */
public final class CairnSite {
	private final BlockPos origin;
	private final long seed;
	private final Direction pointer;
	private final List<BlockPos> order = new ArrayList<>();
	private final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
	private final BoundingBox box;
	private final @Nullable BlockPos tradeSpot;

	CairnSite(BlockPos origin, long seed, Direction pointer, List<Map.Entry<BlockPos, BlockState>> design) {
		this(origin, seed, pointer, new CairnDesign.Designed(design, null));
	}

	CairnSite(BlockPos origin, long seed, Direction pointer, CairnDesign.Designed designed) {
		List<Map.Entry<BlockPos, BlockState>> design = designed.blocks();
		this.tradeSpot = designed.tradeSpot() == null ? null : origin.offset(designed.tradeSpot());
		this.origin = origin;
		this.seed = seed;
		this.pointer = pointer;
		BoundingBox bounds = new BoundingBox(origin);
		for (Map.Entry<BlockPos, BlockState> entry : design) {
			BlockPos pos = origin.offset(entry.getKey());
			order.add(pos);
			blocks.put(pos.asLong(), entry.getValue());
			bounds.encapsulate(pos);
		}
		this.box = bounds;
	}

	public BlockPos origin() {
		return origin;
	}

	public long seed() {
		return seed;
	}

	/** The horizontal direction of the nearest stronghold (the tallest end rod points this way). */
	public Direction pointer() {
		return pointer;
	}

	/** Where an item lies on the trade pad, or null when the cairn has none. */
	public @Nullable BlockPos tradeSpot() {
		return tradeSpot;
	}

	public BoundingBox box() {
		return box;
	}

	/** Every block of the design, bottom up (absolute positions). */
	public List<BlockPos> order() {
		return order;
	}

	public int size() {
		return order.size();
	}

	/** The design's block at {@code pos}, or null when the cairn has nothing there. */
	public @Nullable BlockState at(BlockPos pos) {
		return box.isInside(pos) ? blocks.get(pos.asLong()) : null;
	}

	/** How many of its blocks stand as designed right now. Loaded chunks only (an unloaded block counts as missing). */
	public int standing(ServerLevel level) {
		int count = 0;
		for (BlockPos pos : order) {
			if (level.isLoaded(pos) && TerrainKinds.matches(level.getBlockState(pos), blocks.get(pos.asLong()))) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Whether this design block is part of the worldgen stub: a natural offering (owner: nothing unnatural a player
	 * could already notice). Everything else, all the End blocks, endermen bring.
	 */
	public static boolean isStubBlock(BlockState state) {
		return state.is(net.minecraft.tags.BlockTags.ENDERMAN_HOLDABLE) && TerrainKinds.isNatural(state)
				&& !state.is(net.minecraft.world.level.block.Blocks.TNT);
	}

	/**
	 * How far the cairn has grown, 0 to 1: the share standing of its End blocks (everything but the offerings, footing
	 * included). What spawning follows: a fresh site is 0, and building speeds up only once it is visibly rising.
	 */
	public float growth(ServerLevel level) {
		int total = 0;
		int standing = 0;
		for (BlockPos pos : order) {
			BlockState want = blocks.get(pos.asLong());
			if (want.is(net.minecraft.tags.BlockTags.ENDERMAN_HOLDABLE)) {
				continue;
			}
			total++;
			if (level.isLoaded(pos) && TerrainKinds.matches(level.getBlockState(pos), want)) {
				standing++;
			}
		}
		return total == 0 ? 1.0F : standing / (float) total;
	}

	/** The share of the design standing right now, 0 to 1. */
	public float finished(ServerLevel level) {
		return order.isEmpty() ? 1.0F : standing(level) / (float) order.size();
	}

	/** The largest horizontal distance of any of its blocks from its origin. */
	public int radius() {
		return Math.max(Math.max(origin.getX() - box.minX(), box.maxX() - origin.getX()), Math.max(origin.getZ() - box.minZ(), box.maxZ() - origin.getZ()));
	}

	/** Complete = every block matches its design (E3: a repaired cairn counts as complete again). */
	public boolean complete(ServerLevel level) {
		return standing(level) == order.size();
	}

	/** Whether the designed block could go into {@code pos} now: the spot is free and something holds it up. */
	public static boolean canPlace(Level level, BlockPos pos, BlockState state) {
		BlockState here = level.getBlockState(pos);
		if (!(here.isAir() || here.canBeReplaced()) || !here.getFluidState().isEmpty() || !state.canSurvive(level, pos)) {
			return false;
		}
		for (Direction d : Direction.values()) {
			BlockState next = level.getBlockState(pos.relative(d));
			if (!next.isAir() && next.getFluidState().isEmpty()) {
				return true;
			}
		}
		// edge neighbours: diagonal arms of a design
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 2) {
						BlockState next = level.getBlockState(pos.offset(dx, dy, dz));
						if (!next.isAir() && next.getFluidState().isEmpty()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * The lowest missing block that none of {@code nearby} is already carrying, or null. Stateless: carried blocks are
	 * matched to missing spots of the same block, bottom up, so each newcomer brings the next block nobody has yet
	 * (builders split the work instead of all bringing the same one). It may not be placeable yet.
	 */
	public @Nullable BlockPos nextUnclaimed(ServerLevel level, List<? extends net.minecraft.world.entity.monster.Enderman> nearby) {
		java.util.Map<net.minecraft.world.level.block.Block, Integer> carried = new java.util.HashMap<>();
		for (net.minecraft.world.entity.monster.Enderman e : nearby) {
			BlockState held = e.getCarriedBlock();
			if (held != null) {
				carried.merge(held.getBlock(), 1, Integer::sum);
			}
		}
		for (BlockPos pos : order) {
			BlockState want = blocks.get(pos.asLong());
			if (!level.isLoaded(pos) || TerrainKinds.matches(level.getBlockState(pos), want)) {
				continue;
			}
			Integer held = carried.get(want.getBlock());
			if (held != null && held > 0) {
				carried.put(want.getBlock(), held - 1);
				continue;
			}
			return pos;
		}
		return null;
	}

	/**
	 * The lowest spot of the design with something in its way that an enderman can lift (grass or dirt where the footing
	 * sinks in, a tree grown into it, a block a player put there), or null.
	 */
	public @Nullable BlockPos nextObstacle(ServerLevel level) {
		for (BlockPos pos : order) {
			BlockState here = level.isLoaded(pos) ? level.getBlockState(pos) : null;
			if (here == null || here.isAir() || here.canBeReplaced() || TerrainKinds.matches(here, blocks.get(pos.asLong()))) {
				continue;
			}
			if (TerrainKinds.clearable(level, pos, here)) {
				return pos;
			}
		}
		return null;
	}

	/** The lowest missing, placeable spot that wants {@code carried}'s block, or null. */
	public @Nullable BlockPos spotFor(ServerLevel level, BlockState carried) {
		for (BlockPos pos : order) {
			BlockState want = blocks.get(pos.asLong());
			if (want.getBlock() == carried.getBlock() && level.isLoaded(pos) && !TerrainKinds.matches(level.getBlockState(pos), want)
					&& canPlace(level, pos, want)) {
				return pos;
			}
		}
		return null;
	}

	/** Whether some missing block of this design is {@code block} (an offering a stray flower could fill, say). */
	public boolean wants(ServerLevel level, Block block) {
		for (BlockPos pos : order) {
			BlockState want = blocks.get(pos.asLong());
			if (want.getBlock() == block && level.isLoaded(pos) && !TerrainKinds.matches(level.getBlockState(pos), want)) {
				return true;
			}
		}
		return false;
	}

	/** Places the whole design at once (the build command and tests). */
	public void buildAll(ServerLevel level) {
		for (BlockPos pos : order) {
			level.setBlock(pos, blocks.get(pos.asLong()), Block.UPDATE_CLIENTS);
		}
		for (BlockPos pos : order) {
			BlockState placed = level.getBlockState(pos);
			level.setBlock(pos, Block.updateFromNeighbourShapes(placed, level, pos), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public String toString() {
		return "cairn at " + origin.toShortString() + " (" + order.size() + " blocks, points " + pointer.getSerializedName() + ")";
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import io.github.bubba8587.bubbas_reworked_mobs.world.NaturalSpawns;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Finding a spot "halfway behind a wall" (specs/creepers.md). Works from the watched entity's eyes: a spot is a floor
 * cell where, sliding sideways across the line of sight, the view of the creeper's eyes goes from clear to blocked
 * (the edge of a tree trunk, a wall's end, a boulder, any collision shape: leaves count, like vanilla line of sight).
 * The creeper stands with its middle just on the open side of that edge (half hidden, eyes peeking) or slides past it
 * (about 95 % hidden). Only collision raycasts; at most a few dozen per search, and searches are throttled.
 */
public final class CoverFinder {
	/** Sideways offsets sampled across the line of sight, either side of the cell centre. */
	private static final double[] SAMPLES = {-0.9, -0.6, -0.3, 0.0, 0.3, 0.6, 0.9};
	private static final int BISECTIONS = 3;

	/** A watch spot: the floor cell, the half-hidden and the hidden position, and which way is into cover. */
	public record PeekSpot(BlockPos cell, Vec3 half, Vec3 hidden) {
	}

	private CoverFinder() {
	}

	/** Whether a creeper can stand in this cell: something to stand on, room for its body, no liquid. */
	public static boolean standable(Level level, BlockPos cell) {
		BlockPos below = cell.below();
		return !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
				&& level.getBlockState(cell).getCollisionShape(level, cell).isEmpty()
				&& level.getBlockState(cell.above()).getCollisionShape(level, cell.above()).isEmpty()
				&& level.getFluidState(cell).isEmpty();
	}

	/** The highest standable cell in this column within dy of {@code y}, or null. */
	public static @Nullable BlockPos standableIn(Level level, int x, int y, int z, int up, int down) {
		return NaturalSpawns.highest(x, y, z, up, down, pos -> standable(level, pos));
	}

	/** Clear collision line from {@code from} to {@code to}. */
	public static boolean clear(Level level, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
				.getType() == HitResult.Type.MISS;
	}

	/**
	 * The peek spot at this cell as seen from {@code viewer} (an eye position), or null when there is no cover edge
	 * across the cell, the hidden side isn't deep enough, or the creeper wouldn't fit or stand there.
	 */
	public static @Nullable PeekSpot evaluate(Level level, EntityDimensions body, BlockPos cell, Vec3 viewer) {
		Vec3 base = Vec3.atBottomCenterOf(cell);
		double dx = base.x - viewer.x;
		double dz = base.z - viewer.z;
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0) {
			return null;
		}
		Vec3 perp = new Vec3(-dz / len, 0.0, dx / len);
		double eye = body.eyeHeight();
		boolean[] seen = new boolean[SAMPLES.length];
		for (int i = 0; i < SAMPLES.length; i++) {
			seen[i] = clear(level, viewer, base.add(perp.scale(SAMPLES[i])).add(0.0, eye, 0.0));
		}
		// The visible-to-blocked change nearest the centre decides the edge and which way is into cover.
		int best = -1;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i + 1 < SAMPLES.length; i++) {
			if (seen[i] != seen[i + 1]) {
				double mid = Math.abs((SAMPLES[i] + SAMPLES[i + 1]) / 2.0);
				if (mid < bestDistance) {
					bestDistance = mid;
					best = i;
				}
			}
		}
		if (best < 0) {
			return null;
		}
		// Work along "into cover": u grows toward the blocked side.
		double sign = seen[best] ? 1.0 : -1.0;
		Vec3 into = perp.scale(sign);
		double open = SAMPLES[seen[best] ? best : best + 1] * sign;
		double shut = SAMPLES[seen[best] ? best + 1 : best] * sign;
		for (int i = 0; i < BISECTIONS; i++) {
			double mid = (open + shut) / 2.0;
			if (clear(level, viewer, base.add(into.scale(mid)).add(0.0, eye, 0.0))) {
				open = mid;
			} else {
				shut = mid;
			}
		}
		double edge = (open + shut) / 2.0;
		Vec3 half = base.add(into.scale(edge - CreeperTuning.HALF_HIDE_INSET));
		Vec3 hidden = base.add(into.scale(edge + CreeperTuning.HIDDEN_DEPTH));
		// The far side of the hidden body must be in cover too, or the cover is too thin to hide behind.
		Vec3 farEdge = hidden.add(into.scale(body.width() / 2.0)).add(0.0, eye, 0.0);
		if (clear(level, viewer, farEdge)) {
			return null;
		}
		if (!fits(level, body, half) || !fits(level, body, hidden)) {
			return null;
		}
		return new PeekSpot(cell, half, hidden);
	}

	/**
	 * Cheap pre-check before any raycast: something solid at head height within 2 cells, on the viewer's side of the
	 * cell (cover sits between the creeper and what it watches).
	 */
	private static boolean coverNextTo(Level level, BlockPos cell, Vec3 viewer) {
		double vx = viewer.x - (cell.getX() + 0.5);
		double vz = viewer.z - (cell.getZ() + 0.5);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx * vx + dz * vz <= 0.0) {
					continue;
				}
				pos.set(cell.getX() + dx, cell.getY() + 1, cell.getZ() + dz);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean fits(Level level, EntityDimensions body, Vec3 feet) {
		AABB box = body.makeBoundingBox(feet).deflate(1.0E-3);
		if (!level.noCollision(box)) {
			return false;
		}
		BlockPos below = BlockPos.containing(feet.x, feet.y - 0.5, feet.z);
		return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
	}

	/**
	 * The best peek spot near {@code around} from which {@code viewer} is between {@code minDistance} and
	 * {@code maxDistance} away (horizontally), preferring {@code preferred} distance and short walks; or null.
	 */
	public static @Nullable PeekSpot find(Level level, EntityDimensions body, BlockPos around, Vec3 viewer,
			double minDistance, double maxDistance, double preferred) {
		record Candidate(BlockPos cell, double score) {
		}
		// Random cells (owner, 2026-11-16: creepers seek out walls much farther off; a full grid that wide costs too
		// much): half within SPOT_SEARCH_RADIUS of the creeper, half on its side of the ring of wanted distances round
		// the viewer (within 75 degrees of the creeper's bearing, so it never crosses in front of what it watches).
		List<Candidate> candidates = new ArrayList<>();
		LongSet tried = new LongOpenHashSet();
		RandomSource random = level.getRandom();
		int r = CreeperTuning.SPOT_SEARCH_RADIUS;
		double bearing = Math.atan2(around.getZ() + 0.5 - viewer.z, around.getX() + 0.5 - viewer.x);
		double maxWalkSqr = CreeperTuning.SPOT_MAX_WALK * CreeperTuning.SPOT_MAX_WALK;
		int g = CreeperTuning.SPOT_NEAR_GRID;
		int grid = (2 * g + 1) * (2 * g + 1);
		for (int i = 0; i < grid + CreeperTuning.SPOT_SEARCH_SAMPLES; i++) {
			int x;
			int z;
			if (i < grid) {
				// Every cell close by first (a small tree beside it is never missed).
				x = around.getX() + i % (2 * g + 1) - g;
				z = around.getZ() + i / (2 * g + 1) - g;
			} else if (i % 2 == 0) {
				x = around.getX() + random.nextInt(2 * r + 1) - r;
				z = around.getZ() + random.nextInt(2 * r + 1) - r;
			} else {
				double angle = bearing + Math.toRadians((random.nextDouble() * 2.0 - 1.0) * 75.0);
				double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
				x = Mth.floor(viewer.x + Math.cos(angle) * distance);
				z = Mth.floor(viewer.z + Math.sin(angle) * distance);
			}
			if (!tried.add(BlockPos.asLong(x, 0, z))) {
				continue;
			}
			double dx = x - around.getX();
			double dz = z - around.getZ();
			double walkSqr = dx * dx + dz * dz;
			double cx = x + 0.5 - viewer.x;
			double cz = z + 0.5 - viewer.z;
			double toViewer = Math.sqrt(cx * cx + cz * cz);
			if (toViewer < minDistance || toViewer > maxDistance || walkSqr > maxWalkSqr || !level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos cell = standableIn(level, x, around.getY(), z, 3, 3);
			if (cell == null || !coverNextTo(level, cell, viewer)) {
				continue;
			}
			candidates.add(new Candidate(cell, Math.abs(toViewer - preferred) + 0.3 * Math.sqrt(walkSqr)));
		}
		candidates.sort(Comparator.comparingDouble(Candidate::score));
		int evaluated = 0;
		for (Candidate candidate : candidates) {
			if (evaluated++ >= CreeperTuning.SPOT_MAX_EVALUATIONS) {
				break;
			}
			PeekSpot spot = evaluate(level, body, candidate.cell(), viewer);
			if (spot != null) {
				return spot;
			}
		}
		return null;
	}
}

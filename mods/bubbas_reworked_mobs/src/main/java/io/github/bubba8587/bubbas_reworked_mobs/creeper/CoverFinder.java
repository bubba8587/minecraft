// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import io.github.bubba8587.bubbas_reworked_mobs.world.NaturalSpawns;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Finding a spot "halfway behind a wall" (specs/creepers.md). Works from the watched entity's eyes: a spot is a floor
 * cell where, sliding sideways across the line of sight, the view of the creeper's eyes goes from clear to blocked
 * (the edge of a tree trunk, a wall's end, a window frame, a boulder; leaves block the view, glass and panes don't).
 * The creeper stands with its middle just on the open side of that edge (half hidden, eyes peeking) or slides past it
 * (about 95 % hidden). Only raycasts and block lookups; searches are throttled.
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

	/** Clear line of sight from {@code from} to {@code to}: blocked by what the eye can't see through (glass and panes are clear, leaves aren't). */
	public static boolean clear(Level level, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
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
		int last = SAMPLES.length - 1;
		seen[0] = clear(level, viewer, base.add(perp.scale(SAMPLES[0])).add(0.0, eye, 0.0));
		seen[last] = clear(level, viewer, base.add(perp.scale(SAMPLES[last])).add(0.0, eye, 0.0));
		// Quick reject: both sides alike means no edge across the cell.
		if (seen[0] == seen[last]) {
			return null;
		}
		for (int i = 1; i < last; i++) {
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
		List<PeekSpot> spots = findAll(level, body, around, viewer, minDistance, maxDistance, preferred, 1);
		return spots.isEmpty() ? null : spots.getFirst();
	}

	/** Up to {@code limit} peek spots as {@link #find} picks them, best first. */
	public static List<PeekSpot> findAll(Level level, EntityDimensions body, BlockPos around, Vec3 viewer,
			double minDistance, double maxDistance, double preferred, int limit) {
		record Candidate(BlockPos cell, double score, boolean edge) {
		}
		List<PeekSpot> found = new ArrayList<>();
		if (minDistance > maxDistance) {
			return found;
		}
		RandomSource random = level.getRandom();
		int r = CreeperTuning.SPOT_SEARCH_RADIUS;
		double bearing = Math.atan2(around.getZ() + 0.5 - viewer.z, around.getX() + 0.5 - viewer.x);
		int g = CreeperTuning.SPOT_NEAR_GRID;
		int grid = (2 * g + 1) * (2 * g + 1);
		LongList cells = new LongArrayList();
		// Cells just behind each cover edge the sweep finds.
		for (Vec3 point : sweep(level, viewer, bearing, maxDistance)) {
			cells.add(BlockPos.asLong(Mth.floor(point.x), 0, Mth.floor(point.z)));
		}
		int edgeCells = cells.size();
		for (int i = 0; i < grid + CreeperTuning.SPOT_SEARCH_SAMPLES; i++) {
			if (i < grid) {
				// Every cell close by.
				cells.add(BlockPos.asLong(around.getX() + i % (2 * g + 1) - g, 0, around.getZ() + i / (2 * g + 1) - g));
			} else if (i % 2 == 0) {
				cells.add(BlockPos.asLong(around.getX() + random.nextInt(2 * r + 1) - r, 0, around.getZ() + random.nextInt(2 * r + 1) - r));
			} else {
				// On the creeper's side of the ring of wanted distances round the viewer.
				double angle = bearing + Math.toRadians((random.nextDouble() * 2.0 - 1.0) * CreeperTuning.SWEEP_HALF_ANGLE);
				double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
				cells.add(BlockPos.asLong(Mth.floor(viewer.x + Math.cos(angle) * distance), 0, Mth.floor(viewer.z + Math.sin(angle) * distance)));
			}
		}
		List<Candidate> candidates = new ArrayList<>();
		LongSet tried = new LongOpenHashSet();
		double maxWalkSqr = CreeperTuning.SPOT_MAX_WALK * CreeperTuning.SPOT_MAX_WALK;
		for (int i = 0; i < cells.size(); i++) {
			long packed = cells.getLong(i);
			if (!tried.add(packed)) {
				continue;
			}
			int x = BlockPos.getX(packed);
			int z = BlockPos.getZ(packed);
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
			candidates.add(new Candidate(cell, Math.abs(toViewer - preferred) + 0.3 * Math.sqrt(walkSqr), i < edgeCells));
		}
		candidates.sort(Comparator.comparingDouble(Candidate::score));
		// Separate raycast budgets for edge cells and the rest, so neither crowds the other out.
		int[] budget = {CreeperTuning.SPOT_EDGE_EVALUATIONS, CreeperTuning.SPOT_MAX_EVALUATIONS};
		for (Candidate candidate : candidates) {
			if (found.size() >= limit || budget[0] + budget[1] == 0) {
				break;
			}
			int group = candidate.edge() ? 0 : 1;
			if (budget[group] == 0) {
				continue;
			}
			budget[group]--;
			PeekSpot spot = evaluate(level, body, candidate.cell(), viewer);
			if (spot != null) {
				found.add(spot);
			}
		}
		return found;
	}

	/** Whether the spot's hidden place is out of {@code viewer}'s sight (its eyes there are behind the cover). */
	public static boolean hiddenOutOfSight(Level level, EntityDimensions body, PeekSpot spot, Vec3 viewer) {
		return !clear(level, viewer, spot.hidden().add(0.0, body.eyeHeight(), 0.0));
	}

	/**
	 * Cover edges seen from {@code viewer}: level rays fanned over the creeper's side ({@code bearing}); where two
	 * neighbouring rays end more than a block apart there is an edge, pinned down with a few more rays. Returns points
	 * just behind each edge (where a creeper could peek round it).
	 */
	private static List<Vec3> sweep(Level level, Vec3 viewer, double bearing, double maxDistance) {
		int n = CreeperTuning.SWEEP_RAYS;
		double half = Math.toRadians(CreeperTuning.SWEEP_HALF_ANGLE);
		double step = 2.0 * half / (n - 1);
		double start = bearing - half + (level.getRandom().nextDouble() - 0.5) * step;
		double[] hits = new double[n];
		for (int i = 0; i < n; i++) {
			hits[i] = ray(level, viewer, start + i * step, maxDistance);
		}
		List<Vec3> points = new ArrayList<>();
		for (int i = 0; i + 1 < n; i++) {
			if (Math.abs(hits[i] - hits[i + 1]) <= 1.0) {
				continue;
			}
			boolean nearFirst = hits[i] < hits[i + 1];
			double near = start + (nearFirst ? i : i + 1) * step;
			double far = start + (nearFirst ? i + 1 : i) * step;
			double nearHit = Math.min(hits[i], hits[i + 1]);
			for (int b = 0; b < CreeperTuning.SWEEP_BISECTIONS; b++) {
				double mid = (near + far) / 2.0;
				double hit = ray(level, viewer, mid, maxDistance);
				if (Math.abs(hit - nearHit) <= 1.0) {
					near = mid;
					nearHit = Math.min(nearHit, hit);
				} else {
					far = mid;
				}
			}
			for (double behind : new double[] {0.7, 1.5}) {
				double d = nearHit + behind;
				if (d > maxDistance) {
					continue;
				}
				for (double angle : new double[] {near, far}) {
					points.add(new Vec3(viewer.x + Math.cos(angle) * d, viewer.y, viewer.z + Math.sin(angle) * d));
				}
			}
		}
		return points;
	}

	/** How far a level ray from {@code viewer} along {@code angle} gets before something blocks the view (maxDistance + 2 if nothing does). */
	private static double ray(Level level, Vec3 viewer, double angle, double maxDistance) {
		Vec3 to = viewer.add(Math.cos(angle) * maxDistance, 0.0, Math.sin(angle) * maxDistance);
		BlockHitResult hit = level.clip(new ClipContext(viewer, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS ? maxDistance + 2.0 : hit.getLocation().subtract(viewer).horizontalDistance();
	}
}

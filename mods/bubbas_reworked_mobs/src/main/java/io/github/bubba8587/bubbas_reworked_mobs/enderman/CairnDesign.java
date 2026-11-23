// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * The design of one cairn (specs/endermen.md, "Cairn look"), fixed by its seed. Cairns come in several forms
 * ({@link Form}, owner 2026-11-22: "as much variety as you like"): twisting chorus-like spires, thickets, tall tiered
 * columns, arches, canopies, rings of standing posts and leaning stalks, each on an end stone footing of varying shape
 * and size, with end-rod 'flowers', end-city touches (purpur stairs and slabs, magenta, purple or pink glass) and
 * overworld offerings at its foot that depend on the biome, laid out in a ring, a cluster or a line. The tallest end
 * rod, at the top of the main trunk, lies on its side pointing toward the nearest stronghold. Structural blocks are
 * only ones that never generate in the overworld (no obsidian, E3); offerings are vanilla enderman-holdable blocks by
 * design. No banners (owner).
 *
 * <p>Pure function of its inputs; positions are relative to the cairn origin (the air block just above the ground at
 * its centre, y 0; the footing is sunk into the ground at y -1).
 */
public final class CairnDesign {
	/** What grows around a cairn's foot, picked from the biome at its centre. */
	public enum Offerings {
		TEMPERATE, FLOWERY, JUNGLE, SWAMP, SANDY, BADLANDS, SNOWY, DARK, MUSHROOM
	}

	/**
	 * The cairn's overall form, picked from its seed. Every form has a main trunk whose top carries the stronghold
	 * pointer, the tallest end rod.
	 */
	public enum Form {
		/** The first design: 1-3 chorus-like trunks stepping sideways, branches ending in end-rod flowers. */
		CHORUS,
		/** A thicket: 3-5 short chorus trunks over a wide footing. */
		GROVE,
		/** One tall straight column with tiers of short arms and rods, on a stepped footing. */
		SPIRE,
		/** Two trunks joined at the top by a purpur span; a short stalk above the middle carries the pointer. */
		ARCH,
		/** A thick 2x2 trunk opening into a crown of radiating branches. */
		CANOPY,
		/** A ring of standing posts round a small spire. */
		HENGE,
		/** One trunk that leans far to one side as it climbs, and a short one leaning the other way. */
		LEANING
	}

	/** Cumulative weights of the forms, in Form order. */
	private static final float[] FORM_WEIGHTS = { 0.26F, 0.40F, 0.54F, 0.66F, 0.78F, 0.89F, 1.0F };

	private static final Direction[] HORIZONTAL = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

	private final RandomSource random;
	private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
	private final IntBinaryOperator groundAt;
	/** No end rod but the pointer may be this high, so the pointer is always the tallest. */
	private int rodCeiling;
	private BlockPos pointerRod;
	/** The glass this cairn hangs (magenta, purple or pink), picked once. */
	private BlockState glass;
	/** How far out (blocks) the structure reaches, so offerings stand beyond it. */
	private int reach;

	/** A designed cairn: its blocks bottom up, and where its trade pad's item lies (null when no pad fits). */
	public record Designed(List<Map.Entry<BlockPos, BlockState>> blocks, @org.jspecify.annotations.Nullable BlockPos tradeSpot) {
	}

	private static final long PAD_SALT = 0x7EAD_E0AD_5A1EL;
	private final long seed;
	private @org.jspecify.annotations.Nullable BlockPos tradeSpot;

	private CairnDesign(long seed, IntBinaryOperator groundAt) {
		this.seed = seed;
		this.random = new LegacyRandomSource(seed);
		this.groundAt = groundAt;
	}

	/**
	 * Designs a cairn.
	 *
	 * @param seed the site's seed
	 * @param offerings the offering palette (from the biome)
	 * @param pointer the horizontal direction of the nearest stronghold
	 * @param groundAt for a relative (x, z), the relative y of the top ground block there (-1 on the flat centre)
	 * @return every block, relative to the origin, in build order (bottom up)
	 */
	public static List<Map.Entry<BlockPos, BlockState>> design(long seed, Offerings offerings, Direction pointer, IntBinaryOperator groundAt) {
		return designed(seed, offerings, pointer, groundAt).blocks();
	}

	/** Designs a cairn with its trade pad. */
	public static Designed designed(long seed, Offerings offerings, Direction pointer, IntBinaryOperator groundAt) {
		CairnDesign design = new CairnDesign(seed, groundAt);
		design.build(offerings, pointer);
		List<Map.Entry<BlockPos, BlockState>> list = new ArrayList<>(design.blocks.entrySet());
		// Stable sort: bottom up
		list.sort(Comparator.comparingInt(e -> e.getKey().getY()));
		return new Designed(list, design.tradeSpot);
	}

	/** The form a seed picks (tests and previews): the first roll of the design. */
	public static Form formOf(long seed) {
		return new CairnDesign(seed, (x, z) -> -1).pickForm();
	}

	private Form pickForm() {
		float roll = random.nextFloat();
		for (int i = 0; i < FORM_WEIGHTS.length; i++) {
			if (roll < FORM_WEIGHTS[i]) {
				return Form.values()[i];
			}
		}
		return Form.CHORUS;
	}

	private void build(Offerings offerings, Direction pointer) {
		Form form = pickForm();
		glass = pick(Blocks.STAINED_GLASS.magenta().defaultBlockState(), Blocks.STAINED_GLASS.purple().defaultBlockState(),
				Blocks.STAINED_GLASS.pink().defaultBlockState());
		int height = EndermanTuning.CAIRN_MIN_HEIGHT + random.nextInt(EndermanTuning.CAIRN_MAX_HEIGHT - EndermanTuning.CAIRN_MIN_HEIGHT + 1);
		rodCeiling = height - 2;
		int footing = switch (form) {
			case CHORUS -> random.nextFloat() < 0.45F ? 2 : 1;
			case GROVE, ARCH -> 2 + random.nextInt(2);
			case SPIRE, HENGE -> 1;
			case CANOPY -> 2;
			case LEANING -> 1 + random.nextInt(2);
		};
		reach = footing;
		buildFooting(footing);
		switch (form) {
			case CHORUS -> chorus(height, footing, pointer, 1 + (random.nextFloat() < 0.55F ? 1 : 0) + (random.nextFloat() < 0.2F ? 1 : 0), 2);
			case GROVE -> chorus(Math.max(7, height - 4), footing, pointer, 3 + random.nextInt(3), 1);
			case SPIRE -> spire(height + 3 + random.nextInt(4), pointer);
			case ARCH -> arch(Math.max(6, height - 3 - random.nextInt(3)), footing, pointer);
			case CANOPY -> canopy(Math.max(6, height - 4), pointer);
			case HENGE -> henge(Math.max(7, height - 3), pointer);
			case LEANING -> leaning(height, footing, pointer);
		}
		if (pointerRod != null) {
			// The pointer stays the tallest rod whatever the trunks did.
			int top = pointerRod.getY();
			blocks.entrySet().removeIf(e -> e.getValue().is(Blocks.END_ROD) && e.getKey().getY() >= top && !e.getKey().equals(pointerRod));
		}
		for (BlockPos pos : blocks.keySet()) {
			reach = Math.max(reach, Math.max(Math.abs(pos.getX()), Math.abs(pos.getZ())));
		}
		buildRim(footing);
		buildOfferings(offerings, reach);
		buildTradePad(reach);
	}

	// --- forms -------------------------------------------------------------------------------------------

	/** Several chorus trunks; the first is the main one. {@code wander}: how far a trunk may step from its start. */
	private void chorus(int height, int footing, Direction pointer, int trunks, int wander) {
		boolean glassUsed = false;
		List<BlockPos> starts = new ArrayList<>();
		starts.add(BlockPos.ZERO);
		for (int i = 1; i < trunks; i++) {
			for (int tries = 0; tries < 8; tries++) {
				BlockPos start = new BlockPos(random.nextInt(2 * footing + 1) - footing, 0, random.nextInt(2 * footing + 1) - footing);
				if (starts.stream().noneMatch(s -> s.distManhattan(start) < 2)) {
					starts.add(start);
					break;
				}
			}
		}
		for (int i = 0; i < starts.size(); i++) {
			boolean main = i == 0;
			int h = main ? height : Math.max(4, (int) (height * (0.4F + random.nextFloat() * 0.35F)));
			glassUsed |= growTrunk(starts.get(i), h, main, pointer, !glassUsed, wander, null);
		}
	}

	/** A tall straight column; every few blocks a knuckle sends short arms out with rods. */
	private void spire(int height, Direction pointer) {
		rodCeiling = height - 2;
		// A stepped footing: end stone brick stairs round the base, stepping down outward.
		for (Direction d : HORIZONTAL) {
			putIfFree(new BlockPos(d.getStepX() * 2, -1, d.getStepZ() * 2),
					Blocks.END_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, d.getOpposite()));
		}
		int tier = 3 + random.nextInt(2);
		int y = 0;
		for (; y < height - 1; y++) {
			if (y > 1 && y % tier == 0) {
				put(new BlockPos(0, y, 0), Blocks.PURPUR_BLOCK.defaultBlockState());
				int arms = 2 + random.nextInt(3);
				int offset = random.nextInt(4);
				for (int a = 0; a < arms; a++) {
					Direction d = HORIZONTAL[(offset + a) % 4];
					BlockPos arm = new BlockPos(d.getStepX(), y, d.getStepZ());
					putIfFree(arm, Blocks.PURPUR_STAIRS.defaultBlockState().setValue(StairBlock.FACING, d.getOpposite()).setValue(StairBlock.HALF, Half.TOP));
					rod(arm.above());
				}
			} else {
				put(new BlockPos(0, y, 0), pillar(Direction.Axis.Y));
			}
		}
		BlockPos top = new BlockPos(0, y, 0);
		put(top, Blocks.PURPUR_BLOCK.defaultBlockState());
		placePointer(top, pointer);
	}

	/** Two trunks joined across the top; the pointer on a short stalk above the middle. */
	private void arch(int height, int half, Direction pointer) {
		Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction negative = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
		BlockPos a = BlockPos.ZERO.relative(negative, half);
		BlockPos b = BlockPos.ZERO.relative(negative.getOpposite(), half);
		for (BlockPos leg : List.of(a, b)) {
			for (int y = 0; y < height; y++) {
				put(leg.atY(y), pillar(Direction.Axis.Y));
			}
			put(leg.atY(height), Blocks.PURPUR_BLOCK.defaultBlockState());
		}
		// The span: purpur pillars lying along the top, upside-down stairs where it meets each leg.
		for (int i = -half + 1; i <= half - 1; i++) {
			BlockPos at = BlockPos.ZERO.relative(negative.getOpposite(), i).atY(height);
			boolean end = Math.abs(i) == half - 1 && half > 1;
			Direction toward = i < 0 ? negative : negative.getOpposite();
			put(at, end ? Blocks.PURPUR_STAIRS.defaultBlockState().setValue(StairBlock.FACING, toward).setValue(StairBlock.HALF, Half.TOP) : pillar(axis));
		}
		if (random.nextFloat() < 0.6F) {
			put(new BlockPos(0, height - 1, 0), glass);
		}
		int stalk = 2 + random.nextInt(2);
		for (int y = height + 1; y < height + stalk; y++) {
			put(new BlockPos(0, y, 0), pillar(Direction.Axis.Y));
		}
		BlockPos top = new BlockPos(0, height + stalk, 0);
		put(top, Blocks.PURPUR_BLOCK.defaultBlockState());
		rodCeiling = top.getY() - 1;
		// Rods on the legs' tops, below the pointer.
		rod(a.atY(height + 1));
		rod(b.atY(height + 1));
		placePointer(top, pointer);
	}

	/** A 2x2 trunk opening into a crown of branches. */
	private void canopy(int height, Direction pointer) {
		for (int y = 0; y < height; y++) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					put(new BlockPos(dx, y, dz), pillar(Direction.Axis.Y));
				}
			}
		}
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				put(new BlockPos(dx, height, dz), Blocks.PURPUR_BLOCK.defaultBlockState());
			}
		}
		BlockPos top = new BlockPos(0, height + 2, 0);
		rodCeiling = top.getY() - 1;
		boolean glassUsed = false;
		int offset = random.nextInt(8);
		int branches = 5 + random.nextInt(4);
		int[][] dirs = { { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 }, { 0, -1 }, { 1, -1 } };
		for (int i = 0; i < branches; i++) {
			int[] d = dirs[(offset + i) % 8];
			BlockPos at = new BlockPos(d[0] > 0 ? 1 : 0, height, d[1] > 0 ? 1 : 0);
			int length = 2 + random.nextInt(2);
			for (int l = 0; l < length; l++) {
				at = at.offset(d[0], 0, d[1]);
				if (!putIfFree(at, l == length - 1 ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.PURPUR_SLAB.defaultBlockState())) {
					break;
				}
			}
			rod(at.above());
			if (!glassUsed && random.nextFloat() < 0.35F && !blocks.containsKey(at.below())) {
				put(at.below(), glass);
				glassUsed = true;
			}
		}
		put(top.below(), pillar(Direction.Axis.Y));
		put(top, Blocks.PURPUR_BLOCK.defaultBlockState());
		placePointer(top, pointer);
	}

	/** A ring of end stone brick posts capped with purpur round a small chorus spire. */
	private void henge(int height, Direction pointer) {
		int radius = 3 + random.nextInt(2);
		int posts = 5 + random.nextInt(4);
		double start = random.nextDouble() * Math.PI * 2.0;
		for (int i = 0; i < posts; i++) {
			double a = start + Math.PI * 2.0 * i / posts;
			int x = (int) Math.round(Math.cos(a) * radius);
			int z = (int) Math.round(Math.sin(a) * radius);
			int ground = groundAt.applyAsInt(x, z);
			int tall = 2 + random.nextInt(2);
			boolean slab = random.nextBoolean();
			boolean withRod = random.nextFloat() < 0.4F;
			if (Math.abs(ground + 1) > 1) {
				continue;
			}
			BlockPos base = new BlockPos(x, ground + 1, z);
			for (int y = 0; y < tall; y++) {
				putIfFree(base.above(y), y == 0 ? Blocks.END_STONE_BRICKS.defaultBlockState() : Blocks.END_STONE_BRICK_WALL.defaultBlockState());
			}
			BlockPos cap = base.above(tall);
			putIfFree(cap, slab ? Blocks.PURPUR_SLAB.defaultBlockState() : Blocks.PURPUR_BLOCK.defaultBlockState());
			if (withRod) {
				rod(cap.above());
			}
		}
		growTrunk(BlockPos.ZERO, height, true, pointer, true, 1, null);
	}

	/** One trunk leaning toward a side as it climbs, and a short second one leaning away. */
	private void leaning(int height, int footing, Direction pointer) {
		Direction lean = HORIZONTAL[random.nextInt(4)];
		growTrunk(BlockPos.ZERO, height, true, pointer, true, 4, lean);
		growTrunk(BlockPos.ZERO.relative(lean.getOpposite(), footing), Math.max(4, height / 2), false, pointer, false, 2, lean.getOpposite());
	}

	/** The stronghold pointer: the tallest end rod, on its side at the top block, facing the stronghold. */
	private void placePointer(BlockPos top, Direction pointer) {
		BlockState rod = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, pointer);
		if (putIfFree(top.relative(pointer), rod)) {
			pointerRod = top.relative(pointer);
		} else if (putIfFree(top.above(), rod)) {
			pointerRod = top.above();
		}
	}

	// --- footing and rim ---------------------------------------------------------------------------------

	/** The footing, sunk into the ground: round, square, a cross or a diamond, of end stone bricks with end stone and some purpur. */
	private void buildFooting(int radius) {
		int shape = random.nextInt(4);
		float purpur = random.nextFloat() < 0.3F ? 0.15F : 0.0F;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				boolean in = switch (shape) {
					case 0 -> dx * dx + dz * dz <= radius * radius + 1;
					case 1 -> true;
					case 2 -> dx == 0 || dz == 0 || Math.abs(dx) + Math.abs(dz) <= Math.max(1, radius - 1);
					default -> Math.abs(dx) + Math.abs(dz) <= radius + 1;
				};
				if (!in) {
					continue;
				}
				float roll = random.nextFloat();
				put(new BlockPos(dx, -1, dz), roll < purpur ? Blocks.PURPUR_BLOCK.defaultBlockState()
						: roll < purpur + 0.25F ? Blocks.END_STONE.defaultBlockState() : Blocks.END_STONE_BRICKS.defaultBlockState());
			}
		}
	}

	/** Low slabs around the footing's rim, like a worn step. */
	private void buildRim(int radius) {
		for (Direction side : HORIZONTAL) {
			if (random.nextFloat() < 0.55F) {
				int along = random.nextInt(2 * radius + 1) - radius;
				BlockPos at = new BlockPos(side.getStepX() * (radius + 1) + (side.getAxis() == Direction.Axis.Z ? along : 0), 0,
						side.getStepZ() * (radius + 1) + (side.getAxis() == Direction.Axis.X ? along : 0));
				if (groundAt.applyAsInt(at.getX(), at.getZ()) == -1) {
					putIfFree(at, random.nextBoolean() ? Blocks.PURPUR_SLAB.defaultBlockState() : Blocks.END_STONE_BRICK_SLAB.defaultBlockState());
				}
			}
		}
	}

	// --- trunks and branches -----------------------------------------------------------------------------

	/**
	 * A chorus-like stalk: pillar runs that step sideways at purpur-block knuckles, never more than {@code wander}
	 * from its start, stepping toward {@code lean} when given; returns whether it hung the glass.
	 */
	private boolean growTrunk(BlockPos start, int height, boolean main, Direction pointer, boolean mayHangGlass, int wander, Direction lean) {
		boolean hungGlass = false;
		BlockPos column = start;
		Direction last = null;
		int y = 0;
		while (true) {
			int run = 2 + random.nextInt(2);
			for (int i = 0; i < run && y < height - 1; i++, y++) {
				if (!putIfFree(column.atY(y), pillar(Direction.Axis.Y))) {
					return hungGlass;
				}
			}
			BlockPos knuckle = column.atY(y);
			if (!putIfFree(knuckle, Blocks.PURPUR_BLOCK.defaultBlockState())) {
				return hungGlass;
			}
			Direction step = y >= height - 1 ? null : pickStep(knuckle, last, start, wander, lean);
			if (step == null) {
				if (main) {
					// The tallest end rod, on its side at the very top: points toward the nearest stronghold.
					rodCeiling = Math.min(rodCeiling, y - 1);
					placePointer(knuckle, pointer);
				} else {
					rod(knuckle.above());
				}
				return hungGlass;
			}
			if (y > 2 && random.nextFloat() < 0.6F) {
				hungGlass |= branch(knuckle, step, mayHangGlass && !hungGlass);
			}
			// The knuckle: stairs upside down beside the block, then the next run climbs from the stairs.
			BlockPos next = knuckle.relative(step);
			if (!putIfFree(next, Blocks.PURPUR_STAIRS.defaultBlockState().setValue(StairBlock.FACING, step.getOpposite()).setValue(StairBlock.HALF, Half.TOP))) {
				return hungGlass;
			}
			column = next;
			last = step;
			y++;
		}
	}

	/** A sideways step that doesn't double back, stays within {@code wander} of the trunk's start and lands on free blocks. */
	private Direction pickStep(BlockPos knuckle, Direction last, BlockPos start, int wander, Direction lean) {
		int offset = random.nextInt(4);
		if (lean != null && random.nextFloat() < 0.7F) {
			offset = java.util.Arrays.asList(HORIZONTAL).indexOf(lean); // try the lean first
		}
		for (int i = 0; i < 4; i++) {
			Direction d = HORIZONTAL[(offset + i) % 4];
			if (last != null && d == last.getOpposite()) {
				continue;
			}
			BlockPos next = knuckle.relative(d);
			if (Math.abs(next.getX() - start.getX()) > wander || Math.abs(next.getZ() - start.getZ()) > wander) {
				continue;
			}
			if (!blocks.containsKey(next) && !blocks.containsKey(next.above()) && !blocks.containsKey(next.above(2))) {
				return d;
			}
		}
		return null;
	}

	/** A short arm out of a knuckle, rising to an end-rod flower; maybe a window of glass hanging under it. */
	private boolean branch(BlockPos knuckle, Direction step, boolean mayHangGlass) {
		int offset = random.nextInt(4);
		for (int i = 0; i < 4; i++) {
			Direction d = HORIZONTAL[(offset + i) % 4];
			if (d == step) {
				continue;
			}
			int length = 1 + random.nextInt(2);
			BlockPos arm = knuckle.relative(d);
			BlockPos end = knuckle.relative(d, length);
			if (blocks.containsKey(arm) || blocks.containsKey(end) || blocks.containsKey(end.above()) || blocks.containsKey(end.above(2))) {
				continue;
			}
			if (length == 2) {
				put(arm, pillar(d.getAxis()));
			}
			put(end, Blocks.PURPUR_BLOCK.defaultBlockState());
			BlockPos top = end;
			int rise = random.nextInt(3);
			for (int r = 0; r < rise && !blocks.containsKey(top.above()); r++) {
				top = top.above();
				put(top, pillar(Direction.Axis.Y));
			}
			rod(top.above());
			if (mayHangGlass && random.nextFloat() < 0.5F && !blocks.containsKey(end.below())) {
				put(end.below(), glass);
				return true;
			}
			return false;
		}
		return false;
	}

	// --- offerings ---------------------------------------------------------------------------------------

	/** Offerings beyond the structure's reach: in a ring, a cluster to one side, or a line leading away. */
	private void buildOfferings(Offerings palette, int reachOut) {
		int count = 3 + random.nextInt(5);
		int layout = random.nextInt(3);
		double angle = random.nextDouble() * Math.PI * 2.0;
		double spread = layout == 1 ? Math.PI * 0.7 : Math.PI * 2.0;
		int tntAt = random.nextFloat() < (palette == Offerings.SANDY || palette == Offerings.BADLANDS ? 0.34F : EndermanTuning.CAIRN_TNT_CHANCE)
				? random.nextInt(count) : -1;
		List<BlockPos> used = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double distance;
			if (layout == 2) {
				distance = reachOut + 1.6 + i * 1.6; // a line leading away from the cairn
			} else {
				angle += spread / count * (0.7 + random.nextDouble() * 0.6);
				distance = reachOut + 1.6 + random.nextDouble() * 1.4;
			}
			int x = (int) Math.round(Math.cos(angle) * distance);
			int z = (int) Math.round(Math.sin(angle) * distance);
			// Cactus needs free sides; keep offerings apart.
			if (used.stream().anyMatch(u -> Math.abs(u.getX() - x) <= 1 && Math.abs(u.getZ() - z) <= 1)) {
				continue;
			}
			int ground = groundAt.applyAsInt(x, z);
			if (Math.abs(ground + 1) > 2) {
				continue;
			}
			BlockPos base = new BlockPos(x, ground + 1, z);
			if (blocks.containsKey(base) || blocks.containsKey(base.above()) || blocks.containsKey(base.above(2))) {
				continue;
			}
			used.add(base);
			if (i == tntAt) {
				put(base, Blocks.TNT.defaultBlockState());
				continue;
			}
			offering(palette, base, Direction.fromYRot(Math.toDegrees(angle) - 90.0));
		}
		if (used.isEmpty()) {
			// Every cairn has an offering: one just beyond its reach on each side in turn until one fits.
			for (Direction side : HORIZONTAL) {
				BlockPos base = BlockPos.ZERO.relative(side, reachOut + 2);
				int ground = groundAt.applyAsInt(base.getX(), base.getZ());
				base = base.atY(ground + 1);
				if (!blocks.containsKey(base) && !blocks.containsKey(base.above()) && !blocks.containsKey(base.above(2))) {
					offering(palette, base, side);
					return;
				}
			}
		}
	}

	/** The trade pad: a purpur plinth skirted by four end stone brick slabs, the item lying on top. */
	private void buildTradePad(int reachOut) {
		RandomSource padRandom = new net.minecraft.world.level.levelgen.XoroshiroRandomSource(seed ^ PAD_SALT);
		double start = padRandom.nextDouble() * Math.PI * 2.0;
		int firstRing = padRandom.nextInt(3);
		for (int r = 0; r < 3; r++) {
			int ring = 3 + (firstRing + r) % 3;
			for (int i = 0; i < 16; i++) {
				double a = start + Math.PI * 2.0 * i / 16.0;
				int x = (int) Math.round(Math.cos(a) * (reachOut + ring));
				int z = (int) Math.round(Math.sin(a) * (reachOut + ring));
				int ground = groundAt.applyAsInt(x, z);
				if (Math.abs(ground + 1) > 1 || !padFits(x, ground, z)) {
					continue;
				}
				put(new BlockPos(x, ground + 1, z), Blocks.PURPUR_BLOCK.defaultBlockState());
				for (Direction side : HORIZONTAL) {
					put(new BlockPos(x + side.getStepX(), ground + 1, z + side.getStepZ()), Blocks.END_STONE_BRICK_SLAB.defaultBlockState());
				}
				tradeSpot = new BlockPos(x, ground + 2, z);
				return;
			}
		}
	}

	private boolean padFits(int x, int ground, int z) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) + Math.abs(dz) > 2) {
					continue;
				}
				if (Math.abs(dx) + Math.abs(dz) <= 1 && groundAt.applyAsInt(x + dx, z + dz) != ground) {
					return false;
				}
				for (int dy = -1; dy <= 3; dy++) {
					if (blocks.containsKey(new BlockPos(x + dx, ground + dy, z + dz))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void offering(Offerings palette, BlockPos base, Direction outward) {
		float roll = random.nextFloat();
		switch (palette) {
			case SANDY -> {
				if (roll < 0.55F) {
					cactus(base, Blocks.SAND.defaultBlockState());
				} else {
					put(base, roll < 0.8F ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
				}
			}
			case BADLANDS -> {
				if (roll < 0.5F) {
					cactus(base, Blocks.RED_SAND.defaultBlockState());
				} else {
					put(base, Blocks.RED_SAND.defaultBlockState());
				}
			}
			case SWAMP -> {
				if (roll < 0.35F) {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.BLUE_ORCHID.defaultBlockState());
				} else if (roll < 0.6F) {
					flower(base, Blocks.PODZOL.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState());
				} else {
					put(base, roll < 0.8F ? Blocks.CLAY.defaultBlockState() : Blocks.MUD.defaultBlockState());
				}
			}
			case JUNGLE -> {
				if (roll < 0.4F) {
					put(base, Blocks.MELON.defaultBlockState());
				} else {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.BLUE_ORCHID.defaultBlockState(), Blocks.POPPY.defaultBlockState(),
							Blocks.DANDELION.defaultBlockState()));
				}
			}
			case SNOWY -> {
				if (roll < 0.4F) {
					put(base, roll < 0.2F ? Blocks.GRAVEL.defaultBlockState() : Blocks.CLAY.defaultBlockState());
				} else if (roll < 0.6F) {
					put(base, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, outward));
				} else {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.WHITE_TULIP.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()));
				}
			}
			case DARK -> {
				if (roll < 0.5F) {
					flower(base, Blocks.PODZOL.defaultBlockState(), pick(Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()));
				} else if (roll < 0.75F) {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.CLOSED_EYEBLOSSOM.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()));
				} else {
					put(base, Blocks.PUMPKIN.defaultBlockState());
				}
			}
			case MUSHROOM -> flower(base, Blocks.MYCELIUM.defaultBlockState(), pick(Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()));
			case FLOWERY -> flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.ALLIUM.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState(),
					Blocks.PINK_TULIP.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState()));
			case TEMPERATE -> {
				if (roll < 0.55F) {
					flower(base, Blocks.MOSS_BLOCK.defaultBlockState(), pick(Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
							Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
							Blocks.RED_TULIP.defaultBlockState()));
				} else if (roll < 0.7F) {
					put(base, Blocks.PUMPKIN.defaultBlockState());
				} else if (roll < 0.82F) {
					put(base, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, outward));
				} else if (roll < 0.92F) {
					flower(base, Blocks.PODZOL.defaultBlockState(), pick(Blocks.RED_MUSHROOM.defaultBlockState(), Blocks.BROWN_MUSHROOM.defaultBlockState()));
				} else {
					put(base, Blocks.CLAY.defaultBlockState());
				}
			}
		}
	}

	private void flower(BlockPos base, BlockState soil, BlockState plant) {
		put(base, soil);
		put(base.above(), plant);
	}

	private void cactus(BlockPos base, BlockState sand) {
		put(base, sand);
		put(base.above(), Blocks.CACTUS.defaultBlockState());
		if (random.nextFloat() < 0.5F) {
			put(base.above(2), Blocks.CACTUS_FLOWER.defaultBlockState());
		}
	}

	/** An upright end-rod flower, unless it would top the stronghold pointer. */
	private void rod(BlockPos pos) {
		if (pos.getY() <= rodCeiling) {
			putIfFree(pos, Blocks.END_ROD.defaultBlockState());
		}
	}

	private BlockState pick(BlockState... states) {
		return states[random.nextInt(states.length)];
	}

	private static BlockState pillar(Direction.Axis axis) {
		return Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private void put(BlockPos pos, BlockState state) {
		blocks.put(pos, state);
	}

	private boolean putIfFree(BlockPos pos, BlockState state) {
		if (blocks.containsKey(pos)) {
			return false;
		}
		blocks.put(pos, state);
		return true;
	}
}

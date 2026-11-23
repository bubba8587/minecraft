// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import io.github.bubba8587.bubbas_reworked_mobs.world.LoadedBlockEntities;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

/** An Ancient Fabricator's whole build from a seed and mob, in local coordinates (specs/mob-factory.md). */
public final class FactoryDesign {
	// Bounds of the local grid.
	private static final int MIN_X = -34;
	private static final int MAX_X = 34;
	private static final int MIN_Y = -13;
	private static final int MAX_Y = 34;
	private static final int MIN_Z = -34;
	private static final int MAX_Z = 52;
	private static final int SX = MAX_X - MIN_X + 1;
	private static final int SY = MAX_Y - MIN_Y + 1;
	private static final int SZ = MAX_Z - MIN_Z + 1;

	/** Cavern radius before the wobble. */
	static final int RADIUS = 21;
	/** Platform: x in [-PX, PX], z in [PZ_BACK, PZ_FRONT], top block at y -1. */
	static final int PX = 9;
	static final int PZ_BACK = -10;
	static final int PZ_FRONT = 8;
	/** The face wall's plane (its pixels face +z). */
	static final int WALL_Z = -11;
	/** The paired beacons' column. */
	static final int BEAM_X = -6;
	static final int BEAM_Z = -1;
	/** The drop point: the teleporter pad's middle. The long spikes and the chains' ends hang over it. */
	static final int DROP_Z = 3;

	/** The mobs a factory can make, each with a face on the wall. */
	public enum Mob {
		ZOMBIE("zombie"), SKELETON("skeleton"), CREEPER("creeper"), SPIDER("spider"), ENDERMAN("enderman");

		private final String id;

		Mob(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		public static Mob byId(String id) {
			for (Mob mob : values()) {
				if (mob.id.equals(id)) {
					return mob;
				}
			}
			throw new IllegalArgumentException("no factory mob " + id);
		}
	}

	private final long seed;
	private final Mob mob;
	private final RandomSource random;
	private final double[] phase = new double[16];
	/** 1 where the cavern (or tunnel) is open air. */
	private final byte[] open = new byte[SX * SY * SZ];
	/** Local block states in placement order (solid work first). */
	private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
	/** Things that hang on or stand on others (vines, veins, candles, lanterns, sensors): placed last. */
	private final Map<BlockPos, BlockState> attached = new LinkedHashMap<>();
	private BlockPos spawner;
	private BlockPos floorBeacon;
	private BlockPos ceilingBeacon;
	private final BlockPos warden = new BlockPos(15, 6, 0);
	private BlockPos jukebox;
	/** Where vanilla's ancient-city sculk patches are grown once the build stands (air above a surface). */
	private final List<BlockPos> sculkSeeds = new ArrayList<>();
	private int dripperBottom;
	private final int[] ceilCache = new int[SX * SZ];
	/** Local position of the ancient city's centre this Fabricator stands beside, or null (a hand-placed one). */
	private final @Nullable BlockPos city;
	/** Office chests (loot tables are set when they're placed). */
	private final List<BlockPos> chests = new ArrayList<>();
	/** The lamp ring round the pad, in the order they light. */
	private final List<BlockPos> timerLamps = new ArrayList<>();
	private int minX = Integer.MAX_VALUE;
	private int minY = Integer.MAX_VALUE;
	private int minZ = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int maxY = Integer.MIN_VALUE;
	private int maxZ = Integer.MIN_VALUE;

	private FactoryDesign(long seed, Mob mob, @Nullable BlockPos city) {
		this.seed = seed;
		this.mob = mob;
		this.city = city;
		this.random = RandomSource.create(seed);
		for (int i = 0; i < this.phase.length; i++) {
			this.phase[i] = this.random.nextDouble() * Math.PI * 2.0;
		}
	}

	/**
	 * The design for this seed and mob, its entrance tunnel running to an ancient city whose centre is at {@code city}
	 * (local coordinates; null for the plain winding tunnel). Deterministic.
	 */
	public static FactoryDesign design(long seed, Mob mob, @Nullable BlockPos city) {
		FactoryDesign d = new FactoryDesign(seed, mob, city);
		d.carve();
		d.shell();
		d.growth();
		d.tendrils();
		d.floorDetails();
		d.platform();
		d.faceWall();
		d.tower();
		d.lowerCity();
		d.rejectPit();
		d.beams();
		d.intake();
		d.sculkSeeds();
		d.cityTunnel();
		d.bounds();
		return d;
	}

	private void bounds() {
		for (Map<BlockPos, BlockState> pass : List.of(this.blocks, this.attached)) {
			for (BlockPos p : pass.keySet()) {
				this.minX = Math.min(this.minX, p.getX());
				this.minY = Math.min(this.minY, p.getY());
				this.minZ = Math.min(this.minZ, p.getZ());
				this.maxX = Math.max(this.maxX, p.getX());
				this.maxY = Math.max(this.maxY, p.getY());
				this.maxZ = Math.max(this.maxZ, p.getZ());
			}
		}
	}

	/** The world box the build covers. */
	public net.minecraft.world.level.levelgen.structure.BoundingBox box(BlockPos origin, Rotation rotation) {
		return net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(
				toWorld(origin, new BlockPos(this.minX, this.minY, this.minZ), rotation), toWorld(origin, new BlockPos(this.maxX, this.maxY, this.maxZ), rotation));
	}

	/** Local: the middle of the teleporter pad, first air above it. */
	public BlockPos pad() {
		return new BlockPos(0, 0, DROP_Z);
	}

	/** Local: the pad's lodestone, the teleporter's redstone control (powered: stopped). */
	public BlockPos control() {
		return new BlockPos(0, -1, DROP_Z);
	}

	/** Local: where a dropped mob appears, just under the dripper's crying obsidian tip. */
	public BlockPos dropPoint() {
		return new BlockPos(0, this.dripperBottom - 1, DROP_Z);
	}

	/** Local: the dripper's tip. */
	public BlockPos dripperTip() {
		return new BlockPos(0, this.dripperBottom, DROP_Z);
	}

	public List<BlockPos> timerLamps() {
		return this.timerLamps;
	}

	/** Local position of the buried spawner. */
	public BlockPos spawner() {
		return this.spawner;
	}

	public BlockPos floorBeacon() {
		return this.floorBeacon;
	}

	public BlockPos ceilingBeacon() {
		return this.ceilingBeacon;
	}

	/** Local position of the office jukebox. */
	public BlockPos jukebox() {
		return this.jukebox;
	}

	/** Where the Warden stands (feet), local; it looks toward -x. */
	public BlockPos wardenSpot() {
		return this.warden;
	}

	public int size() {
		return this.blocks.size() + this.attached.size();
	}

	// ---- Shape ----

	private double wobble(double angle) {
		return 2.2 * Math.sin(3 * angle + this.phase[0]) + 1.4 * Math.sin(5 * angle + this.phase[1]) + 0.8 * Math.sin(9 * angle + this.phase[2]);
	}

	/** The cavern's edge radius in this direction. */
	private double edge(int x, int z) {
		return RADIUS + wobble(Math.atan2(z, x));
	}

	/** First solid block above the cavern at this column (the ceiling), or MIN when outside it. */
	int ceiling(int x, int z) {
		double r = Math.hypot(x, z);
		double e = edge(x, z);
		if (r >= e) {
			return Integer.MIN_VALUE;
		}
		double t = r / e;
		return (int) Math.round(13.0 + 9.0 * (1.0 - t * t) + 1.5 * Math.sin(x * 0.31 + this.phase[5]) * Math.sin(z * 0.27 + this.phase[6]));
	}

	private int floor(int x, int z) {
		double r = Math.hypot(x, z);
		double e = edge(x, z);
		double rise = Math.max(0.0, r - (e - 6.0));
		return (int) Math.round(-8.0 + rise * 1.1 + 1.2 * Math.sin(x * 0.45 + this.phase[3]) * Math.cos(z * 0.38 + this.phase[4]));
	}

	private void carve() {
		for (int x = MIN_X; x <= MAX_X; x++) {
			for (int z = MIN_Z; z <= MAX_Z; z++) {
				int c = ceiling(x, z);
				this.ceilCache[(x - MIN_X) * SZ + (z - MIN_Z)] = c;
				if (c == Integer.MIN_VALUE) {
					continue;
				}
				for (int y = floor(x, z) + 1; y < c; y++) {
					setOpen(x, y, z, true);
				}
			}
		}
		// The entrance tunnel, winding out of the front and climbing (a Fabricator beside a city gets its own,
		// cityTunnel).
		for (int z = 12; z <= MAX_Z - 4 && this.city == null; z++) {
			double cx = 3.0 * Math.sin(z * 0.13 + this.phase[7]);
			double cy = z < 18 ? 1.0 : 1.0 + (z - 18) * 0.35;
			for (int x = (int) cx - 4; x <= (int) cx + 4; x++) {
				for (int y = (int) cy - 4; y <= (int) cy + 4; y++) {
					double dx = (x - cx) / 2.6;
					double dy = (y - cy) / 2.2;
					if (dx * dx + dy * dy <= 1.0) {
						setOpen(x, y, z, true);
					}
				}
			}
		}
	}

	private void shell() {
		for (int x = MIN_X + 2; x <= MAX_X - 2; x++) {
			for (int y = MIN_Y + 2; y <= MAX_Y - 2; y++) {
				for (int z = MIN_Z + 2; z <= MAX_Z - 2; z++) {
					if (isOpen(x, y, z)) {
						set(x, y, z, Blocks.AIR.defaultBlockState());
						continue;
					}
					int near = nearOpen(x, y, z);
					if (near == 0) {
						continue;
					}
					if (near == 1) {
						set(x, y, z, surface(x, y, z));
					} else {
						set(x, y, z, rock(x, y, z));
					}
				}
			}
		}
		// Sculk veins creeping over walls and ceiling, hanging lichen-like on the rock.
		for (int x = MIN_X + 2; x <= MAX_X - 2; x++) {
			for (int y = MIN_Y + 2; y <= MAX_Y - 2; y++) {
				for (int z = MIN_Z + 2; z <= MAX_Z - 2; z++) {
					if (!isOpen(x, y, z) || hash(x, y, z, 11) > 0.16) {
						continue;
					}
					BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();
					boolean any = false;
					for (Direction dir : Direction.values()) {
						if (dir == Direction.DOWN) {
							continue;
						}
						int nx = x + dir.getStepX();
						int ny = y + dir.getStepY();
						int nz = z + dir.getStepZ();
						if (!isOpen(nx, ny, nz) && inGrid(nx, ny, nz)) {
							vein = vein.setValue(MultifaceBlock.getFaceProperty(dir), true);
							any = true;
						}
					}
					if (any) {
						attach(x, y, z, vein);
					}
				}
			}
		}
	}

	/** How close the nearest open cell is: 1 (touching), 2 (two out) or 0 (farther). */
	private int nearOpen(int x, int y, int z) {
		for (Direction dir : Direction.values()) {
			if (isOpen(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ())) {
				return 1;
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (isOpen(x + dx, y + dy, z + dz)) {
						return 2;
					}
				}
			}
		}
		return 0;
	}

	/** A rock face touching the cavern: sculk spreads over the floor and much of the walls. */
	private BlockState surface(int x, int y, int z) {
		boolean floorFace = isOpen(x, y + 1, z);
		double n = noise(x * 0.18, y * 0.18, z * 0.18, 0);
		if (floorFace && n > 0.35 || !floorFace && !isOpen(x, y - 1, z) && n > 0.62) {
			return Blocks.SCULK.defaultBlockState();
		}
		return rock(x, y, z);
	}

	private BlockState rock(int x, int y, int z) {
		double h = hash(x, y, z, 3);
		if (h < 0.12) {
			return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		}
		if (h < 0.18) {
			return Blocks.TUFF.defaultBlockState();
		}
		return Blocks.DEEPSLATE.defaultBlockState();
	}

	// ---- The ceiling growth ----

	private boolean inGrowth(int x, int z) {
		return Math.hypot(x, z) < 15.5 + wobble(Math.atan2(z, x)) * 0.5;
	}

	/**
	 * The ceiling mass: warped wart with teal stripped
	 * hyphae veins through it, dark basalt seams, shroomlight accents, the odd bone knot.
	 */
	private BlockState massBlock(int x, int y, int z) {
		double h = hash(x, y, z, 5);
		double n = noise(x * 0.22, y * 0.3, z * 0.22, 1);
		if (h < 0.003) {
			return Blocks.SHROOMLIGHT.defaultBlockState();
		}
		if (crimsonStreak(x, y, z)) {
			return crimson(x, y, z);
		}
		if (n < -0.55) {
			return h < 0.12 ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.BASALT.defaultBlockState();
		}
		if (n < -0.35) {
			return Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState();
		}
		return Blocks.WARPED_WART_BLOCK.defaultBlockState();
	}

	private void growth() {
		// The mass: the ceiling and 4-6 blocks into it, with a gently lumpy underside.
		for (int x = -18; x <= 18; x++) {
			for (int z = -18; z <= 18; z++) {
				if (!inGrowth(x, z)) {
					continue;
				}
				int c = ceil(x, z);
				if (c == Integer.MIN_VALUE) {
					continue;
				}
				int thick = 4 + (int) Math.round(1.5 * noise(x * 0.2, 0, z * 0.2, 2));
				for (int y = c; y <= c + thick; y++) {
					set(x, y, z, massBlock(x, y, z));
				}
				int lump = (int) Math.round(1.2 * noise(x * 0.35, 3.0, z * 0.35, 3) - 0.5);
				for (int d = 1; d <= lump; d++) {
					if (!nearBeam(x, z, 2.0)) {
						solid(x, c - d, z, massBlock(x, c - d, z));
					}
				}
			}
		}
		centralDripper();
		// Small drippers: short warped teats, 2-5 long, spread over the ceiling,
		// none within 6 of the central one.
		List<int[]> drippers = new ArrayList<>();
		int tries = 0;
		while (drippers.size() < 34 && tries++ < 2000) {
			double a = this.random.nextDouble() * Math.PI * 2.0;
			double r = Math.sqrt(this.random.nextDouble()) * 15.0;
			int x = (int) Math.round(Math.cos(a) * r);
			int z = (int) Math.round(Math.sin(a) * r);
			if (nearBeam(x, z, 2.6) || !inGrowth(x, z) || x >= 12 || Math.hypot(x, z - DROP_Z) < 5.5) {
				continue;
			}
			boolean crowded = false;
			for (int[] s : drippers) {
				if (Math.hypot(s[0] - x, s[1] - z) < 2.8) {
					crowded = true;
					break;
				}
			}
			if (crowded) {
				continue;
			}
			int length = 2 + this.random.nextInt(z < 0 || x > 4 && Math.abs(z) < 5 ? 2 : 4);
			double base = 0.6 + this.random.nextDouble() * 0.5;
			drippers.add(new int[] { x, z });
			smallDripper(x, z, length, base);
		}
		// The inert spawner, buried in the mass right above the central dripper's root, wrapped in hyphae.
		int sx = 0;
		int sz = DROP_Z;
		int sy = ceil(sx, sz) + 2;
		for (Direction dir : Direction.values()) {
			set(sx + dir.getStepX(), sy + dir.getStepY(), sz + dir.getStepZ(), Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState());
		}
		set(sx, sy, sz, Blocks.SPAWNER.defaultBlockState());
		this.spawner = new BlockPos(sx, sy, sz);
	}

	/**
	 * The dripper (owner, 2026-11-16: the roof is the Fabricator, a living warped growth that makes the mobs; the
	 * ancient teleporter below harvests its drip). One dominant stalk hangs precisely over the teleporter pad: a
	 * collar of warped wart where it leaves the mass, then a stalk of stripped warped hyphae (the teal flesh inside a
	 * warped stem) tapering down, shroomlight glands set in it, ending in a single crying obsidian tip 8 above the
	 * deck. Crying obsidian drips purple tears from its underside on its own, so the drip is real and lands on the
	 * pad. Strands of wart hang round the tip like a membrane.
	 */
	private void centralDripper() {
		int c = ceil(0, DROP_Z);
		int tip = 8;
		int length = c - 1 - tip; // stalk blocks above the tip
		for (int d = 0; d < length; d++) {
			double t = (double) d / Math.max(1, length - 1);
			double radius = 3.4 * Math.pow(1.0 - t, 1.4) + 0.35;
			int y = c - 1 - d;
			int reach = (int) Math.ceil(radius);
			for (int x = -reach; x <= reach; x++) {
				for (int z = DROP_Z - reach; z <= DROP_Z + reach; z++) {
					double dist = Math.hypot(x, z - DROP_Z);
					if (dist > radius) {
						continue;
					}
					boolean outer = dist > radius - 1.0;
					BlockState s;
					if (t < 0.3 && outer) {
						s = Blocks.WARPED_WART_BLOCK.defaultBlockState();
					} else if (!outer && dist < 0.5 && Math.floorMod(d, 4) == 2 && t > 0.2) {
						s = Blocks.SHROOMLIGHT.defaultBlockState(); // a gland in the core
					} else if (radius < 1.6 ? dist < 0.5 && Math.floorMod(d, 2) == 1 : outer && t >= 0.3 && Math.floorMod(d, 4) == 1 && hash(x, y, z, 34) < 0.4) {
						s = Blocks.SHROOMLIGHT.defaultBlockState(); // glands: rings on the skin of the thick part, every other block down the thin spine; the dripper glows
					} else if (outer && crimsonStreak(x, y, z)) {
						s = crimson(x, y, z);
					} else {
						s = Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState();
					}
					solid(x, y, z, s);
				}
			}
		}
		solid(0, tip, DROP_Z, Blocks.CRYING_OBSIDIAN.defaultBlockState());
		// The membrane: short wart strands hanging round the lower stalk.
		int[][] around = { { 1, 1 }, { -1, -1 }, { 1, -1 }, { -1, 1 }, { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 } };
		for (int i = 0; i < around.length; i++) {
			int x = around[i][0];
			int z = DROP_Z + around[i][1];
			int top = tip + 3 + (i % 3);
			int len = 1 + (int) (hash(x, top, z, 35) * 2.99);
			for (int k = 0; k < len; k++) {
				if (isOpen(x, top - k, z)) {
					solid(x, top - k, z, Blocks.WARPED_WART_BLOCK.defaultBlockState());
				}
			}
		}
		this.dripperBottom = tip;
	}

	/** A small dripper: the same anatomy far smaller: a wart collar, a short teal stalk, now and then a crying obsidian tip. */
	private void smallDripper(int x0, int z0, int length, double base) {
		int top = ceil(x0, z0);
		int y = top - 1;
		for (int d = 0; d < length; d++, y--) {
			double t = (double) d / length;
			double radius = base * (1.0 - t) + 0.3;
			int reach = (int) Math.ceil(radius);
			boolean any = false;
			for (int x = x0 - reach; x <= x0 + reach; x++) {
				for (int z = z0 - reach; z <= z0 + reach; z++) {
					double dist = Math.hypot(x - x0, z - z0);
					if (dist <= radius && !nearBeam(x, z, 1.5)) {
						solid(x, y, z, d == 0 && dist > 0.5 ? Blocks.WARPED_WART_BLOCK.defaultBlockState() : Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState());
						any = true;
					}
				}
			}
			if (!any) {
				solid(x0, y, z0, Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState());
			}
		}
		if (!nearBeam(x0, z0, 1.5) && isOpen(x0, y, z0)) {
			solid(x0, y, z0, hash(x0, y, z0, 36) < 0.2 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.WARPED_WART_BLOCK.defaultBlockState());
		}
	}

	private static boolean nearBeam(int x, int z, double r) {
		return Math.hypot(x - BEAM_X, z - BEAM_Z) < r;
	}

	// ---- The chains: alien chains climbing the walls and curling in to the dripper ----

	/**
	 * Owner (2026-11-16): the warped growth forms alien "chains" that curve up around the cavern's walls, then curl in
	 * and bend toward the central dripper. Each is a thin strand (about one block thick) of warped wart along a cubic
	 * curve, threaded with knots of stripped warped hyphae every ~3 blocks so it reads as links: from low on the wall,
	 * up it, then sagging free under the roof, twisting as it goes (so they spiral in), to the dripper's stalk. Every
	 * few knots is crimson. None start on the face wall, the tower or the tunnel mouth.
	 */
	private void tendrils() {
		int count = 11 + this.random.nextInt(3);
		double start = this.random.nextDouble() * Math.PI * 2.0;
		for (int i = 0; i < count; i++) {
			double a = start + i * Math.PI * 2.0 / count + (this.random.nextDouble() - 0.5) * 0.3;
			double ca = Math.cos(a);
			double sa = Math.sin(a);
			// Skip the face wall (behind, -z), the tower (+x) and the tunnel mouth (+z, middle).
			if (sa < -0.55 || ca > 0.8 || sa > 0.93) {
				continue;
			}
			double e = RADIUS + wobble(a);
			double turn = (this.random.nextBoolean() ? 1 : -1) * (0.6 + this.random.nextDouble() * 0.5);
			double endY = 10.0 + this.random.nextDouble() * 6.0;
			double[][] ctrl = {
					{ e - 1.5, -5.0 + this.random.nextDouble() * 2.0, 0.0 },
					{ e - 2.0, 14.0, turn * 0.2 },
					{ 9.0, 18.0, turn * 0.65 },
					{ 2.6, endY, turn },
			};
			int step = 0;
			int knots = 0;
			for (double t = 0.0; t <= 1.0; t += 0.008, step++) {
				double u = 1.0 - t;
				double[] b = { u * u * u, 3 * u * u * t, 3 * u * t * t, t * t * t };
				double r = 0;
				double y = 0;
				double tw = 0;
				for (int k = 0; k < 4; k++) {
					r += b[k] * ctrl[k][0];
					y += b[k] * ctrl[k][1];
					tw += b[k] * ctrl[k][2];
				}
				double ang = a + tw;
				double cx = Math.cos(ang) * r;
				double cz = Math.sin(ang) * r + t * t * DROP_Z;
				// Hang free under the roof: at least 1.5 below the ceiling, sagging between wall and dripper.
				int ceilHere = ceil((int) Math.round(cx), (int) Math.round(cz));
				if (ceilHere != Integer.MIN_VALUE) {
					y = Math.min(y, ceilHere - 1.5 - 2.5 * Math.sin(Math.PI * Math.max(0.0, (t - 0.35) / 0.65)));
				}
				boolean knot = step % 7 == 0 && t > 0.03;
				if (knot) {
					knots++;
				}
				double radius = knot ? 1.2 : 0.62;
				int reach = (int) Math.ceil(radius);
				for (int x = (int) Math.floor(cx) - reach; x <= (int) Math.ceil(cx) + reach; x++) {
					for (int yy = (int) Math.floor(y) - reach; yy <= (int) Math.ceil(y) + reach; yy++) {
						for (int z = (int) Math.floor(cz) - reach; z <= (int) Math.ceil(cz) + reach; z++) {
							double d = Math.sqrt((x - cx) * (x - cx) + (yy - y) * (yy - y) + (z - cz) * (z - cz));
							if (d > radius || nearBeam(x, z, 1.6) || inTower(x, z) || Math.hypot(x, z - DROP_Z) < 1.9 && yy < ceil(0, DROP_Z) - 4
									|| yy <= 1 && Math.abs(x) <= PX + 1 && z >= PZ_BACK && z <= PZ_FRONT) {
								continue;
							}
							BlockState s;
							if (knot) {
								s = knots % 4 == 0 ? crimson(x, yy, z) : Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState();
							} else if (crimsonStreak(x, yy, z)) {
								s = crimson(x, yy, z);
							} else {
								s = Blocks.WARPED_WART_BLOCK.defaultBlockState();
							}
							solid(x, yy, z, s);
							this.chainBlocks.add(new BlockPos(x, yy, z));
						}
					}
				}
			}
		}
	}

	/**
	 * The owner's crimson streaks (2026-11-16): thin wandering sheets through the warped growth (where two seeded
	 * waves cross zero together), about one block in twenty, so crimson shows as veins and streaks, never patches.
	 */
	private boolean crimsonStreak(int x, int y, int z) {
		// Two wavy sheets; where both are near zero they cross in a line, so crimson runs as streaks.
		double v = Math.sin(x * 0.21 + y * 0.55 + 0.6 * Math.sin(z * 0.19 + this.phase[10]) + this.phase[8]);
		double w = Math.sin(z * 0.23 - y * 0.43 + 0.6 * Math.sin(x * 0.17 + this.phase[11]) + this.phase[9]);
		return Math.abs(v) < 0.3 && Math.abs(w) < 0.45;
	}

	/** A crimson streak block: nether wart, stripped crimson hyphae (the red flesh), crimson hyphae. */
	private BlockState crimson(int x, int y, int z) {
		double h = hash(x, y, z, 41);
		return h < 0.5 ? Blocks.NETHER_WART_BLOCK.defaultBlockState()
				: h < 0.85 ? Blocks.STRIPPED_CRIMSON_HYPHAE.defaultBlockState() : Blocks.CRIMSON_HYPHAE.defaultBlockState();
	}

	private static boolean inTower(int x, int z) {
		return x >= 9 && Math.abs(z) <= 6;
	}

	// ---- The cavern floor ----

	private void floorDetails() {
		for (int x = -24; x <= 24; x++) {
			for (int z = -24; z <= 24; z++) {
				int c = ceil(x, z);
				if (c == Integer.MIN_VALUE) {
					continue;
				}
				int f = floor(x, z);
				boolean underPlatform = Math.abs(x) <= PX + 4 && z >= PZ_BACK - 2 && z <= PZ_FRONT + 2;
				double h = hash(x, f, z, 7);
				if (!isOpen(x, f + 1, z) || isOpen(x, f, z)) {
					continue;
				}
				if (h < 0.022) {
					attach(x, f + 1, z, Blocks.SCULK_SENSOR.defaultBlockState());
				} else if (h < 0.026) {
					set(x, f, z, Blocks.SCULK_CATALYST.defaultBlockState());
				} else if (underPlatform && h < 0.045) {
					// Soul fire smouldering in the pit under the platform.
					set(x, f, z, Blocks.SOUL_SOIL.defaultBlockState());
					attach(x, f + 1, z, Blocks.SOUL_FIRE.defaultBlockState());
				}
			}
		}
		// A few low basalt-and-bone stalagmites around the pit.
		for (int i = 0; i < 14; i++) {
			double a = this.random.nextDouble() * Math.PI * 2.0;
			double r = 12.0 + this.random.nextDouble() * 6.0;
			int x = (int) Math.round(Math.cos(a) * r);
			int z = (int) Math.round(Math.sin(a) * r);
			if (x >= 12 && Math.abs(z) <= 6 || z > 8 && Math.abs(x) < 6) {
				continue;
			}
			int f = floor(x, z);
			int h = 2 + this.random.nextInt(4);
			for (int d = 1; d <= h; d++) {
				double t = (double) d / h;
				solid(x, f + d, z, t > 0.7 ? Blocks.WARPED_WART_BLOCK.defaultBlockState() : Blocks.BASALT.defaultBlockState());
				if (d <= h / 2) {
					for (Direction dir : Direction.Plane.HORIZONTAL) {
						if (hash(x, f + d, z, dir.ordinal()) < 0.55) {
							solid(x + dir.getStepX(), f + d, z + dir.getStepZ(), Blocks.BASALT.defaultBlockState());
						}
					}
				}
			}
		}
	}

	// ---- The platform ----

	private void platform() {
		// Supports first: brick pillars from the cavern floor up to the deck.
		int[][] pillars = { { -8, -9 }, { 8, -9 }, { -8, -1 }, { 8, -1 }, { -8, 7 }, { 8, 7 }, { 0, 7 }, { 0, -9 } };
		for (int[] p : pillars) {
			for (int dx = 0; dx <= 1; dx++) {
				for (int dz = 0; dz <= 1; dz++) {
					int x = p[0] + dx - (p[0] > 0 ? 1 : 0);
					int z = p[1] + dz;
					for (int y = floor(x, z); y <= -3; y++) {
						BlockState s = y == -3 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
								: hash(x, y, z, 9) < 0.3 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
						solid(x, y, z, s);
					}
				}
			}
			// An observer staring out of each outer pillar, ancient-city machinery.
			int ox = p[0] + (p[0] > 0 ? 0 : 0);
			if (p[0] != 0) {
				Direction out = p[0] > 0 ? Direction.EAST : Direction.WEST;
				int x = p[0] > 0 ? p[0] : p[0];
				solid(x, -6, p[1], Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, out));
			}
		}
		// Upside-down stair corbels under the deck edge.
		for (int x = -PX; x <= PX; x++) {
			for (int z = PZ_BACK; z <= PZ_FRONT; z++) {
				boolean edgeX = Math.abs(x) == PX;
				boolean edgeZ = z == PZ_FRONT;
				// Deck, two layers.
				solid(x, -2, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				solid(x, -1, z, deckTop(x, z));
				solid(x, 0, z, Blocks.AIR.defaultBlockState());
				if (edgeX && hash(x, -3, z, 2) < 0.7) {
					solid(x, -3, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
							.setValue(StairBlock.FACING, x > 0 ? Direction.WEST : Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
				} else if (edgeZ && hash(x, -3, z, 2) < 0.7) {
					solid(x, -3, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
							.setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP));
				}
			}
		}
		// Redstone lamps set into the deck in two lines down the sides, unlit: the pad's timer ring is the deck's light.
		for (int z : new int[] { -8, -4, 4 }) {
			for (int x : new int[] { -7, 7 }) {
				solid(x, -1, z, Blocks.REDSTONE_LAMP.defaultBlockState());
			}
		}
		// The teleporter pad: a lodestone ringed by crying obsidian, then the timer ring (owner, 2026-11-16: a
		// redstone-visible activation timer: lamps that light up, not a redstone clock). Eight lamps go round clockwise from the back; mechanics will light one
		// per step toward the moment it fires. The look shows it mid-count: the first five lit (on redstone blocks).
		int pz = DROP_Z;
		int[][] timer = { { 0, -2 }, { 2, -2 }, { 2, 0 }, { 2, 2 }, { 0, 2 }, { -2, 2 }, { -2, 0 }, { -2, -2 } };
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				BlockState s = ring == 0 ? Blocks.LODESTONE.defaultBlockState()
						: ring == 1 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
						: Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
				solid(dx, -1, pz + dz, s);
			}
		}
		for (int i = 0; i < timer.length; i++) {
			boolean lit = false;
			this.timerLamps.add(new BlockPos(timer[i][0], -1, pz + timer[i][1]));
			solid(timer[i][0], -1, pz + timer[i][1], Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, lit));
			solid(timer[i][0], -2, pz + timer[i][1], lit ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
		}
		// Floor beacon on its iron inlay, framed by polished blackstone, with candles at the corners.
		this.floorBeacon = new BlockPos(BEAM_X, 0, BEAM_Z);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				boolean inner = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
				solid(BEAM_X + dx, -1, BEAM_Z + dz, inner ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
				// A second pyramid layer, hidden in the deck: 2 layers let a beacon gem through at up to Gem Power III.
				solid(BEAM_X + dx, -2, BEAM_Z + dz, Blocks.IRON_BLOCK.defaultBlockState());
			}
		}
		solid(BEAM_X, 0, BEAM_Z, Blocks.BEACON.defaultBlockState());
		for (int dx : new int[] { -2, 2 }) {
			for (int dz : new int[] { -2 }) {
				attach(BEAM_X + dx, 0, BEAM_Z + dz, candle(1 + (int) (hash(dx, 0, dz, 4) * 1.99)));
			}
		}
		// Rim: low tile walls along the sides and front (a gap to the front and to the Warden's stairs).
		for (int z = PZ_BACK; z <= PZ_FRONT; z++) {
			for (int x : new int[] { -PX, PX }) {
				boolean stairGap = x == PX && z >= 6 || x == -PX && z >= TERRACE_STAIR_Z - 1 && z <= TERRACE_STAIR_Z + 1 || x == PX && (z == -9 || z == -8);
				if (!stairGap && Math.floorMod(z, 6) != 3) {
					attach(x, 0, z, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
				}
			}
		}
		for (int x = -PX; x <= PX; x++) {
			if (Math.abs(x) > 2) {
				attach(x, 0, PZ_FRONT, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
			}
		}
		// Corner posts; soul lanterns on the front two only.
		for (int x : new int[] { -PX, PX }) {
			for (int z : new int[] { PZ_BACK, PZ_FRONT }) {
				solid(x, 0, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				solid(x, 1, z, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
				if (z == PZ_FRONT) {
					attach(x, 2, z, Blocks.SOUL_LANTERN.defaultBlockState());
				}
			}
		}
		// Front posts by the entrance gap.
		for (int x : new int[] { -3, 3 }) {
			solid(x, 0, PZ_FRONT, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
			attach(x, 1, PZ_FRONT, candle(4));
		}
		// Sculk sensors and shriekers on the deck: the machine listens.
		attach(-4, 0, -9, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, false));
		attach(4, 0, -9, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, false));
		attach(-7, 0, 6, Blocks.SCULK_SENSOR.defaultBlockState());
		attach(7, 0, -6, Blocks.SCULK_SENSOR.defaultBlockState());
		attach(4, 0, 6, Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
	}

	private BlockState deckTop(int x, int z) {
		if (Math.abs(x) == PX || z == PZ_BACK || z == PZ_FRONT) {
			return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		}
		if (Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2) {
			return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
		}
		double h = hash(x, -1, z, 8);
		return h < 0.3 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
	}

	private static BlockState candle(int count) {
		return Blocks.DYED_CANDLE.gray().defaultBlockState().setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, count))).setValue(CandleBlock.LIT, true);
	}

	// ---- The face wall ----

	private void faceWall() {
		String[] rows = FactoryFaces.FACES.get(this.mob.id());
		// Backing and frame: a brick slab of wall from the deck up to the ceiling.
		for (int x = -11; x <= 10; x++) {
			for (int y = -2; y <= 20; y++) {
				for (int z = WALL_Z - 2; z <= WALL_Z; z++) {
					boolean frame = z == WALL_Z && (x == -9 || x == 8 || y == 0 || y == 17) && x >= -9 && x <= 8 && y >= 0 && y <= 17;
					BlockState s;
					if (frame) {
						boolean corner = (x == -9 || x == 8) && (y == 0 || y == 17);
						s = corner ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
					} else if (z == WALL_Z && (x == -10 || x == 9 || x == -11 || x == 10)) {
						s = Blocks.DEEPSLATE_TILES.defaultBlockState();
					} else {
						s = hash(x, y, z, 6) < 0.3 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
					}
					solid(x, y, z, s);
				}
			}
		}
		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				BlockState s = parse(FactoryFaces.BLOCKS.get(rows[r].charAt(c)));
				for (int dy = 0; dy <= 1; dy++) {
					for (int dx = 0; dx <= 1; dx++) {
						solid(-8 + 2 * c + dx, 16 - 2 * r - dy, WALL_Z, s);
					}
				}
			}
		}
		// Redstone lamps set into the frame, unlit.
		for (int x : new int[] { -7, -3, 2, 6 }) {
			solid(x, 17, WALL_Z, Blocks.REDSTONE_LAMP.defaultBlockState());
		}
		for (int x : new int[] { -9, 8 }) {
			for (int y : new int[] { 4, 9, 13 }) {
				solid(x, y, WALL_Z, Blocks.REDSTONE_LAMP.defaultBlockState());
			}
		}
		// A shelf of tile slabs under the face, and soul lanterns hanging either side.
		for (int x = -9; x <= 8; x++) {
			solid(x, 0, WALL_Z + 1, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
		}
		for (int x : new int[] { -10, 9 }) {
			solid(x, 14, WALL_Z + 1, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP));
			attach(x, 13, WALL_Z + 1, Blocks.IRON_CHAIN.defaultBlockState());

		}
	}

	// ---- The Warden's tower ----

	/** The office's sunken archive starts at this x (floor one lower), with the loft over it (floor block y). */
	private static final int OFFICE_ARCHIVE_X = 20;
	private static final int OFFICE_LOFT_Y = 9;

	private void tower() {
		int x0 = 14;
		int x1 = 23;
		for (int x = x0; x <= x1; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = floor(Math.min(x, 16), z) - 1; y <= 21; y++) {
					boolean inside = x > x0 && x < x1 && Math.abs(z) < 4 && y >= (x >= OFFICE_ARCHIVE_X ? 5 : 6) && y <= 13
							&& !(x >= OFFICE_ARCHIVE_X && y == OFFICE_LOFT_Y);
					boolean window = x == x0 && Math.abs(z) <= 2 && y >= 6 && y <= 10;
					if (inside || window) {
						solid(x, y, z, Blocks.AIR.defaultBlockState());
						continue;
					}
					boolean corner = (x == x0 || x == x1) && Math.abs(z) == 4;
					BlockState s;
					if (corner) {
						s = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
					} else if (x == x0 && Math.abs(z) <= 3 && (y == 5 || y == 11) || x == x0 && Math.abs(z) == 3 && y >= 5 && y <= 11) {
						s = Blocks.CHISELED_DEEPSLATE.defaultBlockState(); // window frame
					} else if (y == 5 && x > x0 && x < x1) {
						s = hash(x, y, z, 1) < 0.5 ? Blocks.SCULK.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
					} else if (x >= OFFICE_ARCHIVE_X && y == OFFICE_LOFT_Y && x < x1 && Math.abs(z) < 4) {
						s = tile(x, y, z); // the loft's floor
					} else if (y == 11 || y == 2 || y == 14) {
						s = Blocks.DEEPSLATE_TILES.defaultBlockState(); // bands
					} else {
						s = hash(x, y, z, 12) < 0.3 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
					}
					solid(x, y, z, s);
				}
			}
		}
		// Inside: the Warden's office (owner, 2026-11-16; made roomier and split-level, owner: "a more dynamic Office,
		// more stairs"). The main floor by the window is double height; behind it, a step down to a sunken
		// archive with the filing chests set in nooks in the back wall; over the archive a loft (a mezzanine) reached
		// by a stair along the north wall, its tile-wall rail looking over the office and out of the window. Gray wool
		// under carpet (so the jukebox's music doesn't reach the sculk), a desk (lectern, slab desktop, stair chair),
		// the jukebox playing an eerie disc (music disc 13, loaded when the factory is placed), a sculk catalyst grown
		// in at one corner, one soul lantern on a chain, candles.
		for (int x = 15; x <= 19; x++) {
			for (int z = -3; z <= 3; z++) {
				solid(x, 5, z, Blocks.WOOL.gray().defaultBlockState());
				if (x >= 16) {
					attach(x, 6, z, Blocks.CARPET.gray().defaultBlockState());
				}
			}
		}
		solid(19, 5, -3, Blocks.SCULK_CATALYST.defaultBlockState());
		this.attached.remove(new BlockPos(19, 6, -3));
		attach(17, 13, -1, Blocks.IRON_CHAIN.defaultBlockState());
		attach(17, 12, -1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		// A step down (3 wide) into the archive (its floor one lower), beside the jukebox corner.
		for (int z = -1; z <= 1; z++) {
			solid(19, 5, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
			this.attached.remove(new BlockPos(19, 6, z));
		}
		for (int x = OFFICE_ARCHIVE_X; x <= x1 - 1; x++) {
			for (int z = -3; z <= 3; z++) {
				solid(x, 4, z, Blocks.WOOL.gray().defaultBlockState());
				if (hash(x, 5, z, 13) < 0.6) {
					attach(x, 5, z, (x + z) % 3 == 0 ? Blocks.CARPET.lightBlue().defaultBlockState() : Blocks.CARPET.gray().defaultBlockState());
				}
			}
		}
		// Filing nooks: the chests sit in the back wall, each under a slab lintel (so their lids open), chiseled between.
		for (int z : new int[] { -2, 0, 2 }) {
			solid(x1, 5, z, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
			solid(x1, 6, z, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
			this.chests.add(new BlockPos(x1, 5, z));
			this.attached.remove(new BlockPos(x1 - 1, 5, z));
		}
		for (int z : new int[] { -3, -1, 1, 3 }) {
			solid(x1, 5, z, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
		}
		attach(x1 - 1, 5, -3, candle(2));
		// The stair to the loft, along the north wall, on brick supports.
		for (int k = 0; k < 4; k++) {
			int x = 16 + k;
			solid(x, 6 + k, -3, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
			for (int y = 6; y < 6 + k; y++) {
				solid(x, y, -3, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
			}
			this.attached.remove(new BlockPos(x, 6, -3));
		}
		// The loft: a rail along its edge, a reading corner (wool, carpet, a lectern, a skull, unlit candles).
		for (int z = -1; z <= 3; z++) {
			attach(OFFICE_ARCHIVE_X, OFFICE_LOFT_Y + 1, z, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
		}
		for (int x = OFFICE_ARCHIVE_X + 1; x <= x1 - 1; x++) {
			for (int z = 0; z <= 3; z++) {
				solid(x, OFFICE_LOFT_Y, z, Blocks.WOOL.gray().defaultBlockState());
				attach(x, OFFICE_LOFT_Y + 1, z, z == 1 ? Blocks.CARPET.lightBlue().defaultBlockState() : Blocks.CARPET.gray().defaultBlockState());
			}
		}
		solid(x1 - 1, OFFICE_LOFT_Y + 1, -2, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
		attach(x1 - 1, OFFICE_LOFT_Y + 1, 3, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 4));
		attach(x1 - 1, OFFICE_LOFT_Y + 1, -3, unlitCandle(3));
		// The desk, along the south wall: a lectern with a stair "chair" in front of it and a slab desktop beside.
		solid(17, 6, 3, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		solid(16, 6, 3, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
		solid(17, 6, 2, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
		attach(16, 7, 3, candle(1));
		for (BlockPos pos : new BlockPos[] { new BlockPos(17, 6, 3), new BlockPos(16, 6, 3), new BlockPos(17, 6, 2) }) {
			this.attached.remove(pos);
		}
		// The jukebox in the corner, on wool, with wool beside it.
		this.jukebox = new BlockPos(19, 6, 2);
		solid(19, 6, 2, Blocks.JUKEBOX.defaultBlockState());
		solid(19, 6, 3, Blocks.WOOL.gray().defaultBlockState());
		solid(18, 6, 3, Blocks.WOOL.gray().defaultBlockState());
		this.attached.remove(new BlockPos(19, 6, 2));
		this.attached.remove(new BlockPos(19, 6, 3));
		this.attached.remove(new BlockPos(18, 6, 3));
		// Window sill: a ledge out over the platform, carried on upside-down stairs.
		for (int x = 10; x <= 13; x++) {
			for (int z = -1; z <= 1; z++) {
				solid(x, 5, z, x == 13 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
				solid(x, 4, z, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
			}
			attach(x, 6, -1, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
		}
		attach(13, 7, -1, Blocks.SOUL_LANTERN.defaultBlockState());
		// A lamp in the floor under the Warden, so it shows in its window.

		// Stairs down from the ledge to a walkway beside the deck.
		for (int k = 0; k <= 4; k++) {
			int z = 2 + k;
			int y = 4 - k;
			for (int x = 10; x <= 12; x++) {
				solid(x, y, z, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
				for (int yy = -3; yy < y; yy++) {
					solid(x, yy, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
				}
			}
		}
		for (int x = 10; x <= 12; x++) {
			for (int z = 7; z <= PZ_FRONT; z++) {
				solid(x, -1, z, Blocks.DEEPSLATE_TILES.defaultBlockState());
				solid(x, -2, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			}
		}
	}

	// ---- The paired beacon column ----

	private void beams() {
		int c = ceil(BEAM_X, BEAM_Z);
		this.ceilingBeacon = new BlockPos(BEAM_X, c, BEAM_Z);
		for (int y = 1; y < c; y++) {
			set(BEAM_X, y, BEAM_Z, Blocks.AIR.defaultBlockState());
			this.attached.remove(new BlockPos(BEAM_X, y, BEAM_Z));
		}
		// The upside-down beacon set into the growth, its iron pyramid above it, crying obsidian around it.
		set(BEAM_X, c, BEAM_Z, Blocks.BEACON.defaultBlockState());
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				set(BEAM_X + dx, c + 1, BEAM_Z + dz, Blocks.IRON_BLOCK.defaultBlockState());
				if (dx != 0 || dz != 0) {
					set(BEAM_X + dx, c, BEAM_Z + dz, Math.abs(dx) + Math.abs(dz) == 1 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
							: Blocks.POLISHED_BASALT.defaultBlockState());
				}
			}
		}
		// Its second pyramid layer, 5 x 5 above that (2 layers: a beacon gem works at up to Gem Power III).
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				set(BEAM_X + dx, c + 2, BEAM_Z + dz, Blocks.IRON_BLOCK.defaultBlockState());
			}
		}
	}

	// ---- Sculk, grown the way ancient cities get theirs ----

	/**
	 * Owner (2026-11-16): sculk should look like it grew there, as in ancient cities. Vanilla's city pieces hold no
	 * sculk at all: it comes from the {@code sculk_patch_ancient_city} feature run over the city afterwards (the real
	 * sculk spreader: 10 charges of 32 creeping over surfaces and leaving veins at the edges, a catalyst at half of
	 * them, 1-3 shriekers), on pieces already degraded (30 % cracked bricks and tiles). So the build is placed clean
	 * and then that same feature is grown from these spots: on the deck, by the face wall, at the tower foot and
	 * stairs, on the window ledge, in the tunnel and over the cavern floor.
	 */
	private void sculkSeeds() {
		int[][] deck = { { -6, -8 }, { 6, 6 } };
		for (int[] p : deck) {
			this.sculkSeeds.add(new BlockPos(p[0], 0, p[1]));
		}
		for (int i = 0; i < 4; i++) {
			double a = this.random.nextDouble() * Math.PI * 2.0;
			double r = 12.0 + this.random.nextDouble() * 6.0;
			int x = (int) Math.round(Math.cos(a) * r);
			int z = (int) Math.round(Math.sin(a) * r);
			if (inTower(x, z)) {
				continue;
			}
			int f = floor(x, z);
			if (isOpen(x, f + 1, z)) {
				this.sculkSeeds.add(new BlockPos(x, f + 1, z));
			}
		}
		for (int z : new int[] { 30 }) {
			double cx = 3.0 * Math.sin(z * 0.13 + this.phase[7]);
			double cy = 1.0 + (z - 18) * 0.35;
			int x = (int) Math.round(cx);
			int y = (int) Math.round(cy - 2.2) + 1;
			while (y > cy - 4 && isOpen(x, y - 1, z)) {
				y--;
			}
			this.sculkSeeds.add(new BlockPos(x, y, z));
		}
	}

	// ---- The tunnel to the ancient city ----

	/** How far from the city's centre the tunnel runs (into the city's outer ring). */
	static final int CITY_TUNNEL_END = 70;
	/** Where the doorway stands, measured from the city's centre. */
	static final int CITY_DOOR = 96;

	/**
	 * Owner (2026-11-16): one Fabricator beside every ancient city, reached from it. The tunnel leaves the cavern's
	 * front and runs straight (with a slight wander) toward the city's centre at the deck's level, easing to the city's
	 * floor, until it is {@link #CITY_TUNNEL_END} from it: it cuts its own gap through whatever of the city's outer
	 * ring is in the way (it is placed after the city). A doorway of polished and chiseled deepslate frames the
	 * passage {@link #CITY_DOOR} from the city's centre. Its floor is cracked deepslate tiles; the rock round it is only
	 * touched where it is liquid (sealed with deepslate), so lava and water don't pour in.
	 */
	private void cityTunnel() {
		if (this.city == null) {
			return;
		}
		double sx = 0.0;
		double sz = 12.0;
		double dx = this.city.getX() - sx;
		double dz = this.city.getZ() - sz;
		double len = Math.hypot(dx, dz);
		double ux = dx / len;
		double uz = dz / len;
		double run = Math.max(8.0, len - CITY_TUNNEL_END);
		double door = Math.max(4.0, len - CITY_DOOR);
		double endY = Math.max(-6.0, Math.min(6.0, this.city.getY()));
		java.util.Set<BlockPos> carved = new java.util.HashSet<>();
		for (double t = 0.0; t <= run; t += 0.5) {
			double wob = 1.5 * Math.sin(t * 0.11 + this.phase[12]);
			double cx = sx + ux * t - uz * wob;
			double cz = sz + uz * t + ux * wob;
			double cy = 1.0 + (endY - 1.0) * Math.min(1.0, t / run);
			for (int x = (int) Math.floor(cx) - 3; x <= (int) Math.ceil(cx) + 3; x++) {
				for (int z = (int) Math.floor(cz) - 3; z <= (int) Math.ceil(cz) + 3; z++) {
					double side = (x - cx) * -uz + (z - cz) * ux;
					double along = (x - cx) * ux + (z - cz) * uz;
					if (Math.abs(along) > 0.6) {
						continue;
					}
					for (int y = (int) Math.floor(cy) - 1; y <= (int) Math.ceil(cy) + 3; y++) {
						double ex = side / 2.4;
						double ey = (y - cy - 1.0) / 2.2;
						if (ex * ex + ey * ey <= 1.0) {
							carved.add(new BlockPos(x, y, z));
						}
					}
				}
			}
			// The doorway: a frame across the passage.
			if (Math.abs(t - door) < 0.25) {
				for (int k = -4; k <= 4; k++) {
					for (int y = (int) Math.floor(cy) - 2; y <= (int) Math.ceil(cy) + 5; y++) {
						int x = (int) Math.round(cx - uz * k);
						int z = (int) Math.round(cz + ux * k);
						double ex = k / 2.4;
						double ey = (y - cy - 1.0) / 2.2;
						if (ex * ex + ey * ey > 1.0 && ex * ex + ey * ey <= 2.4) {
							boolean edge = Math.abs(k) == 3 && y == (int) Math.ceil(cy) + 3;
							this.doorway.put(new BlockPos(x, y, z), edge ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
									: Blocks.POLISHED_DEEPSLATE.defaultBlockState());
						}
					}
				}
			}
		}
		for (BlockPos p : carved) {
			if (inGrid(p.getX(), p.getY(), p.getZ()) && isOpen(p.getX(), p.getY(), p.getZ())) {
				continue;
			}
			BlockPos below = p.below();
			if (!carved.contains(below)) {
				set(below.getX(), below.getY(), below.getZ(), hash(below.getX(), below.getY(), below.getZ(), 51) < 0.3
						? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
			}
			set(p.getX(), p.getY(), p.getZ(), Blocks.AIR.defaultBlockState());
			this.attached.remove(p);
			for (Direction dir : Direction.values()) {
				BlockPos n = p.relative(dir);
				if (dir != Direction.DOWN && !carved.contains(n) && !this.blocks.containsKey(n)) {
					this.blocks.put(n, SEAL);
				}
			}
		}
		for (Map.Entry<BlockPos, BlockState> e : this.doorway.entrySet()) {
			this.blocks.remove(e.getKey());
			this.blocks.put(e.getKey(), e.getValue());
		}
	}

	private final Map<BlockPos, BlockState> doorway = new LinkedHashMap<>();
	/** Placement marker: deepslate only where the world holds a liquid there (the tunnel's sealed sides). */
	private static final BlockState SEAL = Blocks.STRUCTURE_VOID.defaultBlockState();

	// ---- Secrets and the lower city ----

	/** The deck's west rim gap (its middle z), where stairs go down to the lower terrace. */
	static final int TERRACE_STAIR_Z = 3;
	/** The lower terrace's top block (y), west of the deck. */
	private static final int TERRACE_Y = -5;
	/** The reject vault under the deck: x, z span (walls included) and its floor block. */
	private static final int VAULT_X0 = 2;
	private static final int VAULT_X1 = 7;
	private static final int VAULT_Z0 = -8;
	private static final int VAULT_Z1 = -2;
	private static final int VAULT_FLOOR = -7;

	/** Every block the chains were built from (the intake's throat starts on one). */
	private final java.util.Set<BlockPos> chainBlocks = new java.util.LinkedHashSet<>();
	private final List<BlockPos> rejectChests = new ArrayList<>();
	private final List<BlockPos> buriedChests = new ArrayList<>();
	/** Floor tops of the new lower works (for sculk veins). */
	private final List<BlockPos> newTops = new ArrayList<>();
	private @Nullable BlockPos rejectTell;
	private @Nullable BlockPos throat;
	private @Nullable BlockPos intakeFloor;
	private @Nullable BlockPos hut;
	private double causewayEnd;

	/** Local: the odd chiseled tile in the deck over the reject vault (mine it to open the vault). */
	public @Nullable BlockPos rejectTell() {
		return this.rejectTell;
	}

	/** Local: the reject vault's chest(s). */
	public List<BlockPos> rejectChests() {
		return this.rejectChests;
	}

	/** Local: the chests sunk in the floors of the lower city. */
	public List<BlockPos> buriedChests() {
		return this.buriedChests;
	}

	/** Local: the foot of the intake's throat (first air above the chain block its vines grow from), or null. */
	public @Nullable BlockPos throat() {
		return this.throat;
	}

	/** Local: the intake chamber's middle, first air above its floor (the knot over the spawner). */
	public @Nullable BlockPos intakeFloor() {
		return this.intakeFloor;
	}

	/** Local: the ruined hut's middle, first air above its floor, or null. */
	public @Nullable BlockPos hut() {
		return this.hut;
	}

	/**
	 * The lower city: ancient-city work round the
	 * deck at broken, varied levels: a causeway on piers from the tunnel mouth to the deck's front, a lower terrace on
	 * the west reached by stairs from the deck (a ruined arch, a back wall, a wool corner, a buried chest) with stairs
	 * on down to the cavern floor, a ruined hut on the floor (wool, a buried chest), a broken arch and a buried chest
	 * behind the face wall, ruined pillars, rubble, and sculk veins over the new stone.
	 */
	private void lowerCity() {
		causeway();
		terrace();
		int side = frontDir()[0] > 0.15 ? -1 : 1;
		hut(side > 0 ? 10 : -14, 11, side);
		behindTheFace();
		int[][] ruins = { { -16, -12 }, { 12, -15 }, { side > 0 ? -8 : 8, 16 } };
		for (int[] r : ruins) {
			if (clearOfCauseway(r[0], r[1], 3.5) && clearOfHut(r[0], r[1])) {
				ruinedPillar(r[0], r[1]);
			}
		}
		// Rubble in the pit under the deck.
		rubble(-4, -5, 2.6, -3, 70);
		rubble(-3, 4, 2.0, -3, 71);
		flights(side);
		veins();
	}

	/**
	 * More ways down, each its own width: from the deck's east back gap a 2-wide flight to a
	 * landing north of the tower, then a broken flight on down; from the west back gap a narrow 1-wide broken flight
	 * behind the terrace to the floor; and from the causeway's side a 2-wide flight down into the pit (on the side
	 * away from the hut).
	 */
	private void flights(int side) {
		int end = flight(PX + 1, -1, -9, Direction.EAST, 2, 4, false, Blocks.DEEPSLATE_BRICK_STAIRS, 93);
		int lx = PX + 1 + end;
		int ly = -1 - end;
		for (int x = lx; x <= lx + 1; x++) {
			for (int z = -10; z <= -8; z++) {
				if (isOpen(x, ly, z)) {
					foundation(x, ly, z);
					solid(x, ly, z, x == lx + 1 || z == -10 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : tile(x, ly, z));
					this.newTops.add(new BlockPos(x, ly, z));
				}
			}
		}
		if (isOpen(lx + 1, ly + 1, -10)) {
			attach(lx + 1, ly + 1, -10, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
			attach(lx + 1, ly + 2, -10, unlitCandle(2));
		}
		flight(lx + 2, ly - 1, -9, Direction.EAST, 2, 10, true, Blocks.DEEPSLATE_TILE_STAIRS, 94);
		flight(-PX - 1, -1, -9, Direction.WEST, 1, 10, true, Blocks.COBBLED_DEEPSLATE_STAIRS, 95);
		double[] u = frontDir();
		int cx = (int) Math.round(u[0] * 6.0);
		int cz = (int) Math.round(8.5 + u[1] * 6.0);
		Direction out = side > 0 ? Direction.WEST : Direction.EAST;
		int sx = cx + 3 * out.getStepX();
		if (isOpen(sx, -1, cz) && isOpen(sx, -1, cz - 1)) {
			for (int x = cx - 3; x <= cx + 3; x++) {
				this.attached.remove(new BlockPos(x, 0, cz));
				this.attached.remove(new BlockPos(x, 0, cz - 1));
			}
			flight(sx, -1, out == Direction.WEST ? cz : cz - 1, out, 2, 10, false, Blocks.DEEPSLATE_TILE_STAIRS, 96);
		}
	}

	/**
	 * A flight of stairs going down toward {@code down} from its top step at (x, y, z), {@code width} wide (to the
	 * right of {@code down}), until it reaches the ground or {@code max} steps, on brick foundations. A broken flight
	 * misses some steps past the first two (nothing under them). Returns how many steps it ran.
	 */
	private int flight(int x, int y, int z, Direction down, int width, int max, boolean broken, Block stair, int salt) {
		Direction right = down.getClockWise();
		BlockState state = stair.defaultBlockState().setValue(StairBlock.FACING, down.getOpposite());
		int k = 0;
		for (; k < max; k++) {
			boolean any = false;
			for (int w = 0; w < width; w++) {
				int px = x + down.getStepX() * k + right.getStepX() * w;
				int pz = z + down.getStepZ() * k + right.getStepZ() * w;
				int py = y - k;
				if (!isOpen(px, py, pz)) {
					continue;
				}
				any = true;
				if (broken && k >= 2 && hash(px, py, pz, salt) < 0.22) {
					continue;
				}
				solid(px, py, pz, state);
				foundation(px, py - 1, pz);
			}
			if (!any) {
				break;
			}
		}
		return k;
	}

	/** Unit vector (x, z) from the deck's front toward the tunnel: straight ahead, or toward the city. */
	private double[] frontDir() {
		if (this.city == null) {
			return new double[] { 0.0, 1.0 };
		}
		double dx = this.city.getX();
		double dz = this.city.getZ() - 12.0;
		double len = Math.hypot(dx, dz);
		return new double[] { dx / len, dz / len };
	}

	private double causewayAlong(int x, int z) {
		double[] u = frontDir();
		return x * u[0] + (z - 8.5) * u[1];
	}

	private double causewaySide(int x, int z) {
		double[] u = frontDir();
		return x * -u[1] + (z - 8.5) * u[0];
	}

	private boolean clearOfCauseway(int x, int z, double margin) {
		double a = causewayAlong(x, z);
		return a < -margin || a > this.causewayEnd + margin || Math.abs(causewaySide(x, z)) > 2.4 + margin;
	}

	private boolean clearOfHut(int x, int z) {
		return this.hut == null || Math.abs(x - this.hut.getX()) > 5 || Math.abs(z - this.hut.getZ()) > 5;
	}

	/**
	 * A causeway from the deck's front gap to where the tunnel leaves the cavern (it used to be a 7-block drop into
	 * the pit): 5 wide at deck level, tiles with a polished edge on a slab underside, carried on piers every 6, a
	 * broken tile-wall parapet.
	 */
	private void causeway() {
		double[] u = frontDir();
		double end = 0.5;
		for (double t = 0.5; t <= 34.0; t += 0.5) {
			int x = (int) Math.round(u[0] * t);
			int z = (int) Math.round(8.5 + u[1] * t);
			if (!isOpen(x, 0, z) || !isOpen(x, -1, z)) {
				break;
			}
			end = t;
		}
		this.causewayEnd = end;
		for (int x = -30; x <= 30; x++) {
			for (int z = PZ_FRONT + 1; z <= 44; z++) {
				double along = causewayAlong(x, z);
				double side = causewaySide(x, z);
				if (along < 0.0 || along > end + 0.5 || Math.abs(side) > 2.4 || !isOpen(x, -1, z)) {
					continue;
				}
				boolean edge = Math.abs(side) > 1.5;
				solid(x, -1, z, edge ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : tile(x, -1, z));
				this.newTops.add(new BlockPos(x, -1, z));
				if (isOpen(x, -2, z)) {
					solid(x, -2, z, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
				}
				int seg = (int) Math.floor(along);
				if (Math.floorMod(seg, 6) == 3) {
					solid(x, -2, z, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
					foundation(x, -3, z);
				}
				if (edge && along > 1.5 && hash(x, 0, z, 63) < 0.45) {
					attach(x, 0, z, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
				}
			}
		}
	}

	/**
	 * The lower terrace, west of the deck at 4 below it: brick foundations, a tile floor with a broken outer edge,
	 * stairs up to the deck's west gap and down to the cavern floor, a ruined back wall with candles, a ruined arch
	 * with a soul lantern under its lintel, a gray and light blue wool corner, and a chest sunk in the floor.
	 */
	private void terrace() {
		int x0 = -19;
		int x1 = -14;
		int z0 = -6;
		int z1 = 7;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (!isOpen(x, TERRACE_Y + 1, z)) {
					continue;
				}
				boolean outer = x == x0 || z == z1;
				if (outer && hash(x, 0, z, 64) < 0.4 || x == x0 + 1 && hash(x, 0, z, 64) < 0.12) {
					continue; // broken away
				}
				foundation(x, TERRACE_Y, z);
				solid(x, TERRACE_Y, z, x == x1 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : tile(x, TERRACE_Y, z));
				this.newTops.add(new BlockPos(x, TERRACE_Y, z));
			}
		}
		// Stairs from the deck's west gap down to the terrace (3 wide).
		for (int k = 0; k < 4; k++) {
			int x = -PX - 1 - k;
			int y = -1 - k;
			for (int z = TERRACE_STAIR_Z - 1; z <= TERRACE_STAIR_Z + 1; z++) {
				solid(x, y, z, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
				foundation(x, y - 1, z);
			}
		}
		// Stairs on down from the terrace's front to the cavern floor.
		for (int x = -17; x <= -15; x++) {
			for (int k = 0; k < 8; k++) {
				int z = z1 + 1 + k;
				int y = TERRACE_Y - 1 - k;
				if (!isOpen(x, y, z)) {
					break;
				}
				solid(x, y, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
				foundation(x, y - 1, z);
			}
		}
		// The back wall, ruined to uneven heights, a tile band, candles on its broken top.
		for (int x = x0; x <= x1; x++) {
			int z = z0 - 1;
			if (!isOpen(x, TERRACE_Y + 1, z)) {
				continue;
			}
			foundation(x, TERRACE_Y, z);
			int h = 2 + (int) (hash(x, 1, z, 65) * 3.99);
			for (int y = TERRACE_Y; y <= TERRACE_Y + h; y++) {
				solid(x, y, z, y == TERRACE_Y + 3 ? tile(x, y, z) : x == x1 && y > TERRACE_Y ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : brick(x, y, z));
			}
			if (hash(x, 2, z, 65) < 0.35 && isOpen(x, TERRACE_Y + h + 1, z)) {
				attach(x, TERRACE_Y + h + 1, z, x == -16 ? candle(3) : unlitCandle(1 + (int) (hash(x, 3, z, 65) * 2.99)));
			}
		}
		// The wool corner (ancient-city camps: gray and light blue wool under carpet), and a chest sunk beside it.
		for (int x = x0 + 1; x <= x0 + 3; x++) {
			for (int z = z0; z <= z0 + 1; z++) {
				if (this.blocks.get(new BlockPos(x, TERRACE_Y, z)) == null || !isOpen(x, TERRACE_Y + 1, z)) {
					continue;
				}
				solid(x, TERRACE_Y, z, Blocks.WOOL.gray().defaultBlockState());
				if (hash(x, 4, z, 66) < 0.75) {
					attach(x, TERRACE_Y + 1, z, (x + z) % 2 == 0 ? Blocks.CARPET.lightBlue().defaultBlockState() : Blocks.CARPET.gray().defaultBlockState());
				}
			}
		}
		if (isOpen(x0 + 3, TERRACE_Y + 1, z0 + 2)) {
			attach(x0 + 3, TERRACE_Y + 1, z0 + 2, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 5));
		}
		if (this.blocks.get(new BlockPos(x1 - 1, TERRACE_Y, z0 + 1)) != null) {
			buriedChest(x1 - 1, TERRACE_Y, z0 + 1, Direction.EAST);
		}
		// A ruined arch across the terrace, its lintel carrying a soul lantern.
		arch(-17, -1, -17, 4, 6, 67, true);
		rubble(-20, 2, 2.5, 0, 68);
		this.sculkSeeds.add(new BlockPos(-16, TERRACE_Y + 1, 5));
	}

	/**
	 * A small ruined hut on the cavern floor (front corner away from the causeway): brick walls with a tile band,
	 * polished corners, a door toward the deck, a window, a slab roof fallen in at the far corner (rubble inside),
	 * a wool corner with carpet and a skull, unlit candles, and a chest sunk in its floor.
	 */
	private void hut(int x0, int z0, int side) {
		int top = Integer.MIN_VALUE;
		for (int x = x0 + 1; x <= x0 + 3; x++) {
			for (int z = z0 + 1; z <= z0 + 3; z++) {
				top = Math.max(top, groundAt(x, 0, z));
			}
		}
		if (top == Integer.MIN_VALUE) {
			return;
		}
		int fy = Math.min(top, -4);
		this.hut = new BlockPos(x0 + 2, fy + 1, z0 + 2);
		int doorX = side > 0 ? x0 : x0 + 4;
		int farX = side > 0 ? x0 + 4 : x0;
		int farZ = z0 + 4;
		for (int x = x0; x <= x0 + 4; x++) {
			for (int z = z0; z <= z0 + 4; z++) {
				if (ceil(x, z) == Integer.MIN_VALUE) {
					continue;
				}
				boolean wallX = x == x0 || x == x0 + 4;
				boolean wallZ = z == z0 || z == z0 + 4;
				boolean wall = wallX || wallZ;
				boolean ruined = Math.abs(x - farX) <= 2 && Math.abs(z - farZ) <= 2;
				foundation(x, fy - 1, z);
				solid(x, fy, z, wall ? brick(x, fy, z) : tile(x, fy, z));
				for (int h = 1; h <= 4; h++) {
					int y = fy + h;
					if (!wall) {
						solid(x, y, z, Blocks.AIR.defaultBlockState());
						continue;
					}
					boolean corner = wallX && wallZ;
					boolean door = x == doorX && z == z0 + 2 && h <= 2;
					boolean window = h == 2 && !wallX && x == x0 + 2;
					boolean broken = h >= 3 && hash(x, y, z, 73) < (corner ? 0.1 : 0.3) || ruined && h >= 2 && hash(x, y, z, 74) < 0.55;
					if (door || window || broken) {
						solid(x, y, z, Blocks.AIR.defaultBlockState());
					} else {
						solid(x, y, z, corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : h == 3 ? tile(x, y, z) : brick(x, y, z));
					}
				}
				if (!ruined && hash(x, fy + 5, z, 75) > 0.12) {
					solid(x, fy + 5, z, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
				}
			}
		}
		// Inside: the fallen corner's rubble, a wool corner by the door, a chest sunk in the floor.
		int inX = side > 0 ? x0 + 1 : x0 + 3;
		int outX = side > 0 ? x0 + 3 : x0 + 1;
		for (int z = z0 + 1; z <= z0 + 2; z++) {
			solid(inX, fy, z, Blocks.WOOL.gray().defaultBlockState());
			attach(inX, fy + 1, z, z == z0 + 1 ? Blocks.CARPET.lightBlue().defaultBlockState() : Blocks.CARPET.gray().defaultBlockState());
		}
		solid(outX, fy + 1, z0 + 3, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
		solid(outX, fy + 1, z0 + 2, Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
		solid(x0 + 2, fy + 1, z0 + 3, Blocks.COBBLED_DEEPSLATE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
		buriedChest(inX, fy, z0 + 3, side > 0 ? Direction.EAST : Direction.WEST);
		attach(outX, fy + 1, z0 + 1, unlitCandle(2));
		this.attached.remove(new BlockPos(inX, fy + 1, z0 + 1));
		solid(inX, fy + 1, z0 + 1, Blocks.WOOL.lightBlue().defaultBlockState());
		attach(inX, fy + 2, z0 + 1, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, side > 0 ? 12 : 4));
		this.newTops.add(new BlockPos(x0 + 2, fy, z0 + 2));
	}

	/** Behind the face wall: a broken arch over a chest sunk in the rubble, a scrap of carpet. */
	private void behindTheFace() {
		int z = -16;
		arch(-3, z, 3, z, 7, 76, false);
		int g = groundAt(0, 0, z);
		if (g != Integer.MIN_VALUE) {
			buriedChest(0, g, z, Direction.SOUTH);
			for (int x = -2; x <= -1; x++) {
				int gx = groundAt(x, 0, z - 1);
				if (gx != Integer.MIN_VALUE && this.attached.get(new BlockPos(x, gx + 1, z - 1)) == null) {
					attach(x, gx + 1, z - 1, Blocks.CARPET.gray().defaultBlockState());
				}
			}
		}
		rubble(4, z - 1, 2.2, 0, 77);
		rubble(-5, z + 1, 1.8, 0, 78);
	}

	/** A broken 2 x 2 pillar of bricks banded with polished basalt, chiseled at the foot, rubble round it. */
	private void ruinedPillar(int x0, int z0) {
		int h = 4 + (int) (hash(x0, 0, z0, 66) * 5.99);
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				int x = x0 + dx;
				int z = z0 + dz;
				int g = groundAt(x, 4, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				int top = g + h - (int) (hash(x, 1, z, 67) * 3.0);
				for (int y = g + 1; y <= top; y++) {
					int k = y - g;
					solid(x, y, z, k == 1 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
							: k % 4 == 0 ? Blocks.POLISHED_BASALT.defaultBlockState() : brick(x, y, z));
				}
			}
		}
		rubble(x0, z0, 3.2, 4, 69);
	}

	/**
	 * A ruined ancient-city arch between posts at a and b (same x or same z): its lintel {@code height} over the higher
	 * foot, on an upside-down stair corbel by the whole post; the other post broken short and the lintel reaching only
	 * part way, rubble at its foot. With {@code lantern}, a soul lantern hangs under the lintel.
	 */
	private void arch(int ax, int az, int bx, int bz, int height, int salt, boolean lantern) {
		int ga = groundAt(ax, 2, az);
		int gb = groundAt(bx, 2, bz);
		if (ga == Integer.MIN_VALUE || gb == Integer.MIN_VALUE) {
			return;
		}
		int lintel = Math.max(ga, gb) + height;
		post(ax, az, ga + 1, lintel - 1);
		int brokenTop = gb + 3 + (int) (hash(bx, 0, bz, salt) * 2.99);
		post(bx, bz, gb + 1, Math.min(brokenTop, lintel - 2));
		int dx = Integer.signum(bx - ax);
		int dz = Integer.signum(bz - az);
		int n = Math.abs(bx - ax) + Math.abs(bz - az);
		int reach = (int) Math.round(n * 0.6);
		for (int i = 0; i <= reach; i++) {
			solid(ax + dx * i, lintel, az + dz * i, i == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : tile(ax + dx * i, lintel, az + dz * i));
		}
		Direction towardA = Direction.getApproximateNearest(-dx, 0, -dz);
		solid(ax + dx, lintel - 1, az + dz, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, towardA).setValue(StairBlock.HALF, Half.TOP));
		if (lantern && reach >= 2) {
			attach(ax + dx * 2, lintel - 1, az + dz * 2, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}
		rubble(bx, bz, 1.8, gb + 2, salt + 1);
	}

	private void post(int x, int z, int y0, int y1) {
		for (int y = y0; y <= y1; y++) {
			solid(x, y, z, y == y0 || y == y1 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
					: (y - y0) % 3 == 2 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : brick(x, y, z));
		}
	}

	/** Loose rubble (cobbled deepslate, slabs, stairs, broken bricks) on the ground round a spot. */
	private void rubble(int cx, int cz, double r, int fromY, int salt) {
		int reach = (int) Math.ceil(r);
		Direction[] sides = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
		for (int x = cx - reach; x <= cx + reach; x++) {
			for (int z = cz - reach; z <= cz + reach; z++) {
				if (Math.hypot(x - cx, z - cz) > r || hash(x, 0, z, salt) > 0.5) {
					continue;
				}
				int g = groundAt(x, fromY, z);
				if (g == Integer.MIN_VALUE) {
					continue;
				}
				BlockState below = this.blocks.get(new BlockPos(x, g, z));
				if (below != null && (below.getBlock() instanceof StairBlock || below.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
						|| below.is(Blocks.CHEST) || below.is(Blocks.WOOL.gray()) || below.is(Blocks.REDSTONE_LAMP))) {
					continue;
				}
				double h = hash(x, 1, z, salt);
				BlockState s;
				if (h < 0.4) {
					s = Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
				} else if (h < 0.6) {
					s = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
				} else if (h < 0.75) {
					s = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
				} else if (h < 0.9) {
					s = Blocks.COBBLED_DEEPSLATE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, sides[(int) (hash(x, 2, z, salt) * 3.99)]);
				} else {
					s = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
				}
				solid(x, g + 1, z, s);
			}
		}
	}

	/** A chest sunk to its lid in the floor (its top block replaced), the floor round it broken, rubble half over it. */
	private void buriedChest(int x, int y, int z, Direction facing) {
		solid(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		this.buriedChests.add(new BlockPos(x, y, z));
		solid(x, y + 1, z, Blocks.AIR.defaultBlockState());
		for (Direction d : Direction.Plane.HORIZONTAL) {
			int nx = x + d.getStepX();
			int nz = z + d.getStepZ();
			BlockState n = this.blocks.get(new BlockPos(nx, y, nz));
			if (!isOpen(nx, y, nz) && (n == null || !n.is(Blocks.CHEST) && !n.is(Blocks.WOOL.gray()))) {
				solid(nx, y, nz, hash(nx, y, nz, 79) < 0.5 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState());
			}
			if (d != facing && isOpen(nx, y + 1, nz) && !this.attached.containsKey(new BlockPos(nx, y + 1, nz))) {
				double h = hash(nx, y + 1, nz, 80);
				if (h < 0.4) {
					solid(nx, y + 1, nz, Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
				} else if (h < 0.55) {
					solid(nx, y + 1, nz, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
				}
			}
		}
	}

	/** Sculk veins creeping over a few of the new floors (the ancient-city way: veins at the edges of growth). */
	private void veins() {
		for (BlockPos p : this.newTops) {
			BlockPos up = p.above();
			BlockState there = this.blocks.get(up);
			if (hash(p.getX(), p.getY(), p.getZ(), 81) < 0.08 && isOpen(up.getX(), up.getY(), up.getZ()) && !this.attached.containsKey(up)
					&& (there == null || there.isAir())) {
				attach(up.getX(), up.getY(), up.getZ(), Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
			}
		}
	}

	/**
	 * The reject pit: a sealed vault under the deck, its way in one odd tile: a chiseled deepslate in the
	 * deck off the chiseled grid (the deck's chiseled tiles sit every 4th block). Under it a ladder drops into a
	 * brick room heaped with failed products: bone blocks, the site's mob's heads (skeleton skulls for mobs without
	 * one; cobwebs for spiders, obsidian for endermen), soul soil, and a chest of rejects. From the pit below, its
	 * west face shows a bricked-up doorway.
	 */
	private void rejectPit() {
		int fy = VAULT_FLOOR;
		for (int x = VAULT_X0; x <= VAULT_X1; x++) {
			for (int z = VAULT_Z0; z <= VAULT_Z1; z++) {
				boolean wall = x == VAULT_X0 || x == VAULT_X1 || z == VAULT_Z0 || z == VAULT_Z1;
				if (wall && !isOpen(x, -4, z)) {
					continue; // a deck pillar stands here
				}
				foundation(x, fy - 1, z);
				solid(x, fy, z, wall ? brick(x, fy, z) : hash(x, fy, z, 82) < 0.35 ? Blocks.SOUL_SOIL.defaultBlockState() : tile(x, fy, z));
				for (int y = fy + 1; y <= -3; y++) {
					solid(x, y, z, wall ? brick(x, y, z) : Blocks.AIR.defaultBlockState());
				}
			}
		}
		// The bricked-up doorway on its west face.
		for (int z = -6; z <= -4; z++) {
			for (int y = fy + 1; y <= fy + 4; y++) {
				BlockState s = y == fy + 4 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
						: z != -5 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
						: hash(VAULT_X0, y, z, 83) < 0.5 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
				solid(VAULT_X0, y, z, s);
			}
		}
		// The tell and the ladder down.
		this.rejectTell = new BlockPos(VAULT_X0 + 1, -1, -5);
		solid(VAULT_X0 + 1, -1, -5, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
		for (int y = -2; y >= fy + 1; y--) {
			solid(VAULT_X0 + 1, y, -5, Blocks.AIR.defaultBlockState());
			attach(VAULT_X0 + 1, y, -5, ladder);
		}
		// The heap, piled into the back corner (+x, -z), away from the ladder's foot.
		BlockState head = switch (this.mob) {
			case ZOMBIE -> Blocks.ZOMBIE_HEAD.defaultBlockState();
			case CREEPER -> Blocks.CREEPER_HEAD.defaultBlockState();
			default -> Blocks.SKELETON_SKULL.defaultBlockState();
		};
		Direction.Axis[] axes = Direction.Axis.values();
		for (int x = VAULT_X0 + 1; x <= VAULT_X1 - 1; x++) {
			for (int z = VAULT_Z0 + 1; z <= VAULT_Z1 - 1; z++) {
				if (x <= VAULT_X0 + 2 && z >= -5) {
					continue; // the ladder's foot and the way in
				}
				double d = Math.hypot(x - (VAULT_X1 - 1), z - (VAULT_Z0 + 1));
				int h = (int) Math.round(2.4 - 0.7 * d + hash(x, 0, z, 84) * 0.9);
				int y = fy;
				for (int k = 1; k <= Math.min(h, 3); k++) {
					y = fy + k;
					double m = hash(x, y, z, 85);
					BlockState s;
					if (this.mob == Mob.ENDERMAN && m < 0.15) {
						s = Blocks.OBSIDIAN.defaultBlockState();
					} else if (m < 0.2) {
						s = Blocks.SOUL_SOIL.defaultBlockState();
					} else {
						s = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axes[(int) (hash(x, y, z, 86) * 2.99)]);
					}
					solid(x, y, z, s);
				}
				if (y + 1 <= -3) {
					double t = hash(x, y + 1, z, 87);
					if (t < 0.5) {
						attach(x, y + 1, z, head.setValue(SkullBlock.ROTATION, (int) (hash(x, 2, z, 88) * 15.99)));
					} else if (this.mob == Mob.SPIDER && t < 0.85) {
						attach(x, y + 1, z, Blocks.COBWEB.defaultBlockState());
					}
				}
			}
		}
		if (this.mob == Mob.SPIDER) {
			attach(VAULT_X1 - 1, -3, VAULT_Z1 - 1, Blocks.COBWEB.defaultBlockState());
			attach(VAULT_X0 + 2, -3, VAULT_Z0 + 1, Blocks.COBWEB.defaultBlockState());
		}
		// The rejects' chest, against the east wall at the near end, a head left on the floor beside it.
		BlockPos chest = new BlockPos(VAULT_X1 - 1, fy + 1, VAULT_Z1 - 1);
		solid(chest.getX(), chest.getY(), chest.getZ(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
		this.rejectChests.add(chest);
		this.attached.remove(chest.above());
		solid(chest.getX() - 1, chest.getY(), chest.getZ(), Blocks.AIR.defaultBlockState());
		attach(chest.getX() - 1, chest.getY(), chest.getZ(), head.setValue(SkullBlock.ROTATION, 6));
	}

	/**
	 * The intake: over the dripper's root, a low sculk-choked chamber that feeds the machine: floors of soul
	 * soil, soul sand, sculk and bone, walls and roof of sculk and wart, warped stems running in like roots toward the
	 * knot of hyphae in its floor (the hidden spawner is right under that knot), bone heaps, a sculk catalyst, one soul
	 * fire. Reached by climbing the chains: twisting vines grow up a throat from the chain that runs closest under the
	 * roof near the dripper, into the chamber.
	 */
	private void intake() {
		int fy = this.spawner.getY() + 1;
		int cz = DROP_Z;
		java.util.Set<BlockPos> air = new java.util.LinkedHashSet<>();
		for (int x = -10; x <= 10; x++) {
			for (int z = cz - 10; z <= cz + 10; z++) {
				double d = Math.hypot(x, z - cz);
				double a = Math.atan2(z - cz, x);
				double r = 6.5 + 1.2 * Math.sin(3 * a + this.phase[13]) + 0.6 * Math.sin(5 * a + this.phase[14]);
				if (d > r || nearBeam(x, z, 4.2)) {
					continue;
				}
				double t = d / r;
				int h = 2 + (int) Math.round(3.5 * (1.0 - t * t));
				for (int y = fy + 1; y <= fy + h; y++) {
					air.add(new BlockPos(x, y, z));
				}
			}
		}
		// The throat: from the chain block that runs closest under the roof, a few blocks out from the dripper.
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (BlockPos p : this.chainBlocks) {
			BlockState s = this.blocks.get(p);
			int x = p.getX();
			int z = p.getZ();
			double d = Math.hypot(x, z - cz);
			if (s == null || s.isAir() || d < 4.5 || d > 9.0 || nearBeam(x, z, 4.2) || !isOpen(x, p.getY() + 1, z) || !isOpen(x, p.getY() + 2, z)) {
				continue;
			}
			int gap = 0;
			while (isOpen(x, p.getY() + 1 + gap, z) && gap < 30) {
				gap++;
			}
			double score = gap + 1.5 * Math.abs(d - 5.5) + (air.contains(new BlockPos(x, fy + 1, z)) ? 0.0 : 3.0);
			if (score < bestScore) {
				bestScore = score;
				best = p;
			}
		}
		if (best != null && !air.contains(new BlockPos(best.getX(), fy + 1, best.getZ()))) {
			// A low passage in from the throat's head to the chamber.
			double len = Math.hypot(best.getX(), best.getZ() - cz);
			for (double s = 0.0; s <= len; s += 0.5) {
				int px = (int) Math.round(best.getX() - best.getX() / len * s);
				int pz = (int) Math.round(best.getZ() - (best.getZ() - cz) / len * s);
				air.add(new BlockPos(px, fy + 1, pz));
				air.add(new BlockPos(px, fy + 2, pz));
			}
		}
		for (BlockPos p : air) {
			solid(p.getX(), p.getY(), p.getZ(), Blocks.AIR.defaultBlockState());
		}
		// The lining.
		java.util.Set<BlockPos> lining = new java.util.LinkedHashSet<>();
		for (BlockPos p : air) {
			for (Direction dir : Direction.values()) {
				BlockPos n = p.relative(dir);
				if (!air.contains(n) && !isWrap(n)) {
					lining.add(n);
				}
			}
		}
		for (BlockPos n : lining) {
			int x = n.getX();
			int y = n.getY();
			int z = n.getZ();
			double h = hash(x, y, z, 90);
			BlockState s;
			if (air.contains(n.above())) {
				s = h < 0.35 ? Blocks.SOUL_SOIL.defaultBlockState() : h < 0.5 ? Blocks.SOUL_SAND.defaultBlockState()
						: h < 0.88 ? Blocks.SCULK.defaultBlockState() : Blocks.BONE_BLOCK.defaultBlockState();
			} else if (air.contains(n.below())) {
				s = h < 0.45 ? Blocks.SCULK.defaultBlockState() : h < 0.75 ? Blocks.WARPED_WART_BLOCK.defaultBlockState()
						: h < 0.92 ? Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState() : Blocks.BONE_BLOCK.defaultBlockState();
			} else {
				s = h < 0.4 ? Blocks.SCULK.defaultBlockState() : h < 0.6 ? Blocks.WARPED_WART_BLOCK.defaultBlockState()
						: h < 0.75 ? Blocks.SOUL_SOIL.defaultBlockState() : h < 0.88 ? Blocks.BONE_BLOCK.defaultBlockState()
						: Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState();
			}
			solid(x, y, z, s);
		}
		// Roots: warped stems along the floor from the walls in toward the knot, rising up the wall at their outer end.
		for (int k = 0; k < 6; k++) {
			double a = this.phase[15] + k * Math.PI / 3.0;
			double ca = Math.cos(a);
			double sa = Math.sin(a);
			Direction.Axis axis = Math.abs(ca) > Math.abs(sa) ? Direction.Axis.X : Direction.Axis.Z;
			BlockPos last = null;
			for (double s = 1.6; s <= 9.0; s += 0.4) {
				BlockPos p = new BlockPos((int) Math.round(ca * s), fy + 1, cz + (int) Math.round(sa * s));
				if (!air.contains(p)) {
					break;
				}
				solid(p.getX(), p.getY(), p.getZ(), Blocks.WARPED_STEM.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
				last = p;
			}
			for (BlockPos p = last == null ? null : last.above(); p != null && air.contains(p); p = p.above()) {
				solid(p.getX(), p.getY(), p.getZ(), Blocks.WARPED_STEM.defaultBlockState());
			}
		}
		// Bone heaps against the walls, a sculk catalyst, one soul fire, sculk veins, twisting vine tufts.
		for (int k = 0; k < 3; k++) {
			double a = this.phase[12] + k * 2.1 + 0.5;
			double hx = Math.cos(a) * 5.0;
			double hz = cz + Math.sin(a) * 5.0;
			for (int x = -10; x <= 10; x++) {
				for (int z = cz - 10; z <= cz + 10; z++) {
					double d = Math.hypot(x - hx, z - hz);
					if (d > 1.4) {
						continue;
					}
					for (int y = fy + 1; y <= fy + (d < 0.7 ? 2 : 1); y++) {
						BlockPos p = new BlockPos(x, y, z);
						if (air.contains(p) && this.blocks.get(p).isAir()) {
							solid(x, y, z, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.values()[(int) (hash(x, y, z, 91) * 2.99)]));
						}
					}
				}
			}
		}
		int catX = (int) Math.round(Math.cos(this.phase[15] + Math.PI / 6.0) * 3.0);
		int catZ = cz + (int) Math.round(Math.sin(this.phase[15] + Math.PI / 6.0) * 3.0);
		if (air.contains(new BlockPos(catX, fy + 1, catZ)) && !isWrap(new BlockPos(catX, fy, catZ))) {
			solid(catX, fy, catZ, Blocks.SCULK_CATALYST.defaultBlockState());
			solid(catX, fy + 1, catZ, Blocks.AIR.defaultBlockState());
		}
		int fireX = (int) Math.round(Math.cos(this.phase[15] + Math.PI * 7.0 / 6.0) * 2.5);
		int fireZ = cz + (int) Math.round(Math.sin(this.phase[15] + Math.PI * 7.0 / 6.0) * 2.5);
		if (air.contains(new BlockPos(fireX, fy + 1, fireZ)) && !isWrap(new BlockPos(fireX, fy, fireZ))) {
			solid(fireX, fy, fireZ, Blocks.SOUL_SOIL.defaultBlockState());
			solid(fireX, fy + 1, fireZ, Blocks.AIR.defaultBlockState());
			attach(fireX, fy + 1, fireZ, Blocks.SOUL_FIRE.defaultBlockState());
		}
		for (BlockPos p : air) {
			if (!this.blocks.get(p).isAir() || this.attached.containsKey(p)) {
				continue;
			}
			double h = hash(p.getX(), p.getY(), p.getZ(), 92);
			BlockState under = this.blocks.get(p.below());
			if (h < 0.05 && !air.contains(p.below()) && under != null && !under.isAir()) {
				attach(p.getX(), p.getY(), p.getZ(), Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25));
			} else if (h < 0.2) {
				BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();
				boolean any = false;
				for (Direction dir : Direction.values()) {
					if (dir != Direction.DOWN && !air.contains(p.relative(dir))) {
						vein = vein.setValue(MultifaceBlock.getFaceProperty(dir), true);
						any = true;
					}
				}
				if (any) {
					attach(p.getX(), p.getY(), p.getZ(), vein);
				}
			}
		}
		this.intakeFloor = new BlockPos(0, fy + 1, cz);
		if (best == null) {
			return;
		}
		// The throat itself: cut up from the chain to the chamber floor, lined with teal hyphae, twisting vines climbing it.
		int tx = best.getX();
		int tz = best.getZ();
		for (int y = best.getY() + 1; y <= fy; y++) {
			if (!isOpen(tx, y, tz)) {
				solid(tx, y, tz, Blocks.AIR.defaultBlockState());
				for (Direction dir : Direction.Plane.HORIZONTAL) {
					int nx = tx + dir.getStepX();
					int nz = tz + dir.getStepZ();
					BlockPos n = new BlockPos(nx, y, nz);
					if (!isOpen(nx, y, nz) && !air.contains(n) && !isWrap(n)) {
						solid(nx, y, nz, Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState());
					}
				}
			}
		}
		solid(tx, fy + 1, tz, Blocks.AIR.defaultBlockState());
		for (int y = best.getY() + 1; y <= fy + 1; y++) {
			attach(tx, y, tz, y == fy + 1 ? Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25)
					: Blocks.TWISTING_VINES_PLANT.defaultBlockState());
		}
		this.throat = best.above();
	}

	/** The hidden spawner and the six hyphae wrapping it. */
	private boolean isWrap(BlockPos p) {
		return p.distManhattan(this.spawner) <= 1;
	}

	/** Fills the open cells from {@code top} down to the ground with bricks (rock stays rock). */
	private void foundation(int x, int top, int z) {
		for (int y = top; y > MIN_Y + 2 && isOpen(x, y, z); y--) {
			solid(x, y, z, brick(x, y, z));
		}
	}

	/** The solid block under the first air at or below {@code fromY} (which must be open), or MIN. */
	private int groundAt(int x, int fromY, int z) {
		if (!isOpen(x, fromY, z)) {
			return Integer.MIN_VALUE;
		}
		int y = fromY;
		while (y > MIN_Y + 2 && isOpen(x, y - 1, z)) {
			y--;
		}
		return y - 1;
	}

	private BlockState brick(int x, int y, int z) {
		return hash(x, y, z, 61) < 0.3 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	}

	private BlockState tile(int x, int y, int z) {
		return hash(x, y, z, 62) < 0.3 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
	}

	private static BlockState unlitCandle(int count) {
		return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, count))).setValue(CandleBlock.LIT, false);
	}

	// ---- Placing ----

	/**
	 * Builds the whole Fabricator at once (the op command, tests): origin is the platform's middle (first air above the
	 * deck), then does what worldgen leaves to the first time a player comes near ({@link #prepare}).
	 */
	public void place(ServerLevel level, BlockPos origin, Rotation rotation) {
		placeIn(level, level.getChunkSource().getGenerator(), origin, rotation, null, RandomSource.create(this.seed));
		prepare(level, origin, rotation);
	}

	/**
	 * Places the part of the build inside {@code box} (a chunk during worldgen; null for all of it). Works on a
	 * worldgen region: block states, the upside-down beacon, chest loot tables, wall connections, and vanilla's
	 * ancient-city sculk patches grown from seeds inside the box.
	 */
	public void placeIn(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, BlockPos origin,
			Rotation rotation, net.minecraft.world.level.levelgen.structure.@Nullable BoundingBox box, RandomSource random) {
		int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		BooleanProperty upsideDown = upsideDownProperty();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<Map.Entry<BlockPos, BlockState>> entries = entriesFor(origin, rotation, box);
		for (Map.Entry<BlockPos, BlockState> e : entries) {
			BlockPos local = e.getKey();
			BlockPos r = local.rotate(rotation);
			pos.set(origin.getX() + r.getX(), origin.getY() + r.getY(), origin.getZ() + r.getZ());
			if (box != null && !box.isInside(pos) || level.isOutsideBuildHeight(pos)) {
				continue;
			}
			BlockState state = e.getValue();
			if (state == SEAL) {
				if (!level.getFluidState(pos).isEmpty()) {
					level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), flags);
				}
				continue;
			}
			if (local.equals(this.ceilingBeacon) && upsideDown != null) {
				state = state.setValue(upsideDown, true);
			}
			level.setBlock(pos, state.rotate(rotation), flags);
		}
		for (BlockPos chest : this.chests) {
			BlockPos at = toWorld(origin, chest, rotation);
			if ((box == null || box.isInside(at)) && level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
				c.setLootTable(FabricatorLoot.office());
				c.setLootTableSeed(random.nextLong());
			}
		}
		for (List<BlockPos> list : List.of(this.rejectChests, this.buriedChests)) {
			for (BlockPos chest : list) {
				BlockPos at = toWorld(origin, chest, rotation);
				if ((box == null || box.isInside(at)) && level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
					c.setLootTable(list == this.rejectChests ? FabricatorLoot.REJECTS : FabricatorLoot.BURIED);
					c.setLootTableSeed(random.nextLong());
				}
			}
		}
		// walls settle their connections
		for (Map.Entry<BlockPos, BlockState> e : entries) {
			if (e.getValue().getBlock() instanceof WallBlock || e.getValue().getBlock() instanceof IronBarsBlock) {
				BlockPos at = toWorld(origin, e.getKey(), rotation);
				if (box == null || box.isInside(at)) {
					level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), flags);
				}
			}
		}
		// Sculk grown with vanilla's own ancient-city patch feature (as the cities get theirs), each patch in the
		// chunk its seed is in.
		net.minecraft.world.level.levelgen.feature.Feature patch = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE)
				.getValueOrThrow(net.minecraft.data.worldgen.features.CaveFeatures.SCULK_PATCH_ANCIENT_CITY);
		RandomSource sculkRandom = RandomSource.create(this.seed ^ 0x5C01C0DEL);
		for (BlockPos seedPos : this.sculkSeeds) {
			BlockPos at = toWorld(origin, seedPos, rotation);
			RandomSource r = RandomSource.create(sculkRandom.nextLong());
			if (box == null || box.isInside(at)) {
				patch.place(level, generator, r, at);
			}
		}
	}

	/** Entries by world chunk, for the last origin and rotation placed (worldgen places one site chunk by chunk). */
	private record ChunkIndex(BlockPos origin, Rotation rotation,
			it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<Map.Entry<BlockPos, BlockState>>> byChunk) {
	}

	private volatile @Nullable ChunkIndex chunkIndex;

	/** The entries that can fall inside {@code box}: blocks then attached, in placing order. */
	private List<Map.Entry<BlockPos, BlockState>> entriesFor(BlockPos origin, Rotation rotation,
			net.minecraft.world.level.levelgen.structure.@Nullable BoundingBox box) {
		if (box == null || box.minX() >> 4 != box.maxX() >> 4 || box.minZ() >> 4 != box.maxZ() >> 4) {
			List<Map.Entry<BlockPos, BlockState>> all = new java.util.ArrayList<>(this.blocks.size() + this.attached.size());
			all.addAll(this.blocks.entrySet());
			all.addAll(this.attached.entrySet());
			return all;
		}
		ChunkIndex index = this.chunkIndex;
		if (index == null || !index.origin.equals(origin) || index.rotation != rotation) {
			var byChunk = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<List<Map.Entry<BlockPos, BlockState>>>();
			for (Map<BlockPos, BlockState> pass : List.of(this.blocks, this.attached)) {
				for (Map.Entry<BlockPos, BlockState> e : pass.entrySet()) {
					BlockPos r = e.getKey().rotate(rotation);
					long key = net.minecraft.world.level.ChunkPos.pack((origin.getX() + r.getX()) >> 4, (origin.getZ() + r.getZ()) >> 4);
					byChunk.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(e);
				}
			}
			index = new ChunkIndex(origin.immutable(), rotation, byChunk);
			this.chunkIndex = index;
		}
		return index.byChunk.getOrDefault(net.minecraft.world.level.ChunkPos.pack(box.minX() >> 4, box.minZ() >> 4), List.of());
	}

	/**
	 * What waits for the first time a player comes near (worldgen can't do it): the sculk patches' shriekers made
	 * unable to summon (one Warden per Fabricator), the pad and its lamps cleared of sculk, the office jukebox loaded
	 * with music disc 13 and playing.
	 */
	public void prepare(ServerLevel level, BlockPos origin, Rotation rotation) {
		int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		for (Map.Entry<BlockPos, BlockState> e : this.blocks.entrySet()) {
			BlockPos local = e.getKey();
			boolean pad = Math.abs(local.getX()) <= 2 && Math.abs(local.getZ() - DROP_Z) <= 2 && local.getY() == -1;
			if (pad || e.getValue().is(Blocks.REDSTONE_LAMP)) {
				BlockPos pos = toWorld(origin, local, rotation);
				if (!level.getBlockState(pos).is(e.getValue().getBlock())) {
					level.setBlock(pos, e.getValue().rotate(rotation), flags);
				}
				if (level.getBlockState(pos.above()).is(Blocks.SCULK_VEIN)) {
					level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), flags);
				}
			}
		}
		// shriekers: no Warden summons
		BoundingBox area = BoundingBox.fromCorners(toWorld(origin, new BlockPos(MIN_X, MIN_Y, MIN_Z), rotation),
				toWorld(origin, new BlockPos(MAX_X, MAX_Y, MAX_Z), rotation));
		for (SculkShriekerBlockEntity shrieker : LoadedBlockEntities.inLoading(level, SculkShriekerBlockEntity.class, area)) {
			BlockState state = shrieker.getBlockState();
			if (state.is(Blocks.SCULK_SHRIEKER) && state.getValue(SculkShriekerBlock.CAN_SUMMON)) {
				level.setBlock(shrieker.getBlockPos(), state.setValue(SculkShriekerBlock.CAN_SUMMON, false), flags);
			}
		}
		// office jukebox: disc 13
		if (level.getBlockEntity(toWorld(origin, this.jukebox, rotation)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box
				&& box.getTheItem().isEmpty()) {
			box.setTheItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MUSIC_DISC_13));
		}
	}

	/** Reworked Beacons' {@code upside_down} beacon state, when that mod is installed (looked up by name: no link). */
	@SuppressWarnings("unchecked")
	private static BooleanProperty upsideDownProperty() {
		Property<?> p = Blocks.BEACON.getStateDefinition().getProperty("upside_down");
		return p instanceof BooleanProperty b ? b : null;
	}

	/** Local to world. */
	public static BlockPos toWorld(BlockPos origin, BlockPos local, Rotation rotation) {
		return origin.offset(local.rotate(rotation));
	}

	private static BlockState parse(String id) {
		try {
			return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, new StringReader(id), false).blockState();
		} catch (CommandSyntaxException e) {
			throw new IllegalStateException("bad face block " + id, e);
		}
	}

	// ---- Grid helpers ----

	private int ceil(int x, int z) {
		if (x < MIN_X || x > MAX_X || z < MIN_Z || z > MAX_Z) {
			return Integer.MIN_VALUE;
		}
		return this.ceilCache[(x - MIN_X) * SZ + (z - MIN_Z)];
	}

	private static boolean inGrid(int x, int y, int z) {
		return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y && z >= MIN_Z && z <= MAX_Z;
	}

	private boolean isOpen(int x, int y, int z) {
		return inGrid(x, y, z) && this.open[((x - MIN_X) * SY + (y - MIN_Y)) * SZ + (z - MIN_Z)] != 0;
	}

	private void setOpen(int x, int y, int z, boolean value) {
		if (inGrid(x, y, z)) {
			this.open[((x - MIN_X) * SY + (y - MIN_Y)) * SZ + (z - MIN_Z)] = (byte) (value ? 1 : 0);
		}
	}

	private void set(int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		this.blocks.remove(pos);
		this.blocks.put(pos, state);
	}

	/** Sets a block that fills (or empties) the cavern: things attached there go. */
	private void solid(int x, int y, int z, BlockState state) {
		set(x, y, z, state);
		this.attached.remove(new BlockPos(x, y, z));
		setOpen(x, y, z, state.isAir());
	}

	private void attach(int x, int y, int z, BlockState state) {
		this.attached.put(new BlockPos(x, y, z), state);
	}

	private double hash(int x, int y, int z, int salt) {
		long h = this.seed * 0x9E3779B97F4A7C15L + x * 0x632BE59BD9B4E019L + y * 0x85157AF5L + z * 0x94D049BB133111EBL + salt * 0x2545F4914F6CDD1DL;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h >>> 11) * 0x1.0p-53;
	}

	/** Smooth-ish noise in [-1, 1] from a few seeded sine waves. */
	private double noise(double x, double y, double z, int salt) {
		double p = this.phase[(salt * 3) % 16];
		double q = this.phase[(salt * 3 + 1) % 16];
		double r = this.phase[(salt * 3 + 2) % 16];
		return (Math.sin(x * 1.7 + p) * Math.cos(z * 1.3 + q) + Math.sin(y * 1.9 + z * 0.7 + r) * 0.6
				+ Math.cos(x * 0.9 - y * 1.1 + q) * 0.5) / 2.1;
	}
}

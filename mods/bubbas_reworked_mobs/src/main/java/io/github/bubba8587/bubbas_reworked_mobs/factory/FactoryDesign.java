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
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WeepingVinesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jspecify.annotations.Nullable;

/**
 * An Ancient Fabricator's whole build (the mob factory of specs/mob-factory.md; owner's name, 2026-11-16), worked out from a seed and the factory's mob: a pure function,
 * so worldgen can later place it chunk by chunk. Local coordinates: the origin is the first air block above the
 * middle of the platform; +z is the front (the entrance tunnel), -z the back (the face wall), +x the Warden's tower.
 *
 * <p>Parts, back to front: a squashed dome cavern (radius about 21, floor 8 below the platform, 22 high in the
 * middle) lined with sculk; the <b>ceiling growth</b> (a 5-block mass of bone, basalt and wart with hanging spikes,
 * shroomlight bulbs and weeping vines; the inert spawner buried in it, the upside-down beacon set into it); the
 * <b>platform</b> (deepslate tiles on brick pillars, lit redstone lamps, the floor beacon on an iron inlay, the
 * teleporter pad, candles, sensors); the <b>face wall</b> (the mob's 8 x 8 face at 2 blocks a pixel,
 * {@link FactoryFaces}); the Warden's <b>tower</b> with its window, ledge and stairs down; and a tunnel out front.
 */
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

	/** The design for this seed and mob (deterministic), with the old winding tunnel out of the front. */
	public static FactoryDesign design(long seed, Mob mob) {
		return design(seed, mob, null);
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
		d.beams();
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

	public List<BlockPos> chests() {
		return this.chests;
	}

	public Mob mob() {
		return this.mob;
	}

	public long seed() {
		return this.seed;
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

	/** Where the Warden stands (feet), local. It looks toward -x, out of its window. */
	/** Local position of the office jukebox. */
	public BlockPos jukebox() {
		return this.jukebox;
	}

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
	 * The ceiling mass (owner, 2026-11-16: warped first, bone only a minor accent): warped wart with teal stripped
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
		// Small drippers (owner: any others much smaller): short warped teats, 2-5 long, spread over the ceiling,
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

	private void hangVines(int x, int y, int z, int length) {
		for (int i = 0; i < length; i++) {
			if (!isOpen(x, y - i - 1, z) || i == length - 1) {
				attach(x, y - i, z, Blocks.WEEPING_VINES.defaultBlockState().setValue(WeepingVinesBlock.AGE, 25));
				return;
			}
			attach(x, y - i, z, Blocks.WEEPING_VINES_PLANT.defaultBlockState());
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
	 * few knots is crimson (the owner's crimson streaks). None start on the face wall, the tower or the tunnel mouth.
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
				boolean stairGap = x == PX && z >= 6;
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
		// Corner posts; soul lanterns on the front two only (owner: dark like an ancient city).
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
		// Redstone lamps set into the frame, unlit (owner: dark like an ancient city; you bring a torch).
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

	private void tower() {
		int x0 = 14;
		int x1 = 20;
		for (int x = x0; x <= x1; x++) {
			for (int z = -4; z <= 4; z++) {
				for (int y = floor(Math.min(x, 16), z) - 1; y <= 21; y++) {
					boolean inside = x > x0 && x < x1 && Math.abs(z) < 4 && y >= 6 && y <= 10;
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
					} else if (y == 11 || y == 2 || y == 14) {
						s = Blocks.DEEPSLATE_TILES.defaultBlockState(); // bands
					} else {
						s = hash(x, y, z, 12) < 0.3 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
					}
					solid(x, y, z, s);
				}
			}
		}
		// Inside: the Warden's office (owner, 2026-11-16). Soul lanterns on chains, a gray carpet laid on wool
		// (wool under the carpet and round the jukebox, so its music doesn't reach the sculk), filing chests along
		// the back wall, a desk (tile slab on a lectern-and-stair frame) and a jukebox playing an eerie disc
		// (music disc 13, loaded when the factory is placed). A sculk catalyst in the corner, where it grew in.
		attach(17, 10, -2, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
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
		// Filing: three chests against the back wall, facing into the room, a candle on one.
		for (int z : new int[] { -2, -1, 0 }) {
			solid(19, 6, z, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
			this.chests.add(new BlockPos(19, 6, z));
			this.attached.remove(new BlockPos(19, 6, z));
		}
		attach(19, 7, -1, candle(2));
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
		for (Map<BlockPos, BlockState> pass : List.of(this.blocks, this.attached)) {
			for (Map.Entry<BlockPos, BlockState> e : pass.entrySet()) {
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
		}
		for (BlockPos chest : this.chests) {
			BlockPos at = toWorld(origin, chest, rotation);
			if ((box == null || box.isInside(at)) && level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) {
				c.setLootTable(FabricatorLoot.OFFICE);
				c.setLootTableSeed(random.nextLong());
			}
		}
		// Walls settle their connections.
		for (Map<BlockPos, BlockState> pass : List.of(this.blocks, this.attached)) {
			for (Map.Entry<BlockPos, BlockState> e : pass.entrySet()) {
				if (e.getValue().getBlock() instanceof WallBlock || e.getValue().getBlock() instanceof IronBarsBlock) {
					BlockPos at = toWorld(origin, e.getKey(), rotation);
					if (box == null || box.isInside(at)) {
						level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), flags);
					}
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
		BlockPos a = toWorld(origin, new BlockPos(MIN_X, MIN_Y, MIN_Z), rotation);
		BlockPos b = toWorld(origin, new BlockPos(MAX_X, MAX_Y, MAX_Z), rotation);
		for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.SCULK_SHRIEKER) && state.getValue(SculkShriekerBlock.CAN_SUMMON)) {
				level.setBlock(pos, state.setValue(SculkShriekerBlock.CAN_SUMMON, false), flags);
			}
		}
		// The office jukebox plays music disc 13 (eerie; owner: "a weird music disc that's always playing"; vanilla's
		// infinite jukebox, coming in current snapshots, will keep it going).
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

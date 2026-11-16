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

	private FactoryDesign(long seed, Mob mob) {
		this.seed = seed;
		this.mob = mob;
		this.random = RandomSource.create(seed);
		for (int i = 0; i < this.phase.length; i++) {
			this.phase[i] = this.random.nextDouble() * Math.PI * 2.0;
		}
	}

	/** The design for this seed and mob (deterministic). */
	public static FactoryDesign design(long seed, Mob mob) {
		FactoryDesign d = new FactoryDesign(seed, mob);
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
		return d;
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
		// The entrance tunnel, winding out of the front and climbing.
		for (int z = 12; z <= MAX_Z - 4; z++) {
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
		if (floorFace && n > 0.1 || !floorFace && !isOpen(x, y - 1, z) && n > 0.5) {
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
		if (h < 0.035) {
			return Blocks.SHROOMLIGHT.defaultBlockState();
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
						s = hash(x, y, z, 33) < 0.1 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.WARPED_WART_BLOCK.defaultBlockState();
					} else if (!outer && dist < 0.5 && Math.floorMod(d, 4) == 2 && t > 0.2) {
						s = Blocks.SHROOMLIGHT.defaultBlockState(); // a gland in the core
					} else if (outer && t >= 0.3 && hash(x, y, z, 34) < 0.08) {
						s = Blocks.SHROOMLIGHT.defaultBlockState(); // a gland on the skin
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
					solid(x, top - k, z, k == len - 1 && i % 2 == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : Blocks.WARPED_WART_BLOCK.defaultBlockState());
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
			solid(x0, y, z0, hash(x0, y, z0, 36) < 0.45 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.WARPED_WART_BLOCK.defaultBlockState());
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

	// ---- The chains: alien spurs climbing the walls and curling in to the drop point ----

	/**
	 * Owner (2026-11-16): the warped growth forms alien "chains"/spurs that curve up around the cavern's walls, then
	 * curl in and bend toward the central drop spot. Each is a tapering tube along a cubic curve: from low on the wall,
	 * up it, along the ceiling (twisting as it goes, so they spiral in), and down to a point hanging over the deck's
	 * middle. Warped wart flesh with bone vertebrae every few blocks and bone spurs with dripstone points sticking
	 * out and down. None start on the face wall, the Warden's tower or the entrance.
	 */
	private void tendrils() {
		int count = 9 + this.random.nextInt(3);
		double start = this.random.nextDouble() * Math.PI * 2.0;
		for (int i = 0; i < count; i++) {
			double a = start + i * Math.PI * 2.0 / count + (this.random.nextDouble() - 0.5) * 0.35;
			double ca = Math.cos(a);
			double sa = Math.sin(a);
			// Skip the face wall (behind, -z), the tower (+x) and the tunnel mouth (+z, middle).
			if (sa < -0.55 || ca > 0.8 || sa > 0.93) {
				continue;
			}
			double e = RADIUS + wobble(a);
			double turn = (this.random.nextBoolean() ? 1 : -1) * (0.5 + this.random.nextDouble() * 0.4);
			double[][] ctrl = {
					{ e - 2.5, -3.0 + this.random.nextDouble() * 3.0, 0.0 },
					{ e - 4.0, 11.0, turn * 0.15 },
					{ 10.0, 19.0, turn * 0.6 },
					{ 2.5 + this.random.nextDouble() * 1.5, 11.0 + this.random.nextDouble() * 3.0, turn },
			};
			double base = 0.95 + this.random.nextDouble() * 0.4;
			double lastX = Double.NaN;
			int step = 0;
			for (double t = 0.0; t <= 1.0; t += 0.012, step++) {
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
				// Never poke through the ceiling growth's top or below the deck's middle.
				int ceilHere = ceil((int) Math.round(cx), (int) Math.round(cz));
				if (ceilHere != Integer.MIN_VALUE) {
					y = Math.min(y, ceilHere - 0.5);
				}
				double radius = base * (1.0 - 0.75 * t) + 0.25;
				boolean rib = step % 7 == 0;
				int reach = (int) Math.ceil(radius + (rib ? 0.8 : 0));
				for (int x = (int) Math.floor(cx) - reach; x <= (int) Math.ceil(cx) + reach; x++) {
					for (int yy = (int) Math.floor(y) - reach; yy <= (int) Math.ceil(y) + reach; yy++) {
						for (int z = (int) Math.floor(cz) - reach; z <= (int) Math.ceil(cz) + reach; z++) {
							double d = Math.sqrt((x - cx) * (x - cx) + (yy - y) * (yy - y) + (z - cz) * (z - cz));
							if (nearBeam(x, z, 1.6) || inTower(x, z) || yy <= 1 && Math.abs(x) <= PX + 1 && z >= PZ_BACK && z <= PZ_FRONT) {
								continue;
							}
							if (d <= radius) {
								BlockState flesh = hash(x, yy, z, 21) < 0.03 ? Blocks.NETHER_WART_BLOCK.defaultBlockState()
										: hash(x, yy, z, 22) < 0.06 ? Blocks.SHROOMLIGHT.defaultBlockState()
										: Blocks.WARPED_WART_BLOCK.defaultBlockState();
								solid(x, yy, z, flesh);
							} else if (rib && d <= radius + 0.8) {
								solid(x, yy, z, Blocks.STRIPPED_WARPED_HYPHAE.defaultBlockState());
							}
						}
					}
				}
				// Spurs: a bone thorn sticking out and down every so often, tipped with dripstone.
				if (step % 14 == 7 && t > 0.25 && t < 0.9) {
					double out = this.random.nextDouble() * Math.PI * 2.0;
					int sx = (int) Math.round(cx + Math.cos(out) * (radius + 0.8));
					int sz = (int) Math.round(cz + Math.sin(out) * (radius + 0.8));
					int sy = (int) Math.round(y - radius * 0.5);
					int len = 1 + this.random.nextInt(3);
					int k = 0;
					for (; k < len && isOpen(sx, sy - k, sz) && !nearBeam(sx, sz, 1.6); k++) {
						solid(sx, sy - k, sz, k == len - 1 ? Blocks.WARPED_WART_BLOCK.defaultBlockState() : Blocks.WARPED_STEM.defaultBlockState());
					}
				}
				lastX = cx;
			}
			// The end hangs over the drop point: a bone tip and a long dripstone point.
			double endAng = a + ctrl[3][2];
			int ex = (int) Math.round(Math.cos(endAng) * ctrl[3][0]);
			int ez = (int) Math.round(Math.sin(endAng) * ctrl[3][0] + DROP_Z);
			int ey = (int) Math.round(Math.min(ctrl[3][1], ceil(ex, ez) - 0.5));
			if (!nearBeam(ex, ez, 1.6)) {
				solid(ex, ey - 1, ez, Blocks.WARPED_HYPHAE.defaultBlockState());
			}
		}
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
				} else if (underPlatform && h < 0.06) {
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
		// Lit redstone lamps set into the deck, each on a redstone block (their power), in two lines down the sides.
		for (int z : new int[] { -8, -4, 4 }) {
			for (int x : new int[] { -7, 7 }) {
				solid(x, -1, z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
				solid(x, -2, z, Blocks.REDSTONE_BLOCK.defaultBlockState());
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
			boolean lit = i < 5;
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
			for (int dz : new int[] { -2, 2 }) {
				attach(BEAM_X + dx, 0, BEAM_Z + dz, candle(3 + (int) (hash(dx, 0, dz, 4) * 1.99)));
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
		// Corner posts with soul lanterns.
		for (int x : new int[] { -PX, PX }) {
			for (int z : new int[] { PZ_BACK, PZ_FRONT }) {
				solid(x, 0, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				solid(x, 1, z, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
				attach(x, 2, z, Blocks.SOUL_LANTERN.defaultBlockState());
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
		attach(-5, 0, 7, candle(2));
		attach(6, 0, 7, candle(3));
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
		// Lit redstone lamps set into the frame (powered from behind) so the face reads in the dark.
		for (int x : new int[] { -7, -3, 2, 6 }) {
			solid(x, 17, WALL_Z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
			solid(x, 17, WALL_Z - 1, Blocks.REDSTONE_BLOCK.defaultBlockState());
		}
		for (int x : new int[] { -9, 8 }) {
			for (int y : new int[] { 4, 9, 13 }) {
				solid(x, y, WALL_Z, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
				solid(x, y, WALL_Z - 1, Blocks.REDSTONE_BLOCK.defaultBlockState());
			}
		}
		// A shelf of tile slabs under the face, and soul lanterns hanging either side.
		for (int x = -9; x <= 8; x++) {
			solid(x, 0, WALL_Z + 1, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
		}
		for (int x : new int[] { -10, 9 }) {
			solid(x, 14, WALL_Z + 1, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP));
			attach(x, 13, WALL_Z + 1, Blocks.IRON_CHAIN.defaultBlockState());
			attach(x, 12, WALL_Z + 1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
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
		attach(17, 10, 2, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
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
		attach(10, 7, -1, Blocks.SOUL_LANTERN.defaultBlockState());
		attach(13, 7, -1, Blocks.SOUL_LANTERN.defaultBlockState());
		// A lamp in the floor under the Warden, so it shows in its window.
		solid(16, 5, 1, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
		solid(16, 4, 1, Blocks.REDSTONE_BLOCK.defaultBlockState());
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
		int[][] deck = { { -6, -8 }, { 5, -7 }, { -7, 5 }, { 7, 0 }, { -3, 8 } };
		for (int[] p : deck) {
			this.sculkSeeds.add(new BlockPos(p[0], 0, p[1]));
		}
		this.sculkSeeds.add(new BlockPos(11, 0, 8));
		this.sculkSeeds.add(new BlockPos(12, 6, 0));
		for (int i = 0; i < 9; i++) {
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
		for (int z : new int[] { 24, 34 }) {
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

	// ---- Placing ----

	/** Builds the whole factory in the world: origin is the platform's middle (first air above the deck). */
	public void place(ServerLevel level, BlockPos origin, Rotation rotation) {
		int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		BooleanProperty upsideDown = upsideDownProperty();
		for (Map<BlockPos, BlockState> pass : List.of(this.blocks, this.attached)) {
			for (Map.Entry<BlockPos, BlockState> e : pass.entrySet()) {
				BlockState state = e.getValue();
				if (e.getKey().equals(this.ceilingBeacon) && upsideDown != null) {
					state = state.setValue(upsideDown, true);
				}
				level.setBlock(origin.offset(e.getKey().rotate(rotation)), state.rotate(rotation), flags);
			}
		}
		// Sculk grown with vanilla's own ancient-city patch feature, then its shriekers made unable to summon (one
		// Warden per factory).
		net.minecraft.world.level.levelgen.feature.Feature patch = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE)
				.getValueOrThrow(net.minecraft.data.worldgen.features.CaveFeatures.SCULK_PATCH_ANCIENT_CITY);
		RandomSource sculkRandom = RandomSource.create(this.seed ^ 0x5C01C0DEL);
		for (BlockPos seedPos : this.sculkSeeds) {
			patch.place(level, level.getChunkSource().getGenerator(), sculkRandom, toWorld(origin, seedPos, rotation));
		}
		// Keep the machine readable: the teleporter pad and every lamp stay clear of sculk (the ancient builders'
		// working parts; the growth creeps round them).
		for (Map.Entry<BlockPos, BlockState> e : this.blocks.entrySet()) {
			BlockPos local = e.getKey();
			boolean pad = Math.abs(local.getX()) <= 2 && Math.abs(local.getZ() - DROP_Z) <= 2 && local.getY() == -1;
			if (pad || e.getValue().is(Blocks.REDSTONE_LAMP)) {
				BlockPos pos = toWorld(origin, local, rotation);
				level.setBlock(pos, e.getValue().rotate(rotation), flags);
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
		// The office jukebox plays music disc 13 (eerie; owner: "a weird music disc that's always playing").
		if (level.getBlockEntity(toWorld(origin, this.jukebox, rotation)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity box) {
			box.setTheItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MUSIC_DISC_13));
		}
		// Walls, bars and chains settle their connections now that everything stands.
		for (Map<BlockPos, BlockState> pass : List.of(this.blocks, this.attached)) {
			for (Map.Entry<BlockPos, BlockState> e : pass.entrySet()) {
				if (e.getValue().getBlock() instanceof WallBlock || e.getValue().getBlock() instanceof IronBarsBlock) {
					BlockPos pos = origin.offset(e.getKey().rotate(rotation));
					level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), flags);
				}
			}
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

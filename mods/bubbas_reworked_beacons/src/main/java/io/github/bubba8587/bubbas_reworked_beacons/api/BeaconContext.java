// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * One beacon as a payload sees it: where it is, how it reaches (sky or paired), its pyramid size, the power cap
 * that size gives and the area it covers. The static helpers are the beacon's rules, public so other mods' tests
 * can use the same numbers (specs/reworked-beacons.md).
 */
public final class BeaconContext {
	/** Ticks between pulses: vanilla's 4 seconds. */
	public static final int PULSE_TICKS = 80;

	private final Level level;
	private final BlockPos pos;
	private final BeaconMode mode;
	private final int levels;
	private final int effectivePower;
	private final AABB area;
	private @Nullable List<LivingEntity> living;

	public BeaconContext(Level level, BlockPos pos, BeaconMode mode, int levels, int itemPower, AABB area) {
		this.level = level;
		this.pos = pos.immutable();
		this.mode = mode;
		this.levels = levels;
		this.effectivePower = Math.max(1, Math.min(itemPower, powerCapFor(levels)));
		this.area = area;
	}

	public Level level() {
		return this.level;
	}

	/** The beacon holding the item (the floor beacon of a pair). */
	public BlockPos pos() {
		return this.pos;
	}

	public BeaconMode mode() {
		return this.mode;
	}

	/** Pyramid layers, 1 to 4 (for a pair, the smaller of its two pyramids). */
	public int levels() {
		return this.levels;
	}

	/** The highest Gem Power this pyramid lets through. */
	public int powerCap() {
		return powerCapFor(this.levels);
	}

	/** The item's power after the cap, 1 to 6. */
	public int effectivePower() {
		return this.effectivePower;
	}

	/** Sideways reach from the beacon, in blocks. */
	public int range() {
		return this.mode == BeaconMode.PAIRED ? pairedRange(this.levels) : skyRange(this.levels);
	}

	/** The box the beacon covers. */
	public AABB area() {
		return this.area;
	}

	/** Vanilla's effect length for this pyramid, (9 + 2 x layers) seconds, so effects overlap the next pulse. */
	public int effectDurationTicks() {
		return (9 + this.levels * 2) * 20;
	}

	/** Every living thing in the area (players and mobs; spectators left out). Looked up once, on first use. */
	public List<LivingEntity> livingEntities() {
		if (this.living == null) {
			this.living = this.level.getEntitiesOfClass(LivingEntity.class, this.area, e -> e.isAlive() && !e.isSpectator());
		}
		return this.living;
	}

	/** Pyramid layers to Gem Power cap: 1 layer I, 2 up to III, 3 up to V, 4 VI (0 for no pyramid). */
	public static int powerCapFor(int levels) {
		return switch (Math.max(0, Math.min(levels, 4))) {
			case 0 -> 0;
			case 1 -> 1;
			case 2 -> 3;
			case 3 -> 5;
			default -> 6;
		};
	}

	/** Vanilla's range: 20 / 30 / 40 / 50 blocks for 1-4 layers. */
	public static int skyRange(int levels) {
		return levels * 10 + 10;
	}

	/** A pair reaches 3x vanilla's range sideways: 60 / 90 / 120 / 150. */
	public static int pairedRange(int levels) {
		return 3 * skyRange(levels);
	}

	/** Vanilla's area exactly: {@code new AABB(pos).inflate(range).expandTowards(0, height, 0)}. */
	public static AABB skyArea(Level level, BlockPos pos, int levels) {
		return new AABB(pos).inflate(skyRange(levels)).expandTowards(0.0, level.getHeight(), 0.0);
	}

	/**
	 * A pair's area: square, {@link #pairedRange} out sideways from the floor beacon, from 10 blocks below the
	 * floor pyramid to 10 blocks above the ceiling pyramid (both ends inclusive, in blocks).
	 *
	 * @param floor the floor beacon, whose pyramid of {@code floorLevels} layers is below it
	 * @param ceilingY the upside-down beacon's y, whose pyramid of {@code ceilingLevels} layers is above it
	 * @param levels the smaller of the two pyramids, which sets the range
	 */
	public static AABB pairedArea(BlockPos floor, int floorLevels, int ceilingY, int ceilingLevels, int levels) {
		int r = pairedRange(levels);
		int minY = floor.getY() - floorLevels - 10;
		int maxY = ceilingY + ceilingLevels + 10;
		return new AABB(floor.getX() - r, minY, floor.getZ() - r, floor.getX() + 1 + r, maxY + 1, floor.getZ() + 1 + r);
	}
}

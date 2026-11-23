// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** One beacon as a payload sees it. */
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

	/** The beacon holding the item. */
	public BlockPos pos() {
		return this.pos;
	}

	public BeaconMode mode() {
		return this.mode;
	}

	/** Pyramid layers, 1 to 4. */
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

	/** Vanilla's effect length for this pyramid, in ticks. */
	public int effectDurationTicks() {
		return (9 + this.levels * 2) * 20;
	}

	/** Every living thing in the area. */
	public List<LivingEntity> livingEntities() {
		if (this.living == null) {
			this.living = this.level.getEntitiesOfClass(LivingEntity.class, this.area, EntitySelector.ENTITY_STILL_ALIVE.and(EntitySelector.NO_SPECTATORS));
		}
		return this.living;
	}

	/** Pyramid layers to Gem Power cap: 1 layer I, 2 up to III, 3 up to V, 4 VI. */
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

	/** Vanilla's beacon area. */
	public static AABB skyArea(Level level, BlockPos pos, int levels) {
		return new AABB(pos).inflate(skyRange(levels)).expandTowards(0.0, level.getHeight(), 0.0);
	}

	/** A pair's area. */
	public static AABB pairedArea(BlockPos floor, int floorLevels, int ceilingY, int ceilingLevels, int levels) {
		int r = pairedRange(levels);
		int minY = floor.getY() - floorLevels - 10;
		int maxY = ceilingY + ceilingLevels + 10;
		return new AABB(floor.getX() - r, minY, floor.getZ() - r, floor.getX() + 1 + r, maxY + 1, floor.getZ() + 1 + r);
	}
}

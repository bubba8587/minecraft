// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.beacon;

import io.github.bubba8587.bubbas_reworked_beacons.BubbasReworkedBeacons;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconAreas;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode;
import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** The reworked beacon's rules, run from BeaconBlockEntityMixin after vanilla's tick. */
public final class BeaconLogic {
	/** Pair check interval. */
	public static final int PAIR_CHECK_TICKS = 20;
	/** Corner particles after an effect change: shown this long, a burst every EDGE_BURST_TICKS. */
	public static final int EDGE_FLASH_TICKS = 60;
	public static final int EDGE_BURST_TICKS = 10;
	/** Corner particle column: this many particles, half a block apart, this big. */
	public static final int EDGE_COLUMN = 7;
	public static final float EDGE_SCALE = 3.0F;

	private BeaconLogic() {
	}

	public static void tick(Level level, BlockPos pos, BeaconBlockEntity be) {
		ReworkedBeacon rb = (ReworkedBeacon) be;
		long time = level.getGameTime();
		if (time % PAIR_CHECK_TICKS == 0L) {
			updatePair(level, pos, rb);
		}
		if (time % BeaconContext.PULSE_TICKS == 0L && level instanceof ServerLevel server) {
			pulse(server, pos, rb);
		}
		if (rb.bubba$edgeFlash() > 0 && level instanceof ServerLevel server) {
			if (rb.bubba$edgeFlash() % EDGE_BURST_TICKS == 0) {
				showCorners(server, pos, rb);
			}
			rb.bubba$setEdgeFlash(rb.bubba$edgeFlash() - 1);
		}
	}

	/** Notes the shown effect (item, power, area); a change after the first pulse starts the corner particles. */
	static void noteEdge(BlockPos pos, ReworkedBeacon rb, int signature) {
		if (rb.bubba$edgeSeeded() && signature != rb.bubba$edgeSignature() && signature != 0) {
			rb.bubba$setEdgeFlash(EDGE_FLASH_TICKS);
		}
		rb.bubba$setEdge(signature, true);
	}

	/** A short column of the payload's colour at each corner of the beacon's area. */
	static void showCorners(ServerLevel level, BlockPos pos, ReworkedBeacon rb) {
		BeaconContext ctx = activeContext(level, pos, rb);
		BeaconPayload payload = Payloads.find(rb.bubba$item());
		if (ctx == null || payload == null) {
			rb.bubba$setEdgeFlash(0);
			return;
		}
		DustParticleOptions dust = new DustParticleOptions(payload.color(rb.bubba$item()), EDGE_SCALE);
		AABB area = ctx.area();
		for (double x : new double[] { area.minX, area.maxX }) {
			for (double z : new double[] { area.minZ, area.maxZ }) {
				int bx = Mth.floor(x - (x == area.maxX ? 0.5 : 0.0));
				int bz = Mth.floor(z - (z == area.maxZ ? 0.5 : 0.0));
				List<Integer> heights = new ArrayList<>();
				if (ctx.mode() == BeaconMode.PAIRED) {
					heights.add((int) area.minY);
					heights.add((int) Math.ceil(area.maxY - EDGE_COLUMN * 0.5));
				} else if (level.hasChunkAt(new BlockPos(bx, pos.getY(), bz))) {
					heights.add(Math.max((int) area.minY, level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz)));
				}
				for (int y : heights) {
					for (int i = 0; i < EDGE_COLUMN; i++) {
						level.sendParticles(dust, true, true, x, y + i * 0.5, z, 1, 0.0, 0.0, 0.0, 0.0);
					}
				}
			}
		}
	}

	/** A full pyramid layer count, 0-4, going down or up. */
	public static int pyramidLevels(Level level, BlockPos pos, int dir) {
		int levels = 0;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int step = 1; step <= 4; step++) {
			int y = pos.getY() + dir * step;
			if (y < level.getMinY() || y > level.getMaxY()) {
				break;
			}
			for (int x = pos.getX() - step; x <= pos.getX() + step; x++) {
				for (int z = pos.getZ() - step; z <= pos.getZ() + step; z++) {
					if (!level.getBlockState(at.set(x, y, z)).is(BlockTags.BEACON_BASE_BLOCKS)) {
						return levels;
					}
				}
			}
			levels = step;
		}
		return levels;
	}

	/** Vanilla's beam rule: a block with full light dampening stops it, except bedrock. */
	public static boolean blocksBeam(BlockState state) {
		return state.getLightDampening() >= 15 && !state.is(Blocks.BEDROCK);
	}

	/** Looks straight up from a floor beacon through clear blocks for an upside-down beacon. */
	public static void updatePair(Level level, BlockPos pos, ReworkedBeacon rb) {
		boolean upsideDown = HangingBeacon.isUpsideDown(level.getBlockState(pos));
		if (upsideDown) {
			rb.bubba$setPair(0, 0, 0);
			return;
		}
		int floor = pyramidLevels(level, pos, -1);
		int gap = 0;
		int top = Math.min(level.getMaxY(), level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()));
		BlockPos.MutableBlockPos p = pos.mutable();
		for (int y = pos.getY() + 1; y <= top; y++) {
			p.setY(y);
			BlockState state = level.getBlockState(p);
			if (state.is(Blocks.BEACON)) {
				if (HangingBeacon.isUpsideDown(state)) {
					gap = y - pos.getY();
				}
				break;
			}
			if (blocksBeam(state)) {
				break;
			}
		}
		int ceiling = gap > 0 ? pyramidLevels(level, pos.above(gap), 1) : 0;
		rb.bubba$setPair(floor, gap, ceiling);
	}

	public static BeaconMode mode(ReworkedBeacon rb) {
		if (rb.bubba$isPaired()) {
			return BeaconMode.PAIRED;
		}
		return rb.bubba$skyLevels() > 0 ? BeaconMode.SKY : BeaconMode.IDLE;
	}

	/** The pyramid layers that count for the current mode. */
	public static int levels(ReworkedBeacon rb) {
		return switch (mode(rb)) {
			case PAIRED -> rb.bubba$pairLevels();
			case SKY -> rb.bubba$skyLevels();
			case IDLE -> 0;
		};
	}

	public static AABB area(Level level, BlockPos pos, ReworkedBeacon rb, BeaconMode mode, int levels) {
		return mode == BeaconMode.PAIRED
				? BeaconContext.pairedArea(pos, rb.bubba$floorLevels(), pos.getY() + rb.bubba$pairGap(), rb.bubba$ceilingLevels(), levels)
				: BeaconContext.skyArea(level, pos, levels);
	}

	/** A context for the item now in the beacon. */
	public static @Nullable BeaconContext activeContext(Level level, BlockPos pos, ReworkedBeacon rb) {
		ItemStack item = rb.bubba$item();
		BeaconPayload payload = Payloads.find(item);
		BeaconMode mode = mode(rb);
		int levels = levels(rb);
		if (payload == null || mode == BeaconMode.IDLE || levels < Math.max(1, payload.requiredLevels(item))) {
			return null;
		}
		return new BeaconContext(level, pos, mode, levels, payload.power(item), area(level, pos, rb, mode, levels));
	}

	public static void pulse(ServerLevel level, BlockPos pos, ReworkedBeacon rb) {
		boolean paired = rb.bubba$isPaired();
		if (paired != rb.bubba$wasPaired()) {
			BeaconBlockEntity.playSound(level, pos, paired ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE);
			rb.bubba$setWasPaired(paired);
		} else if (paired) {
			BeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_AMBIENT);
		}

		BeaconContext ctx = activeContext(level, pos, rb);
		if (ctx == null) {
			BeaconAreas.remove(level, pos);
			noteEdge(pos, rb, 0);
			return;
		}
		ItemStack item = rb.bubba$item();
		noteEdge(pos, rb, Objects.hash(ItemStack.hashItemAndComponents(item), ctx.effectivePower(), ctx.area()) | 1);
		BeaconPayload payload = Payloads.find(item);
		try {
			payload.apply(level, item, ctx);
		} catch (RuntimeException e) {
			BubbasReworkedBeacons.LOGGER.error("Beacon payload {} failed at {}", payload.getClass().getName(), pos, e);
		}
		BeaconAreas.put(level, new BeaconAreas.ActiveBeacon(pos.immutable(), ctx.mode(), ctx.levels(), ctx.effectivePower(), ctx.area(),
				item.copy(), level.getGameTime()));
	}
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModGameEvents;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModTags;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hears game events for one sleeper, with vanilla's vibration rules (copied from VibrationSystem: spectators,
 * sneaking on #ignore_vibrations_sneaking, #dampens_vibrations on the affected block, wool occlusion). Cheap checks
 * first, the 6 occlusion raycasts last. A successful roll schedules the wake as a block tick (travel delay).
 */
public final class SleeperListener implements GameEventListener {
	/**
	 * How many more sleepers the awaken event being dispatched right now may wake. Game events are delivered
	 * synchronously on the server thread, so {@link SleeperWaking#emitAwaken} sets this around its dispatch.
	 */
	static int awakenBudget = 0;

	private final SleeperBlockEntity sleeper;
	private final PositionSource source;

	SleeperListener(SleeperBlockEntity sleeper) {
		this.sleeper = sleeper;
		this.source = new BlockPositionSource(sleeper.getBlockPos());
	}

	@Override
	public PositionSource getListenerSource() {
		return source;
	}

	@Override
	public int getListenerRadius() {
		return UndeadTuning.LISTENER_RADIUS;
	}

	/** Nearest sleepers first, so the awaken cap goes to the closest ones. */
	@Override
	public DeliveryMode getDeliveryMode() {
		return DeliveryMode.BY_DISTANCE;
	}

	@Override
	public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 from) {
		if (sleeper.isRemoved() || !event.is(ModTags.SLEEPERS_HEAR) || !sleeper.isHearing(level.getGameTime())) {
			return false;
		}
		boolean awaken = event.is(ModGameEvents.AWAKEN);
		if (awaken && awakenBudget <= 0) {
			return false;
		}
		Entity entity = context.sourceEntity();
		if (entity != null) {
			if (entity.isSpectator() || entity.dampensVibrations()) {
				return false;
			}
			if (entity.isSteppingCarefully() && event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
				return false;
			}
			if (!awaken && entity.is(ModTags.DOES_NOT_WAKE_SLEEPERS)) {
				return false;
			}
		}
		if (context.affectedState() != null && context.affectedState().is(BlockTags.DAMPENS_VIBRATIONS)) {
			return false;
		}
		BlockPos pos = sleeper.getBlockPos();
		Vec3 here = Vec3.atCenterOf(pos);
		double distance = from.distanceTo(here);
		float chance = SleeperHearing.wakeChance(event, distance) * sleeper.getSensitivity();
		if (chance <= 0.0F || isOccluded(level, from, here)) {
			return false;
		}
		sleeper.startCooldown(level.getGameTime(), UndeadTuning.HEARING_COOLDOWN_TICKS);
		if (level.getRandom().nextFloat() >= chance) {
			return false;
		}
		if (awaken) {
			awakenBudget--;
		}
		int delay = Mth.floor(distance);
		level.sendParticles(new VibrationParticleOption(source, Math.max(1, delay)), from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
		sleeper.scheduleWake(level, from, entity != null ? entity.getUUID() : null, delay);
		return true;
	}

	/** Vanilla's VibrationSystem.Listener#isOccluded (private there): blocked only if all 6 nudged rays hit #occludes_vibration_signals. */
	static boolean isOccluded(Level level, Vec3 origin, Vec3 dest) {
		Vec3 from = new Vec3(Mth.floor(origin.x) + 0.5, Mth.floor(origin.y) + 0.5, Mth.floor(origin.z) + 0.5);
		Vec3 to = new Vec3(Mth.floor(dest.x) + 0.5, Mth.floor(dest.y) + 0.5, Mth.floor(dest.z) + 0.5);
		for (Direction direction : Direction.values()) {
			Vec3 nudged = from.relative(direction, 1.0E-5F);
			if (level.isBlockInLine(new ClipBlockStateContext(nudged, to, state -> state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType()
					!= HitResult.Type.BLOCK) {
				return false;
			}
		}
		return true;
	}
}

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
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Hears game events for one sleeper, with vanilla's vibration rules (copied from VibrationSystem: spectators,
 * sneaking on #ignore_vibrations_sneaking, #dampens_vibrations on the affected block, wool occlusion). Cheap checks
 * first, the raycasts last. Each heard noise fills the sleeper's disturbance meter; crossing the wake threshold
 * schedules the wake as a block tick (travel delay), crossing the stir threshold makes it groan.
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
		if (sleeper.isRemoved() || !event.is(ModTags.SLEEPERS_HEAR) || !sleeper.isHearing()) {
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
			// Animals, bats and fish wandering through never wake sleepers (owner's call), but one being hurt or
			// killed is a fight going on, and fighting is loud.
			if (!awaken && isAnimal(entity) && !event.is(GameEvent.ENTITY_DAMAGE) && !event.is(GameEvent.ENTITY_DIE)) {
				return false;
			}
		}
		if (context.affectedState() != null && context.affectedState().is(BlockTags.DAMPENS_VIBRATIONS)) {
			return false;
		}
		BlockPos pos = sleeper.getBlockPos();
		if (BlockPos.containing(from).equals(pos)) {
			return false; // its own block being placed or changed is not a noise to it
		}
		// A bell's ring is heard through its own, longer-range path (SleeperBells); its plain block-change vibration isn't.
		if (event.is(GameEvent.BLOCK_CHANGE) && level.getBlockState(BlockPos.containing(from)).is(Blocks.BELL)) {
			return false;
		}
		Vec3 here = Vec3.atCenterOf(pos);
		double distance = from.distanceTo(here);
		float points = SleeperHearing.loudness(event, distance, entity != null && entity.isSprinting());
		if (points <= 0.0F) {
			return false; // out of earshot: skip the light and ward lookups
		}
		return hear(level, from, points * sleeper.effectiveSensitivity(level), entity, distance, awaken);
	}

	/**
	 * A noise of {@code points} (after falloff and sensitivity, before walls) reaches this sleeper from {@code from}:
	 * wool blocks it, walls muffle it, then it fills the meter and may stir or wake the sleeper.
	 *
	 * @return whether the sleeper heard it
	 */
	boolean hear(ServerLevel level, Vec3 from, float points, @Nullable Entity entity, double distance, boolean awaken) {
		BlockPos pos = sleeper.getBlockPos();
		Vec3 here = Vec3.atCenterOf(pos);
		if (points <= 0.0F || isOccluded(level, from, here)) {
			return false;
		}
		points *= SleeperHearing.wallFactor(level, from, here);
		long now = level.getGameTime();
		float before = sleeper.getDisturbance(now);
		float after = sleeper.disturb(now, points);
		if (after < UndeadTuning.WAKE_THRESHOLD) {
			if (before < UndeadTuning.STIR_THRESHOLD && after >= UndeadTuning.STIR_THRESHOLD) {
				stir(level, pos);
			}
			return true;
		}
		if (awaken) {
			awakenBudget--;
		}
		int delay = Mth.floor(distance);
		level.sendParticles(new VibrationParticleOption(source, Math.max(1, delay)), from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
		sleeper.scheduleWake(level, from, entity != null ? entity.getUUID() : null, delay);
		return true;
	}

	/** The warning: a quiet, low groan from the sleeper (its mob's ambient sound). */
	private void stir(ServerLevel level, BlockPos pos) {
		if (sleeper.getBlockState().getBlock() instanceof SleeperBlock block) {
			level.playSound(null, pos, block.variant().stirSound(), SoundSource.HOSTILE, 0.35F, 0.6F + level.getRandom().nextFloat() * 0.1F);
		}
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

	/** Passive wildlife: every mob category except monsters and misc (players, villagers, golems, projectiles). */
	static boolean isAnimal(Entity entity) {
		net.minecraft.world.entity.MobCategory category = entity.getType().getCategory();
		return category != net.minecraft.world.entity.MobCategory.MONSTER && category != net.minecraft.world.entity.MobCategory.MISC;
	}
}

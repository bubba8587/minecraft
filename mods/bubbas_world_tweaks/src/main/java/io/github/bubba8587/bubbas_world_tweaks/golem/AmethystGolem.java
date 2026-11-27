// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.golem;

import java.util.EnumSet;
import java.util.function.BiConsumer;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The Amethyst Golem: records note blocks, sings them in amethyst on its jukebox, dances to discs, chimes to vibrations. */
public class AmethystGolem extends AbstractGolem implements VibrationSystem {
	private static final EntityDataAccessor<Boolean> DATA_DANCING = SynchedEntityData.defineId(AmethystGolem.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_SINGING = SynchedEntityData.defineId(AmethystGolem.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_RECORDING = SynchedEntityData.defineId(AmethystGolem.class, EntityDataSerializers.BOOLEAN);
	/** Ticks the singing pose holds after a wind chime. */
	private static final int CHIME_POSE_TICKS = 10;

	private final PositionSource ears = new EntityPositionSource(this, 0.8F);
	private final DynamicGameEventListener<SongListener> songListener = new DynamicGameEventListener<>(new SongListener());
	private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
	private final VibrationSystem.User vibrationUser = new ChimeUser();
	private final DynamicGameEventListener<VibrationSystem.Listener> vibrationListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));

	private GolemSong song = GolemSong.EMPTY;
	private int tuning;
	private final IntArrayList take = new IntArrayList();
	private long recordStart;
	private @Nullable BlockPos jukebox;
	private @Nullable BlockPos discJukebox;
	private long songStart = -1;
	private int songIndex;
	private long lastClock = Long.MIN_VALUE;
	private int chimeTicks;
	private int chimes;

	public AmethystGolem(EntityType<? extends AmethystGolem> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2F).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.MAX_HEALTH, 12.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DANCING, false);
		builder.define(DATA_SINGING, false);
		builder.define(DATA_RECORDING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new HoldStillGoal());
		this.goalSelector.addGoal(2, new StandOnJukeboxGoal());
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// --- State ---

	public boolean isDancing() {
		return this.entityData.get(DATA_DANCING);
	}

	public boolean isSinging() {
		return this.entityData.get(DATA_SINGING);
	}

	public boolean isRecording() {
		return this.entityData.get(DATA_RECORDING);
	}

	public GolemSong song() {
		return this.song;
	}

	public void setSong(GolemSong song) {
		this.song = song;
		this.songStart = -1;
	}

	public int tuning() {
		return this.tuning;
	}

	public @Nullable BlockPos jukebox() {
		return this.jukebox;
	}

	/** Takes this jukebox as its spot (the POI claim is done by {@link #claimJukebox}). */
	public void assignJukebox(@Nullable BlockPos pos) {
		this.jukebox = pos == null ? null : pos.immutable();
	}

	/** Wind chimes rung so far. */
	public int chimes() {
		return this.chimes;
	}

	public boolean onJukebox() {
		return this.jukebox != null && this.blockPosition().equals(this.jukebox.above());
	}

	private boolean canSing() {
		return !this.song.isEmpty() && !this.isRecording() && !this.isDancing() && this.onJukebox();
	}

	// --- Ticking ---

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			VibrationSystem.Ticker.tick(level, this.vibrationData, this.vibrationUser);
			long now = level.getGameTime();
			if (this.isRecording() && now - this.recordStart >= WorldTweaksTuning.GOLEM_RECORD_CAP_TICKS) {
				this.stopRecording(level);
			}
			if (this.isDancing() && this.tickCount % 20 == 0 && this.shouldStopDancing()) {
				this.setDancing(false);
			}
			if ((this.tickCount + this.getId()) % WorldTweaksTuning.GOLEM_JUKEBOX_CHECK_TICKS == 0) {
				this.checkJukebox(level);
			}
			long clock = level.getDefaultClockTime();
			long anchor = clock != this.lastClock ? clock : now;
			this.lastClock = clock;
			this.tickSong(level, now, anchor);
			if (this.chimeTicks > 0) {
				this.chimeTicks--;
			}
			this.entityData.set(DATA_SINGING, this.songStart >= 0 || this.chimeTicks > 0);
		}
	}

	/**
	 * One tick of singing at game time {@code now}; a song may start only when {@code anchor} (the day clock, or game time
	 * while the day clock stands still) is a multiple of the song's period. Returns the tuned notes sung this tick.
	 */
	public IntList tickSong(ServerLevel level, long now, long anchor) {
		if (!this.canSing()) {
			this.songStart = -1;
			return IntList.of();
		}
		if (this.songStart >= 0 && now - this.songStart >= this.song.length()) {
			this.songStart = -1;
		}
		if (this.songStart < 0) {
			if (Math.floorMod(anchor, this.song.period(WorldTweaksTuning.GOLEM_BEAT_TICKS)) != 0) {
				return IntList.of();
			}
			this.songStart = now;
			this.songIndex = 0;
		}
		int elapsed = (int) (now - this.songStart);
		IntArrayList sung = null;
		while (this.songIndex < this.song.size() && this.song.tick(this.songIndex) <= elapsed) {
			if (this.song.tick(this.songIndex) == elapsed) {
				int note = GolemSong.tuned(this.song.note(this.songIndex), this.tuning);
				this.chime(level, note);
				if (sung == null) {
					sung = new IntArrayList();
				}
				sung.add(note);
			}
			this.songIndex++;
		}
		return sung == null ? IntList.of() : sung;
	}

	private void chime(ServerLevel level, int note) {
		level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL,
				WorldTweaksTuning.GOLEM_CHIME_VOLUME, GolemSong.pitch(note));
		this.noteParticle(level, note);
	}

	private void noteParticle(ServerLevel level, int note) {
		level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 1.5, this.getZ(), 0, note / 24.0, 0.0, 0.0, 1.0);
	}

	// --- Recording ---

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.AMETHYST_SHARD)) {
			if (this.level() instanceof ServerLevel level) {
				this.tuneUp(level);
			}
			return InteractionResult.SUCCESS;
		}
		if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
			if (this.level() instanceof ServerLevel level) {
				this.toggleRecording(level);
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Starts listening, or stops and keeps what it heard (an empty take wipes the song). */
	public void toggleRecording(ServerLevel level) {
		if (this.isRecording()) {
			this.stopRecording(level);
		} else {
			this.take.clear();
			this.recordStart = level.getGameTime();
			this.songStart = -1;
			this.entityData.set(DATA_RECORDING, true);
			this.getNavigation().stop();
			this.cue(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, ParticleTypes.NOTE, 5);
		}
	}

	private void stopRecording(ServerLevel level) {
		this.entityData.set(DATA_RECORDING, false);
		if (this.take.isEmpty()) {
			this.song = GolemSong.EMPTY;
			this.cue(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6F, ParticleTypes.SMOKE, 6);
		} else {
			int first = GolemSong.packedTick(this.take.getInt(0));
			int[] packed = new int[this.take.size()];
			for (int i = 0; i < packed.length; i++) {
				int p = this.take.getInt(i);
				packed[i] = GolemSong.pack(GolemSong.packedTick(p) - first, GolemSong.packedNote(p));
			}
			this.song = new GolemSong(packed);
			this.tuning = 0;
			this.cue(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, ParticleTypes.HAPPY_VILLAGER, 6);
		}
		this.take.clear();
		this.songStart = -1;
	}

	private void hear(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.NOTE_BLOCK) || this.take.size() >= WorldTweaksTuning.GOLEM_MAX_NOTES) {
			return;
		}
		int note = state.getValue(NoteBlock.INSTRUMENT).isTunable() ? state.getValue(NoteBlock.NOTE) : 12;
		int tick = (int) (level.getGameTime() - this.recordStart);
		this.take.add(GolemSong.pack(tick, note));
		this.noteParticle(level, note);
	}

	/** Steps the song (and its wind chime) up a semitone. */
	public void tuneUp(ServerLevel level) {
		this.tuning = (this.tuning + 1) % GolemSong.NOTES;
		int note = GolemSong.tuned(this.song.isEmpty() ? 12 : this.song.note(0), this.tuning);
		this.chime(level, note);
	}

	private void cue(ServerLevel level, SoundEvent sound, float pitch, net.minecraft.core.particles.SimpleParticleType particle, int count) {
		level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), sound, SoundSource.NEUTRAL, 1.0F, pitch);
		level.sendParticles(particle, this.getX(), this.getY() + 1.3, this.getZ(), count, 0.3, 0.2, 0.3, 0.02);
	}

	// --- Jukebox ---

	/** Keeps its claimed jukebox valid, or claims the nearest free one. */
	public void checkJukebox(ServerLevel level) {
		PoiManager poi = level.getPoiManager();
		if (this.jukebox != null) {
			boolean valid = poi.exists(this.jukebox, AmethystGolems::isJukeboxPoi);
			if (valid && this.jukebox.closerToCenterThan(this.position(), WorldTweaksTuning.GOLEM_JUKEBOX_RADIUS * 2)) {
				return;
			}
			if (valid) {
				poi.release(this.jukebox);
			}
			this.jukebox = null;
		}
		this.claimJukebox(level);
	}

	/** Claims the nearest jukebox no other golem holds. */
	public boolean claimJukebox(ServerLevel level) {
		PoiManager poi = level.getPoiManager();
		BlockPos here = this.blockPosition();
		int r = WorldTweaksTuning.GOLEM_JUKEBOX_RADIUS;
		BlockPos nearest = poi.findClosest(AmethystGolems::isJukeboxPoi, here, r, PoiManager.Occupancy.HAS_SPACE).orElse(null);
		if (nearest == null) {
			return false;
		}
		this.jukebox = poi.take(AmethystGolems::isJukeboxPoi, (type, pos) -> pos.equals(nearest), nearest, 1).orElse(null);
		return this.jukebox != null;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (reason.shouldDestroy() && this.jukebox != null && this.level() instanceof ServerLevel level
				&& level.getPoiManager().exists(this.jukebox, AmethystGolems::isJukeboxPoi)) {
			level.getPoiManager().release(this.jukebox);
			this.jukebox = null;
		}
		super.remove(reason);
	}

	// --- Dancing ---

	private void setDancing(boolean dancing) {
		this.entityData.set(DATA_DANCING, dancing);
		if (dancing) {
			this.getNavigation().stop();
		}
	}

	/** A jukebox nearby started or stopped a disc. */
	public void setJukeboxPlaying(BlockPos pos, boolean playing) {
		if (playing) {
			if (!this.isDancing()) {
				this.discJukebox = pos.immutable();
				this.setDancing(true);
			}
		} else if (pos.equals(this.discJukebox) || this.discJukebox == null) {
			this.discJukebox = null;
			this.setDancing(false);
		}
	}

	private boolean shouldStopDancing() {
		if (this.discJukebox == null
				|| !this.discJukebox.closerToCenterThan(this.position(), GameEvent.JUKEBOX_PLAY.value().notificationRadius())) {
			return true;
		}
		BlockState state = this.level().getBlockState(this.discJukebox);
		return !state.is(Blocks.JUKEBOX) || !state.getValue(JukeboxBlock.HAS_RECORD);
	}

	// --- Listening ---

	@Override
	public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> action) {
		if (this.level() instanceof ServerLevel level) {
			action.accept(this.songListener, level);
			action.accept(this.vibrationListener, level);
		}
	}

	@Override
	public VibrationSystem.Data getVibrationData() {
		return this.vibrationData;
	}

	@Override
	public VibrationSystem.User getVibrationUser() {
		return this.vibrationUser;
	}

	/** Note blocks (while recording) and jukeboxes, heard at once, without vibration travel time. */
	private class SongListener implements GameEventListener {
		@Override
		public PositionSource getListenerSource() {
			return AmethystGolem.this.ears;
		}

		@Override
		public int getListenerRadius() {
			return GameEvent.NOTE_BLOCK_PLAY.value().notificationRadius();
		}

		@Override
		public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 source) {
			if (event.is(GameEvent.NOTE_BLOCK_PLAY)) {
				if (AmethystGolem.this.isRecording()) {
					AmethystGolem.this.hear(level, BlockPos.containing(source));
					return true;
				}
			} else if (event.is(GameEvent.JUKEBOX_PLAY)) {
				AmethystGolem.this.setJukeboxPlaying(BlockPos.containing(source), true);
				return true;
			} else if (event.is(GameEvent.JUKEBOX_STOP_PLAY)) {
				AmethystGolem.this.setJukeboxPlaying(BlockPos.containing(source), false);
				return true;
			}
			return false;
		}
	}

	/** Wind chime: vibrations at or above the floor, while it has no song. */
	private class ChimeUser implements VibrationSystem.User {
		@Override
		public int getListenerRadius() {
			return WorldTweaksTuning.GOLEM_CHIME_RADIUS;
		}

		@Override
		public PositionSource getPositionSource() {
			return AmethystGolem.this.ears;
		}

		@Override
		public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context) {
			return AmethystGolem.this.song.isEmpty() && !AmethystGolem.this.isRecording()
					&& VibrationSystem.getGameEventFrequency(event) >= WorldTweaksTuning.GOLEM_CHIME_FLOOR;
		}

		@Override
		public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable Entity sourceEntity,
				@Nullable Entity projectileOwner, float receivingDistance) {
			AmethystGolem golem = AmethystGolem.this;
			if (!golem.song.isEmpty() || golem.isRecording()) {
				return;
			}
			int note = GolemSong.tuned(GolemSong.noteForFrequency(VibrationSystem.getGameEventFrequency(event)), golem.tuning);
			golem.chime(level, note);
			golem.chimes++;
			golem.chimeTicks = CHIME_POSE_TICKS;
		}
	}

	// --- Goals ---

	/** Stands still while listening or dancing. */
	private class HoldStillGoal extends Goal {
		HoldStillGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return AmethystGolem.this.isRecording() || AmethystGolem.this.isDancing();
		}

		@Override
		public void start() {
			AmethystGolem.this.getNavigation().stop();
		}
	}

	/** Goes to its jukebox and stays on top. */
	private class StandOnJukeboxGoal extends Goal {
		private int repath;

		StandOnJukeboxGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return AmethystGolem.this.jukebox != null;
		}

		@Override
		public void start() {
			this.repath = 0;
		}

		@Override
		public void tick() {
			AmethystGolem golem = AmethystGolem.this;
			if (golem.jukebox == null || golem.onJukebox()) {
				return;
			}
			if (--this.repath <= 0) {
				this.repath = 20;
				BlockPos top = golem.jukebox.above();
				golem.getNavigation().moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, 1.0);
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return false;
		}
	}

	// --- Saving ---

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (!this.song.isEmpty()) {
			output.putIntArray("song", this.song.packed());
		}
		output.putInt("tuning", this.tuning);
		output.storeNullable("jukebox", BlockPos.CODEC, this.jukebox);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.song = input.getIntArray("song").map(GolemSong::new).orElse(GolemSong.EMPTY);
		this.tuning = Math.floorMod(input.getIntOr("tuning", 0), GolemSong.NOTES);
		this.jukebox = input.read("jukebox", BlockPos.CODEC).orElse(null);
	}
}

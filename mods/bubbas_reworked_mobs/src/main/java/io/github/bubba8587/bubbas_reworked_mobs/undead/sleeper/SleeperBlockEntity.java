// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import java.util.List;
import java.util.UUID;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sleeper state: gear (saved as items and carried on the item as the vanilla minecraft:container component),
 * the disturbance meter, a pending wake and who/what woke it. Implementing {@link GameEventListener.Provider}
 * is enough for the chunk to register the listener; nothing ticks.
 */
public class SleeperBlockEntity extends BlockEntity implements GameEventListener.Provider<SleeperListener> {
	/** Gear slot order, which is also the robbing order: weapon first, then armor from the top down. */
	public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private final NonNullList<ItemStack> gear = NonNullList.withSize(SLOTS.size(), ItemStack.EMPTY);
	private final SleeperListener listener;
	/** The disturbance meter (specs/undead.md "Hearing and waking"), as of {@link #disturbanceTime}; it decays lazily, so no ticker. */
	private float disturbance;
	private long disturbanceTime;
	private boolean wakePending;
	private @Nullable Vec3 wakeSource;
	private @Nullable UUID waker;
	/** Multiplies every noise (and the robbing wake chance). 0 = never wakes from noise (warded, decor); large = always (traps, tests). */
	private float sensitivity = 1.0F;

	public SleeperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SLEEPER, pos, state);
		this.listener = new SleeperListener(this);
	}

	@Override
	public SleeperListener getListener() {
		return listener;
	}

	// --- gear ------------------------------------------------------------------------------------

	public ItemStack getGear(EquipmentSlot slot) {
		return gear.get(SLOTS.indexOf(slot));
	}

	public void setGear(EquipmentSlot slot, ItemStack stack) {
		gear.set(SLOTS.indexOf(slot), stack);
		gearChanged();
	}

	public boolean hasGear() {
		return gear.stream().anyMatch(s -> !s.isEmpty());
	}

	/** Removes and returns all gear (in {@link #SLOTS} order), e.g. to put it on the woken mob. */
	public NonNullList<ItemStack> takeAllGear() {
		NonNullList<ItemStack> taken = NonNullList.withSize(SLOTS.size(), ItemStack.EMPTY);
		for (int i = 0; i < gear.size(); i++) {
			taken.set(i, gear.get(i));
			gear.set(i, ItemStack.EMPTY);
		}
		setChanged();
		return taken;
	}

	private void gearChanged() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Mined, exploded or replaced: the gear falls out (the sleeper item itself carries none). A wake takes the gear first. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			for (ItemStack stack : gear) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
			gear.replaceAll(s -> ItemStack.EMPTY);
		}
	}

	// --- hearing and waking state ---------------------------------------------------------------

	public boolean isHearing() {
		return !wakePending;
	}

	/** The disturbance meter now: what was stored, less the decay since. */
	public float getDisturbance(long gameTime) {
		return Math.max(0.0F, disturbance - UndeadTuning.DISTURBANCE_DECAY_PER_TICK * Math.max(0L, gameTime - disturbanceTime));
	}

	/** Adds noise to the meter (after decaying it to now) and returns the new value. */
	public float disturb(long gameTime, float points) {
		if (SleeperBlock.isResting(getBlockState())) {
			return 0.0F; // the resting dead hear nothing
		}
		disturbance = getDisturbance(gameTime) + points;
		disturbanceTime = gameTime;
		setChanged();
		return disturbance;
	}

	/** Takes points off the meter (after decaying it to now), never below 0: a Totem of Warding's pulse, through api/Sleepers. */
	public void calm(long gameTime, float points) {
		float now = getDisturbance(gameTime);
		if (now > 0.0F) {
			disturbance = Math.max(0.0F, now - points);
			disturbanceTime = gameTime;
			setChanged();
		}
	}

	public boolean isWakePending() {
		return wakePending;
	}

	/** Marks the sleeper as waking and schedules the tick that turns it into a mob. */
	public void scheduleWake(ServerLevel level, @Nullable Vec3 source, @Nullable UUID wakerId, int delay) {
		if (SleeperBlock.isResting(getBlockState())) {
			return; // the resting dead never wake
		}
		wakePending = true;
		wakeSource = source;
		waker = wakerId;
		setChanged();
		level.scheduleTick(worldPosition, getBlockState().getBlock(), Math.max(1, delay));
	}

	public @Nullable Vec3 getWakeSource() {
		return wakeSource;
	}

	public @Nullable UUID getWaker() {
		return waker;
	}

	public float getSensitivity() {
		return sensitivity;
	}

	/**
	 * What noise (and robbing) is multiplied by right now: the stored sensitivity, times the light calm when the block
	 * light here is strong (specs/undead.md "Warding": like torches on a spawner), and 0 inside an area another mod keeps asleep (api/Sleepers: a Totem of Warding ward).
	 */
	public float effectiveSensitivity(Level level) {
		if (SleepAreas.isKeptAsleep(level, worldPosition)) {
			return 0.0F;
		}
		return sensitivity * lightCalm(level, worldPosition);
	}

	/** {@link UndeadTuning#LIGHT_CALM_FACTOR} at block light {@link UndeadTuning#LIGHT_CALM_LEVEL} or more, else 1. */
	public static float lightCalm(Level level, BlockPos pos) {
		return lightCalm(level.getBrightness(LightLayer.BLOCK, pos));
	}

	/** The light-calm factor for this block light level. */
	public static float lightCalm(int blockLight) {
		return blockLight >= UndeadTuning.LIGHT_CALM_LEVEL ? UndeadTuning.LIGHT_CALM_FACTOR : 1.0F;
	}

	public void setSensitivity(float sensitivity) {
		this.sensitivity = sensitivity;
		setChanged();
	}

	// --- saving, item components, client sync -------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, gear);
		if (disturbance > 0.0F) {
			output.putFloat("disturbance", disturbance);
			output.putLong("disturbance_time", disturbanceTime);
		}
		output.putBoolean("wake_pending", wakePending);
		output.storeNullable("wake_source", Vec3.CODEC, wakeSource);
		output.storeNullable("waker", UUIDUtil.CODEC, waker);
		if (sensitivity != 1.0F) {
			output.putFloat("sensitivity", sensitivity);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		gear.replaceAll(s -> ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, gear);
		disturbance = input.getFloatOr("disturbance", 0.0F);
		disturbanceTime = input.getLongOr("disturbance_time", 0L);
		wakePending = input.getBooleanOr("wake_pending", false);
		wakeSource = input.read("wake_source", Vec3.CODEC).orElse(null);
		waker = input.read("waker", UUIDUtil.CODEC).orElse(null);
		sensitivity = input.getFloatOr("sensitivity", 1.0F);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(gear));
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		gear.replaceAll(s -> ItemStack.EMPTY);
		components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(gear);
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("Items");
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The client needs the gear to draw it. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}

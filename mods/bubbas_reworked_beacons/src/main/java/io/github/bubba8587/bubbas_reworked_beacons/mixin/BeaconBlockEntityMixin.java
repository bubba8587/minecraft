// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.mixin;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_beacons.api.BeaconAreas;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.BeaconLogic;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.HangingBeacon;
import io.github.bubba8587.bubbas_reworked_beacons.beacon.ReworkedBeacon;
import io.github.bubba8587.bubbas_reworked_beacons.menu.BeaconItemMenu;
import io.github.bubba8587.bubbas_reworked_beacons.menu.BeaconSlotContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The reworked beacon: one kept item instead of powers and payment. */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin extends BlockEntity implements ReworkedBeacon {
	@Unique
	private static final String ITEM_TAG = "bubbas_reworked_beacons:item";

	@Shadow
	private int levels;
	@Shadow
	private List<BeaconBeamOwner.Section> beamSections;
	@Shadow
	private LockCode lockKey;

	@Unique
	private ItemStack bubba$item = ItemStack.EMPTY;
	@Unique
	private int bubba$floorLevels;
	@Unique
	private int bubba$gap;
	@Unique
	private int bubba$ceilingLevels;
	@Unique
	private boolean bubba$wasPaired;
	@Unique
	private List<BeaconBeamOwner.Section> bubba$pairBeam = List.of();

	@Unique
	private final ContainerData bubba$data = new ContainerData() {
		@Override
		public int get(int id) {
			ReworkedBeacon rb = BeaconBlockEntityMixin.this;
			return switch (id) {
				case BeaconItemMenu.DATA_MODE -> BeaconLogic.mode(rb).ordinal();
				case BeaconItemMenu.DATA_LEVELS -> BeaconLogic.levels(rb);
				case BeaconItemMenu.DATA_FLOOR_LEVELS -> rb.bubba$floorLevels();
				default -> 0;
			};
		}

		@Override
		public void set(int id, int value) {
		}

		@Override
		public int getCount() {
			return BeaconItemMenu.DATA_COUNT;
		}
	};

	protected BeaconBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public ItemStack bubba$item() {
		return this.bubba$item;
	}

	@Override
	public void bubba$setItem(ItemStack stack) {
		this.bubba$item = stack;
	}

	@Override
	public int bubba$skyLevels() {
		return this.levels > 0 && !this.beamSections.isEmpty() ? this.levels : 0;
	}

	@Override
	public int bubba$floorLevels() {
		return this.bubba$floorLevels;
	}

	@Override
	public int bubba$pairGap() {
		return this.bubba$gap;
	}

	@Override
	public int bubba$ceilingLevels() {
		return this.bubba$ceilingLevels;
	}

	@Override
	public void bubba$setPair(int floorLevels, int gap, int ceilingLevels) {
		this.bubba$floorLevels = floorLevels;
		this.bubba$gap = gap;
		this.bubba$ceilingLevels = ceilingLevels;
		if (this.bubba$isPaired()) {
			if (this.bubba$pairBeam.size() != 1 || this.bubba$pairBeam.getFirst().getHeight() != gap + 1) {
				BeaconBeamOwner.Section s = new BeaconBeamOwner.Section(0xFFFFFFFF);
				for (int i = 0; i < gap; i++) {
					s.increaseHeight();
				}
				this.bubba$pairBeam = List.of(s);
			}
		} else {
			this.bubba$pairBeam = List.of();
		}
	}

	@Unique
	private int bubba$edgeSignature;
	@Unique
	private boolean bubba$edgeSeeded;
	@Unique
	private int bubba$edgeFlash;

	@Override
	public int bubba$edgeSignature() {
		return this.bubba$edgeSignature;
	}

	@Override
	public boolean bubba$edgeSeeded() {
		return this.bubba$edgeSeeded;
	}

	@Override
	public void bubba$setEdge(int signature, boolean seeded) {
		this.bubba$edgeSignature = signature;
		this.bubba$edgeSeeded = seeded;
	}

	@Override
	public int bubba$edgeFlash() {
		return this.bubba$edgeFlash;
	}

	@Override
	public void bubba$setEdgeFlash(int ticks) {
		this.bubba$edgeFlash = ticks;
	}

	@Override
	public boolean bubba$wasPaired() {
		return this.bubba$wasPaired;
	}

	@Override
	public void bubba$setWasPaired(boolean paired) {
		this.bubba$wasPaired = paired;
	}

	/** An upside-down beacon does nothing on its own: no beam, no pyramid, no sounds. */
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private static void bubbas_reworked_beacons$hangingIsInert(Level level, BlockPos pos, BlockState state, BeaconBlockEntity entity, CallbackInfo ci) {
		if (HangingBeacon.isUpsideDown(state)) {
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private static void bubbas_reworked_beacons$tick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity entity, CallbackInfo ci) {
		BeaconLogic.tick(level, pos, entity);
	}

	/** Vanilla's primary/secondary powers never apply, even from beacons saved before the mod. */
	@Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
	private static void bubbas_reworked_beacons$noVanillaPowers(CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "getBeamSections", at = @At("HEAD"), cancellable = true)
	private void bubbas_reworked_beacons$beam(CallbackInfoReturnable<List<BeaconBeamOwner.Section>> cir) {
		if (this.bubba$isPaired()) {
			cir.setReturnValue(this.bubba$pairBeam);
		} else if (HangingBeacon.isUpsideDown(this.getBlockState())) {
			cir.setReturnValue(List.of());
		}
	}

	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void bubbas_reworked_beacons$menu(int containerId, Inventory inventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
		if (this.lockKey.canUnlock(player)) {
			cir.setReturnValue(new BeaconItemMenu(containerId, inventory, new BeaconSlotContainer((BeaconBlockEntity) (Object) this), this.bubba$data,
					ContainerLevelAccess.create(this.level, this.getBlockPos())));
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void bubbas_reworked_beacons$load(ValueInput input, CallbackInfo ci) {
		this.bubba$item = input.read(ITEM_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void bubbas_reworked_beacons$save(ValueOutput output, CallbackInfo ci) {
		if (!this.bubba$item.isEmpty()) {
			output.store(ITEM_TAG, ItemStack.CODEC, this.bubba$item);
		}
	}

	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void bubbas_reworked_beacons$forgetArea(CallbackInfo ci) {
		if (this.level != null && !this.level.isClientSide()) {
			BeaconAreas.remove(this.level, this.getBlockPos());
		}
	}

	/** Breaking the beacon drops its item. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level != null && !this.bubba$item.isEmpty()) {
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.bubba$item);
			this.bubba$item = ItemStack.EMPTY;
		}
	}
}

// dte:C8,B15,B17
package io.github.bubba8587.bubbas_world_tweaks.lava;

import io.github.bubba8587.bubbas_world_tweaks.WorldTweaksTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A lava geyser erupting (specs/world-tweaks.md "Lava geysers"): an invisible, short-lived marker at the lava surface
 * over a magma block. While it lasts the client draws a plume of black smoke (vanilla's sulfur geyser plume, darkened)
 * and rumbles, and {@link GeyserLift} lifts players above it. No blocks (B15); never saved (an eruption is momentary).
 */
public class LavaGeyser extends Entity {
	public LavaGeyser(EntityType<? extends LavaGeyser> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	/** The column above it that lifts. */
	public AABB column() {
		double r = WorldTweaksTuning.GEYSER_RADIUS;
		return new AABB(getX() - r, getY(), getZ() - r, getX() + r, getY() + WorldTweaksTuning.GEYSER_HEIGHT, getZ() + r);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			LavaGeyserEffects.tick(this);
			return;
		}
		if (tickCount >= WorldTweaksTuning.GEYSER_DURATION_TICKS || !level().getFluidState(BlockPos.containing(getX(), getY() - 0.5, getZ())).is(FluidTags.LAVA)) {
			discard();
			return;
		}
		if (tickCount == 1) {
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.5F);
		}
		if (tickCount % 20 == 0 && level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 4, 0.3, 0.1, 0.3, 0.0);
		}
	}

	/** Whether a magma block lies under lava at {@code surface} (the top lava block), within MAGMA_DEPTH. */
	public static boolean magmaUnder(Level level, BlockPos surface) {
		BlockPos.MutableBlockPos p = surface.mutable();
		for (int i = 0; i <= WorldTweaksTuning.MAGMA_DEPTH; i++) {
			p.move(0, -1, 0);
			if (level.getBlockState(p).is(Blocks.MAGMA_BLOCK)) {
				return true;
			}
			if (!level.getFluidState(p).is(FluidTags.LAVA)) {
				return false;
			}
		}
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}
}

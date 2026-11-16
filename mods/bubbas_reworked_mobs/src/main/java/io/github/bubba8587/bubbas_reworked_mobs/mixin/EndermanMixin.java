// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanAccess;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanJobGoal;
import io.github.bubba8587.bubbas_reworked_mobs.enderman.EndermanMind;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overworld endermen become caretakers (specs/endermen.md), no new entity type: vanilla's random take-block and
 * leave-block goals are swapped for {@link EndermanJobGoal}. Nether and End endermen (made in those levels) stay
 * vanilla, and so do vanilla's random natural spawns (MobFinalizeSpawnMixin; owner, 2026-11-16), saved as
 * {@code bubbas_reworked_mobs:vanilla}. E4: an enderman busy on a job ignores eye contact. A caretaker carrying a block still despawns (vanilla
 * keeps every block-carrying enderman forever), so a block it couldn't place vanishes with it.
 */
@Mixin(Enderman.class)
abstract class EndermanMixin extends Monster implements EndermanAccess {
	@Unique
	private @Nullable EndermanMind brm$mind;
	/** Vanilla's take-block and leave-block goals, set aside while it's a caretaker. */
	@Unique
	private @Nullable List<WrappedGoal> brm$vanillaGoals;
	@Unique
	private @Nullable EndermanJobGoal brm$jobGoal;
	@Unique
	private boolean brm$vanilla;
	@Unique
	private boolean brm$cairnSpawn;

	protected EndermanMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	public @Nullable EndermanMind brm$enderMind() {
		return brm$mind;
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void brm$caretakerGoals(CallbackInfo ci) {
		// registerGoals runs inside Mob's constructor; the level is already set, this class's fields are not.
		if (this.level().isClientSide() || this.level().dimension() != Level.OVERWORLD) {
			return;
		}
		brm$mind = new EndermanMind();
		brm$vanillaGoals = new ArrayList<>();
		List<WrappedGoal> goals = new ArrayList<>(goalSelector.getAvailableGoals());
		for (WrappedGoal wrapped : goals) {
			String name = wrapped.getGoal().getClass().getSimpleName();
			if (name.equals("EndermanTakeBlockGoal") || name.equals("EndermanLeaveBlockGoal")) {
				brm$vanillaGoals.add(wrapped);
				goalSelector.removeGoal(wrapped.getGoal());
			}
		}
		brm$jobGoal = new EndermanJobGoal((Enderman) (Object) this, brm$mind);
		goalSelector.addGoal(6, brm$jobGoal);
	}

	@Override
	public void brm$makeVanilla() {
		brm$vanilla = true;
		if (brm$mind == null) {
			return;
		}
		if (brm$jobGoal != null) {
			goalSelector.removeGoal(brm$jobGoal);
			brm$jobGoal = null;
		}
		if (brm$vanillaGoals != null) {
			for (WrappedGoal wrapped : brm$vanillaGoals) {
				goalSelector.addGoal(wrapped.getPriority(), wrapped.getGoal());
			}
			brm$vanillaGoals = null;
		}
		brm$mind = null;
	}

	@Override
	public void brm$markCairnSpawn() {
		brm$cairnSpawn = true;
	}

	@Override
	public boolean brm$isCairnSpawn() {
		return brm$cairnSpawn;
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void brm$saveVanilla(ValueOutput output, CallbackInfo ci) {
		if (brm$vanilla) {
			output.putBoolean("bubbas_reworked_mobs:vanilla", true);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void brm$loadVanilla(ValueInput input, CallbackInfo ci) {
		if (input.getBooleanOr("bubbas_reworked_mobs:vanilla", false)) {
			brm$makeVanilla();
		}
	}

	@Inject(method = "isBeingStaredBy", at = @At("HEAD"), cancellable = true)
	private void brm$busyIgnoresStare(Player player, CallbackInfoReturnable<Boolean> cir) {
		if (brm$mind != null && brm$mind.busy()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "requiresCustomPersistence", at = @At("HEAD"), cancellable = true)
	private void brm$despawnWithBlock(CallbackInfoReturnable<Boolean> cir) {
		if (brm$mind != null) {
			cir.setReturnValue(this.isPassenger() || this.isLeashed());
		}
	}
}

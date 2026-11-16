// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.ChargedOnlyGoal;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperFuseGoal;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperMind;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperWatchGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every vanilla creeper becomes a nervous watcher (specs/creepers.md), no new entity type: vanilla's hunting goals
 * (both target goals, MeleeAttackGoal, SwellGoal) are wrapped so they run only while the creeper is charged, and the
 * watcher's fuse and watch goals take their priorities. Floating, cat avoidance, strolling and looking around stay.
 * Creepers from spawners and trial spawners stay vanilla (owner, 2026-11-16; MobFinalizeSpawnMixin), saved as
 * {@code bubbas_reworked_mobs:vanilla}.
 */
@Mixin(Creeper.class)
abstract class CreeperMixin extends Monster implements CreeperAccess {
	@Unique
	private @Nullable CreeperMind brm$mind;
	@Unique
	private boolean brm$vanilla;

	protected CreeperMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	public CreeperMind brm$mind() {
		// Lazy: registerGoals runs inside Mob's constructor, before this class's fields are initialised.
		if (brm$mind == null) {
			brm$mind = new CreeperMind();
		}
		return brm$mind;
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void brm$watcherGoals(CallbackInfo ci) {
		Creeper self = (Creeper) (Object) this;
		brm$chargedOnly(self, goalSelector, true);
		brm$chargedOnly(self, targetSelector, false);
		goalSelector.addGoal(2, new CreeperFuseGoal(self));
		goalSelector.addGoal(4, new CreeperWatchGoal(self));
	}

	@Unique
	private static void brm$chargedOnly(Creeper self, GoalSelector selector, boolean huntingOnly) {
		List<WrappedGoal> goals = new ArrayList<>(selector.getAvailableGoals());
		for (WrappedGoal wrapped : goals) {
			if (!huntingOnly || wrapped.getGoal() instanceof SwellGoal || wrapped.getGoal() instanceof MeleeAttackGoal) {
				selector.removeGoal(wrapped.getGoal());
				selector.addGoal(wrapped.getPriority(), new ChargedOnlyGoal(self, wrapped.getGoal()));
			}
		}
	}

	@Override
	public void brm$makeVanilla() {
		if (brm$vanilla) {
			return;
		}
		brm$vanilla = true;
		for (GoalSelector selector : List.of(goalSelector, targetSelector)) {
			for (WrappedGoal wrapped : new ArrayList<>(selector.getAvailableGoals())) {
				Goal goal = wrapped.getGoal();
				if (goal instanceof ChargedOnlyGoal charged) {
					selector.removeGoal(goal);
					selector.addGoal(wrapped.getPriority(), charged.vanilla());
				} else if (goal instanceof CreeperFuseGoal || goal instanceof CreeperWatchGoal) {
					selector.removeGoal(goal);
				}
			}
		}
	}

	@Override
	public boolean brm$isVanilla() {
		return brm$vanilla;
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

	@Inject(method = "tick", at = @At("HEAD"))
	private void brm$senses(CallbackInfo ci) {
		if (!this.level().isClientSide() && this.isAlive() && !brm$vanilla) {
			brm$mind().tick((Creeper) (Object) this);
		}
	}
}

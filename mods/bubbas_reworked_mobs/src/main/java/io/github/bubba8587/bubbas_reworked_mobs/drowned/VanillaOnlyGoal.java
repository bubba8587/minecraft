// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import java.util.function.BooleanSupplier;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * One of vanilla's drowned goals the lurker doesn't use (trident throwing, melee chasing, beach walks, targeting),
 * kept for drowned that stay vanilla (spawners and trial spawners, owner 2026-11-16): it runs only while
 * {@code vanilla} says so, and is then exactly the wrapped goal.
 */
public final class VanillaOnlyGoal extends Goal {
	private final Goal goal;
	private final BooleanSupplier vanilla;

	public VanillaOnlyGoal(Goal goal, BooleanSupplier vanilla) {
		this.goal = goal;
		this.vanilla = vanilla;
		this.setFlags(goal.getFlags());
	}

	@Override
	public boolean canUse() {
		return vanilla.getAsBoolean() && goal.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return vanilla.getAsBoolean() && goal.canContinueToUse();
	}

	@Override
	public boolean isInterruptable() {
		return goal.isInterruptable();
	}

	@Override
	public void start() {
		goal.start();
	}

	@Override
	public void stop() {
		goal.stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return goal.requiresUpdateEveryTick();
	}

	@Override
	public void tick() {
		goal.tick();
	}
}

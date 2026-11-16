// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Wraps one of vanilla's hunting goals (target selection, melee approach, the swell-when-close) so it only runs for
 * charged creepers, which keep vanilla's suicide-bomber behavior (specs/creepers.md). Same flags and priority as the
 * goal it wraps, so a charged creeper's goal list is exactly vanilla's.
 */
public final class ChargedOnlyGoal extends Goal {
	private final Creeper creeper;
	private final Goal vanilla;

	public ChargedOnlyGoal(Creeper creeper, Goal vanilla) {
		this.creeper = creeper;
		this.vanilla = vanilla;
		this.setFlags(vanilla.getFlags());
	}

	public Goal vanilla() {
		return vanilla;
	}

	@Override
	public boolean canUse() {
		return creeper.isPowered() && vanilla.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return creeper.isPowered() && vanilla.canContinueToUse();
	}

	@Override
	public boolean isInterruptable() {
		return vanilla.isInterruptable();
	}

	@Override
	public void start() {
		vanilla.start();
	}

	@Override
	public void stop() {
		vanilla.stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return vanilla.requiresUpdateEveryTick();
	}

	@Override
	public void tick() {
		vanilla.tick();
	}
}

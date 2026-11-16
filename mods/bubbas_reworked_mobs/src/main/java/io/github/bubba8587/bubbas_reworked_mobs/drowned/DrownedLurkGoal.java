// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.drowned;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Drowned;

/**
 * Holds the drowned's movement, look and jump while its {@link DrownedMind} steers (travelling to a lurk spot,
 * lurking, surging, dragging), so vanilla's strolling and looking around stay out. The mind does the work, from
 * {@code Drowned#tick}; with the mind IDLE this goal stops and vanilla's goals run.
 */
public final class DrownedLurkGoal extends Goal {
	private final Drowned drowned;

	public DrownedLurkGoal(Drowned drowned) {
		this.drowned = drowned;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return DrownedAccess.mind(drowned).steering();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}

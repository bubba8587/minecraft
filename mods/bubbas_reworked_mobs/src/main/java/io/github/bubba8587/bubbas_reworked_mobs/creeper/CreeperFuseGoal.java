// dte:C1,B17
package io.github.bubba8587.bubbas_reworked_mobs.creeper;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

/**
 * The hiss (owner C1): a full startle meter runs vanilla's own swell (30 ticks, the primed sound, the swelling), the
 * creeper standing still and staring at whoever set it off. It defuses when nothing has hit it since the hiss began
 * and that one either sneaks or is beyond vanilla's 7-block swell range (or gone). Replaces vanilla's SwellGoal at the
 * same priority for uncharged creepers. Flint and steel still lights any creeper the vanilla way (not through here).
 */
public final class CreeperFuseGoal extends Goal {
	private final Creeper creeper;

	public CreeperFuseGoal(Creeper creeper) {
		this.creeper = creeper;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	private CreeperMind mind() {
		return CreeperAccess.mind(creeper);
	}

	@Override
	public boolean canUse() {
		return CreeperMind.active(creeper) && mind().isFusing();
	}

	@Override
	public void start() {
		creeper.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		CreeperMind mind = mind();
		LivingEntity target = mind.startler();
		if (!mind.wasHitDuringFuse() && calmsDown(target)) {
			mind.defuse(creeper);
			return;
		}
		creeper.setSwellDir(1);
		if (target != null) {
			creeper.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}
	}

	private boolean calmsDown(LivingEntity target) {
		return target == null || !target.isAlive() || target.isShiftKeyDown()
				|| creeper.distanceToSqr(target) > CreeperTuning.DEFUSE_RANGE * CreeperTuning.DEFUSE_RANGE;
	}
}

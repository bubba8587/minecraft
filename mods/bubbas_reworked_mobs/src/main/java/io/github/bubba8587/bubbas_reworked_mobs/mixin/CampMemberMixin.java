// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Camp mobs burn by day like any others and take cover with vanilla's own sun behaviour (owner: don't reinvent it).
 * Skeletons, strays and bogged have it already (AbstractSkeleton's RestrictSunGoal and FleeSunGoal); camp zombies get
 * the same two goals, once, the first time they tick (goals aren't saved, so this also covers a reload). Husks don't
 * burn and are left alone. The camp's lean-to, roofed towers and underground room are the shade they find.
 *
 * <p>Also the camp leash: camp mobs that chase too far from home turn back (see {@link #brm$leash}).
 */
@Mixin(Mob.class)
abstract class CampMemberMixin {
	private static final Set<Mob> BRM$SHADE_GOALS = Collections.newSetFromMap(new WeakHashMap<>());

	@Shadow
	@Final
	protected GoalSelector goalSelector;

	/** Camp mobs walking back to their camp after chasing too far (not saved: a reload just lets them chase again). */
	private static final Set<Mob> BRM$RETURNING = Collections.newSetFromMap(new WeakHashMap<>());

	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void brm$campZombiesTakeCover(ServerLevel level, CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (mob.hasHome() && mob.entityTags().contains(CampTuning.CAMP_TAG)) {
			brm$leash(mob);
		}
		if (mob instanceof Zombie zombie && mob.is(net.minecraft.tags.EntityTypeTags.BURN_IN_DAYLIGHT) && mob.entityTags().contains(CampTuning.CAMP_TAG) && BRM$SHADE_GOALS.add(mob)) {
			goalSelector.addGoal(2, new RestrictSunGoal(zombie));
			goalSelector.addGoal(3, new FleeSunGoal(zombie, 1.0));
		}
	}

	/**
	 * The camp leash (owner): a camp mob that chases a player more than {@link CampTuning#LEASH_RANGE} blocks from its
	 * home gives up, drops its target and walks home, ignoring players until it is back inside its home radius.
	 */
	private static void brm$leash(Mob mob) {
		BlockPos home = mob.getHomePosition();
		double away = mob.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		if (BRM$RETURNING.contains(mob)) {
			if (mob.isWithinHome()) {
				BRM$RETURNING.remove(mob);
				return;
			}
			mob.setTarget(null);
			mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET); // brain mobs (piglins)
			if (mob.tickCount % 20 == 0 || mob.getNavigation().isDone()) {
				mob.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.1);
			}
		} else if (away > CampTuning.LEASH_RANGE * CampTuning.LEASH_RANGE && mob.tickCount % 10 == 0) {
			BRM$RETURNING.add(mob);
		}
	}
}

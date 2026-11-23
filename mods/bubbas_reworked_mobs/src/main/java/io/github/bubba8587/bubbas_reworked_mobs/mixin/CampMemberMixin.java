// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.server.level.ServerLevel;
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
 */
@Mixin(Mob.class)
abstract class CampMemberMixin {
	private static final Set<Mob> BRM$SHADE_GOALS = Collections.newSetFromMap(new WeakHashMap<>());

	@Shadow
	@Final
	protected GoalSelector goalSelector;

	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void brm$campZombiesTakeCover(ServerLevel level, CallbackInfo ci) {
		Mob mob = (Mob) (Object) this;
		if (mob instanceof Zombie zombie && mob.is(net.minecraft.tags.EntityTypeTags.BURN_IN_DAYLIGHT) && mob.entityTags().contains(CampTuning.CAMP_TAG) && BRM$SHADE_GOALS.add(mob)) {
			goalSelector.addGoal(2, new RestrictSunGoal(zombie));
			goalSelector.addGoal(3, new FleeSunGoal(zombie, 1.0));
		}
	}
}

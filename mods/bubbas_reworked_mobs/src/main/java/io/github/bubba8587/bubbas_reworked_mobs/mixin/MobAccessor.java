// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mob's goal selector is protected; the Totem of Warding's flee goal is added to a monster the first time it is scared. */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("goalSelector")
	GoalSelector brm$goalSelector();
}

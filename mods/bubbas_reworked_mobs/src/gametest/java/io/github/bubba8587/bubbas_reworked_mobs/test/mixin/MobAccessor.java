// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only: which goals a mob is running, for failure messages (FailureContextMixin). */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("goalSelector")
	GoalSelector brmTest$goals();
}

// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the tests run a mob's spawn equipment choice many times without spawning a mob each. */
@Mixin(Mob.class)
public interface MobEquipmentInvoker {
	@Invoker("populateDefaultEquipmentSlots")
	void bubbas$populate(RandomSource random, DifficultyInstance difficulty);
}

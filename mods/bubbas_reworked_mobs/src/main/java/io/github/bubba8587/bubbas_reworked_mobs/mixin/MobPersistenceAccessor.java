// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * An Ancient Fabricator's deactivated mobs are persistent while deactivated (a pile on a stopped teleporter stays
 * when you step away) and lose it when they wake (vanilla only ever sets it).
 */
@Mixin(Mob.class)
public interface MobPersistenceAccessor {
	@Accessor("persistenceRequired")
	void brm$setPersistenceRequired(boolean value);
}

// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only: charge a creeper without a lightning strike (which would hurt it first). */
@Mixin(Creeper.class)
public interface CreeperAccessor {
	@Accessor("DATA_IS_POWERED")
	static EntityDataAccessor<Boolean> brmTest$powered() {
		throw new AssertionError();
	}
}

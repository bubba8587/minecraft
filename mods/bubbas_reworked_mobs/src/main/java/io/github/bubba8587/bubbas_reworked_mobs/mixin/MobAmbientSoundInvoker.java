// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A mob's own ambient sound, so a cocooned animal can cry out loudly (spider/SilkCocoon). */
@Mixin(Mob.class)
public interface MobAmbientSoundInvoker {
	@Invoker("getAmbientSound")
	@Nullable SoundEvent bubbas_reworked_mobs$ambientSound();
}
